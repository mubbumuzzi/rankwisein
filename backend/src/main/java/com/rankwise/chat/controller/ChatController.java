package com.rankwise.chat.controller;

import com.rankwise.chat.dto.*;
import com.rankwise.chat.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "RankWise AI Counsellor")
public class ChatController {

    private final ChatSessionService sessionService;
    private final ChatCounsellorService counsellorService;
    private final ChatAnalyticsService analyticsService;

    public ChatController(ChatSessionService sessionService,
                          ChatCounsellorService counsellorService,
                          ChatAnalyticsService analyticsService) {
        this.sessionService = sessionService;
        this.counsellorService = counsellorService;
        this.analyticsService = analyticsService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Start a new counselling chat session")
    public SessionResponse createSession(@Valid @RequestBody CreateSessionRequest request,
                                         HttpServletRequest http) {
        return sessionService.createSession(request, http);
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionResponse getSession(@PathVariable Long sessionId,
                                      @RequestHeader("X-Visitor-Token") String visitorToken) {
        return sessionService.getSession(sessionId, visitorToken);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageResponse> listMessages(@PathVariable Long sessionId,
                                                  @RequestHeader("X-Visitor-Token") String visitorToken) {
        return sessionService.listMessages(sessionId, visitorToken);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Send a message to the AI counsellor")
    public ChatMessageResponse sendMessage(@PathVariable Long sessionId,
                                           @RequestHeader("X-Visitor-Token") String visitorToken,
                                           @Valid @RequestBody SendMessageRequest request,
                                           HttpServletRequest http) {
        return counsellorService.sendMessage(sessionId, visitorToken, request, http);
    }

    @PutMapping("/sessions/{sessionId}/profile")
    @Operation(summary = "Update student profile for personalized recommendations")
    public StudentProfileResponse updateProfile(@PathVariable Long sessionId,
                                                @RequestHeader("X-Visitor-Token") String visitorToken,
                                                @RequestBody UpdateProfileRequest request) {
        return sessionService.updateProfile(sessionId, visitorToken, request);
    }

    @PostMapping("/sessions/{sessionId}/events")
    @Operation(summary = "Track analytics events (WhatsApp clicks, etc.)")
    public void trackEvent(@PathVariable Long sessionId,
                           @RequestHeader("X-Visitor-Token") String visitorToken,
                           @Valid @RequestBody TrackEventRequest request) {
        var session = sessionService.requireSession(sessionId, visitorToken);
        analyticsService.track(sessionId, session.getChatUserId(), request.eventType(), request.metadata());
    }

    @GetMapping("/suggested-questions")
    public List<String> suggestedQuestions() {
        return ChatPromptBuilder.defaultSuggestedQuestions();
    }
}
