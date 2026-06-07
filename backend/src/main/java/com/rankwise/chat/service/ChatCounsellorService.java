package com.rankwise.chat.service;

import com.rankwise.chat.dto.*;
import com.rankwise.chat.entity.ChatMessage;
import com.rankwise.chat.entity.ChatSession;
import com.rankwise.chat.entity.FaqArticle;
import com.rankwise.chat.entity.StudentProfile;
import com.rankwise.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatCounsellorService {

    private static final Logger logger = LoggerFactory.getLogger(ChatCounsellorService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ChatSessionService sessionService;
    private final ChatSafetyService safetyService;
    private final ChatRateLimitService rateLimitService;
    private final ChatRagService ragService;
    private final ChatCollegeContextService collegeContext;
    private final CursorAgentService cursorAgentService;
    private final ChatAnalyticsService analyticsService;
    private final AppProperties props;
    private final ChatReplyAsyncService replyAsyncService;

    public ChatCounsellorService(ChatSessionService sessionService,
                                 ChatSafetyService safetyService,
                                 ChatRateLimitService rateLimitService,
                                 ChatRagService ragService,
                                 ChatCollegeContextService collegeContext,
                                 CursorAgentService cursorAgentService,
                                 ChatAnalyticsService analyticsService,
                                 AppProperties props,
                                 @Lazy ChatReplyAsyncService replyAsyncService) {
        this.sessionService = sessionService;
        this.safetyService = safetyService;
        this.rateLimitService = rateLimitService;
        this.ragService = ragService;
        this.collegeContext = collegeContext;
        this.cursorAgentService = cursorAgentService;
        this.analyticsService = analyticsService;
        this.props = props;
        this.replyAsyncService = replyAsyncService;
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long sessionId,
                                           String visitorToken,
                                           SendMessageRequest request,
                                           HttpServletRequest http) {
        ChatSession session = sessionService.requireSession(sessionId, visitorToken);
        String content = safetyService.sanitizeUserMessage(request.content());
        if (safetyService.looksLikeSpam(content)) {
            throw new ChatException("Message looks like spam. Please ask a counselling-related question.");
        }

        String rateKey = visitorToken + ":" + clientIp(http);
        rateLimitService.check(rateKey);

        sessionService.saveUserMessage(sessionId, content);
        analyticsService.track(sessionId, session.getChatUserId(), ChatAnalyticsService.EVENT_MESSAGE_SENT, null);

        StudentProfile profile = sessionService.loadProfile(session.getChatUserId());
        ChatIntentDetector.DetectedIntent intent = ChatIntentDetector.detect(content, profile);

        if (intent.detectedRank != null && (profile == null || profile.getRank() == null
                || !intent.detectedRank.equals(profile.getRank()))) {
            UpdateProfileRequest rankUpdate = new UpdateProfileRequest(
                    intent.detectedRank, null, null, null, null, null);
            sessionService.updateProfile(sessionId, visitorToken, rankUpdate);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                replyAsyncService.runReply(sessionId, content);
            }
        });

        return new ChatMessageResponse(
                null,
                ChatMessage.ROLE_ASSISTANT,
                "",
                null,
                null,
                List.of(),
                false,
                List.of(),
                true
        );
    }

    @Transactional
    public void executeReply(Long sessionId, String content) {
        try {
            executeReplyInternal(sessionId, content);
        } catch (Exception e) {
            logger.error("executeReply failed for session {}", sessionId, e);
            sessionService.saveAssistantMessage(
                    sessionId,
                    "Sorry, something went wrong while preparing your answer. Please try again.",
                    null,
                    null,
                    null,
                    null);
        }
    }

    private void executeReplyInternal(Long sessionId, String content) {
        ChatSession session = sessionService.getSessionEntity(sessionId);
        StudentProfile profile = sessionService.loadProfile(session.getChatUserId());
        ChatIntentDetector.DetectedIntent intent = ChatIntentDetector.detect(content, profile);

        List<String> missing = ChatIntentDetector.missingProfileFields(profile);
        ChatStructuredPayload structured = null;
        String predictionContext = "";
        String comparisonContext = "";

        if (intent.wantsDocuments) {
            structured = ChatPromptBuilder.structured("DOCUMENTS", collegeContext.buildDocuments());
        }

        if (intent.branchInterest != null) {
            structured = ChatPromptBuilder.structured("BRANCH_ADVICE", collegeContext.buildBranchAdvice(intent.branchInterest));
        }

        if (intent.wantsComparison && intent.compareLeft != null && intent.compareRight != null) {
            CollegeComparisonPayload comparison = collegeContext.buildComparison(intent.compareLeft, intent.compareRight);
            structured = ChatPromptBuilder.structured("COMPARISON", comparison);
            comparisonContext = collegeContext.formatComparisonForPrompt(comparison);
            analyticsService.track(sessionId, session.getChatUserId(), ChatAnalyticsService.EVENT_COMPARISON_USED, null);
        }

        if (intent.wantsPrediction && ChatIntentDetector.profileCompleteForPrediction(profile)) {
            CollegePredictionPayload prediction = collegeContext.buildPrediction(profile);
            structured = ChatPromptBuilder.structured("PREDICTION", prediction);
            predictionContext = collegeContext.formatPredictionForPrompt(prediction);
            analyticsService.track(sessionId, session.getChatUserId(), ChatAnalyticsService.EVENT_PREDICTOR_USED, null);
        }

        List<FaqArticle> ragArticles = ragService.retrieve(content);
        if (!ragArticles.isEmpty()) {
            analyticsService.track(sessionId, session.getChatUserId(), ChatAnalyticsService.EVENT_FAQ_RAG, null);
        }
        String ragContext = ragService.formatForPrompt(ragArticles);
        String contextBlock = ChatPromptBuilder.buildUserContext(
                ragContext, profile, predictionContext, comparisonContext, missing);

        List<CursorAgentService.ChatTurn> turns = buildTurns(sessionId, contextBlock, content);
        String assistantText;
        String model = null;
        Integer promptTokens = null;
        Integer completionTokens = null;

        if (cursorAgentService.isConfigured()) {
            try {
                String existingAgentId = session.getCursorAgentId();
                String prompt = existingAgentId == null || existingAgentId.isBlank()
                        ? CursorAgentService.formatTurns(turns)
                        : CursorAgentService.formatFollowUp(contextBlock, content);

                CursorAgentService.AgentCompletionResult result = cursorAgentService.complete(existingAgentId, prompt);
                assistantText = result.content();
                model = result.model();
                if (existingAgentId == null || existingAgentId.isBlank()) {
                    sessionService.updateCursorAgentId(sessionId, result.agentId());
                }
            } catch (ChatException e) {
                logger.warn("Cursor reply failed for session {} — using fallback: {}", sessionId, e.getMessage());
                assistantText = fallbackReply(intent, missing, structured);
            }
        } else {
            assistantText = fallbackReply(intent, missing, structured);
        }

        sessionService.saveAssistantMessage(
                sessionId, assistantText, structured, model, promptTokens, completionTokens);

        session = sessionService.getSessionEntity(sessionId);
        if (session.getMessageCount() >= props.getChat().getLeadCtaAfterMessages() && !session.isLeadCtaShown()) {
            sessionService.markLeadCtaShown(sessionId);
        }

        logger.info("Async chat reply finished for session {}", sessionId);
    }

    private List<CursorAgentService.ChatTurn> buildTurns(Long sessionId, String contextBlock, String userMessage) {
        List<CursorAgentService.ChatTurn> turns = new ArrayList<>();
        turns.add(new CursorAgentService.ChatTurn("system", ChatPromptBuilder.SYSTEM_PROMPT));
        if (!contextBlock.isBlank()) {
            turns.add(new CursorAgentService.ChatTurn("system", contextBlock));
        }
        for (ChatMessage msg : sessionService.recentHistory(sessionId, 12)) {
            String role = ChatMessage.ROLE_USER.equals(msg.getRole()) ? "user" : "assistant";
            turns.add(new CursorAgentService.ChatTurn(role, msg.getContent()));
        }
        return turns;
    }

    private String fallbackReply(ChatIntentDetector.DetectedIntent intent,
                                 List<String> missing,
                                 ChatStructuredPayload structured) {
        if (structured != null && "PREDICTION".equals(structured.type())) {
            return "Here are your college lists based on RankWise cutoff data. Dream = ambitious, Target = realistic, Safe = backup. Verify on the official counselling portal.";
        }
        if (!missing.isEmpty()) {
            return "To personalize college recommendations, please share your "
                    + String.join(", ", missing) + ". You can use the profile form in the chat panel.";
        }
        if (intent.wantsCounsellingGuide) {
            return "TG EAPCET counselling: certificate verification → web options by rank → seat allotment in phases. Please verify dates on the official portal.";
        }
        return "I can help with TG EAPCET counselling, college prediction, and branch selection. Share your rank and category, or tap a suggested question below.";
    }

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
