package com.rankwise.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rankwise.chat.dto.*;
import com.rankwise.chat.entity.*;
import com.rankwise.chat.repository.*;
import com.rankwise.common.exception.ResourceNotFoundException;
import com.rankwise.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatSessionService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ChatUserRepository chatUserRepository;
    private final StudentProfileRepository profileRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatAnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public ChatSessionService(ChatUserRepository chatUserRepository,
                              StudentProfileRepository profileRepository,
                              ChatSessionRepository sessionRepository,
                              ChatMessageRepository messageRepository,
                              ChatAnalyticsService analyticsService,
                              ObjectMapper objectMapper) {
        this.chatUserRepository = chatUserRepository;
        this.profileRepository = profileRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request, HttpServletRequest http) {
        ChatUser user = chatUserRepository.findByVisitorToken(request.visitorToken())
                .orElseGet(() -> chatUserRepository.save(ChatUser.builder()
                        .visitorToken(request.visitorToken())
                        .ipAddress(clientIp(http))
                        .userAgent(http.getHeader("User-Agent"))
                        .build()));

        ChatSession session = sessionRepository.save(ChatSession.builder()
                .chatUserId(user.getId())
                .title("Counselling chat")
                .messageCount(0)
                .leadCtaShown(false)
                .active(true)
                .build());

        analyticsService.track(session.getId(), user.getId(), ChatAnalyticsService.EVENT_CHAT_OPEN, null);

        String welcome = """
                Hi! I'm **RankWise AI Counsellor** — your TG EAPCET counselling guide.
                
                I can help with college prediction, branch selection, document checklists, and counselling steps.
                
                Share your rank to get started, or pick a suggested question below.""";

        saveAssistantMessage(session.getId(), welcome, null, null, null, null);

        return toSessionResponse(session, user, loadProfile(user.getId()));
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long sessionId, String visitorToken) {
        ChatSession session = requireSession(sessionId, visitorToken);
        ChatUser user = chatUserRepository.findById(session.getChatUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toSessionResponse(session, user, loadProfile(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(Long sessionId, String visitorToken) {
        requireSession(sessionId, visitorToken);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public StudentProfileResponse updateProfile(Long sessionId, String visitorToken, UpdateProfileRequest req) {
        ChatSession session = requireSession(sessionId, visitorToken);
        StudentProfile profile = profileRepository.findByChatUserId(session.getChatUserId())
                .orElseGet(() -> StudentProfile.builder().chatUserId(session.getChatUserId()).build());

        if (req.rank() != null) {
            profile.setRank(req.rank());
        }
        if (req.category() != null && !req.category().isBlank()) {
            profile.setCategory(req.category().trim());
        }
        if (req.gender() != null && !req.gender().isBlank()) {
            profile.setGender(req.gender().trim().toUpperCase());
        }
        if (req.preferredBranches() != null && !req.preferredBranches().isEmpty()) {
            profile.setPreferredBranches(req.preferredBranches().stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.joining(",")));
        }
        if (req.preferredLocation() != null) {
            profile.setPreferredLocation(req.preferredLocation().trim());
        }
        if (req.budget() != null) {
            profile.setBudget(req.budget().trim());
        }

        profileRepository.save(profile);
        return toProfileResponse(profile);
    }

    @Transactional
    public ChatMessage saveUserMessage(Long sessionId, String content) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        ChatMessage msg = messageRepository.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(ChatMessage.ROLE_USER)
                .content(content)
                .build());
        session.setMessageCount(session.getMessageCount() + 1);
        sessionRepository.save(session);
        return msg;
    }

    @Transactional
    public ChatMessage saveAssistantMessage(Long sessionId,
                                            String content,
                                            ChatStructuredPayload structured,
                                            String model,
                                            Integer promptTokens,
                                            Integer completionTokens) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        ChatMessage msg = messageRepository.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(ChatMessage.ROLE_ASSISTANT)
                .content(content)
                .structuredJson(structured != null ? writeJson(structured) : null)
                .model(model)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build());
        session.setMessageCount(session.getMessageCount() + 1);
        sessionRepository.save(session);
        return msg;
    }

    @Transactional
    public void updateCursorAgentId(Long sessionId, String cursorAgentId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        session.setCursorAgentId(cursorAgentId);
        sessionRepository.save(session);
    }

    @Transactional
    public void markLeadCtaShown(Long sessionId) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setLeadCtaShown(true);
            sessionRepository.save(s);
        });
    }

    public ChatSession requireSession(Long sessionId, String visitorToken) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        ChatUser user = chatUserRepository.findById(session.getChatUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.getVisitorToken().equals(visitorToken)) {
            throw new ChatException("Invalid session access.");
        }
        return session;
    }

    StudentProfile loadProfile(Long chatUserId) {
        return profileRepository.findByChatUserId(chatUserId).orElse(null);
    }

    StudentProfileResponse toProfileResponse(StudentProfile profile) {
        if (profile == null) {
            return new StudentProfileResponse(null, null, null, List.of(), null, null, false);
        }
        List<String> branches = profile.getPreferredBranches() == null || profile.getPreferredBranches().isBlank()
                ? List.of()
                : Arrays.asList(profile.getPreferredBranches().split(","));
        return new StudentProfileResponse(
                profile.getRank(),
                profile.getCategory(),
                profile.getGender(),
                branches,
                profile.getPreferredLocation(),
                profile.getBudget(),
                ChatIntentDetector.profileCompleteForPrediction(profile)
        );
    }

    ChatMessageResponse toMessageResponse(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getRole(),
                m.getContent(),
                readStructured(m.getStructuredJson()),
                m.getCreatedAt().format(ISO),
                List.of(),
                false,
                List.of(),
                false
        );
    }

    @Transactional(readOnly = true)
    public ChatSession getSessionEntity(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    }

    SessionResponse toSessionResponse(ChatSession session, ChatUser user, StudentProfile profile) {
        return new SessionResponse(
                session.getId(),
                user.getId(),
                user.getVisitorToken(),
                session.getTitle(),
                session.getMessageCount(),
                toProfileResponse(profile),
                ChatPromptBuilder.defaultSuggestedQuestions()
        );
    }

    List<ChatMessage> recentHistory(Long sessionId, int limit) {
        List<ChatMessage> recent = messageRepository.findRecent(sessionId, PageRequest.of(0, limit));
        List<ChatMessage> ordered = new ArrayList<>(recent);
        Collections.reverse(ordered);
        return ordered;
    }

    private ChatStructuredPayload readStructured(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ChatStructuredPayload.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
