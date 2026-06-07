package com.rankwise.chat.service;

import com.rankwise.chat.entity.ChatAnalyticsEvent;
import com.rankwise.chat.repository.ChatAnalyticsEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatAnalyticsService {

    public static final String EVENT_CHAT_OPEN = "CHAT_OPEN";
    public static final String EVENT_MESSAGE_SENT = "MESSAGE_SENT";
    public static final String EVENT_WHATSAPP_COMMUNITY = "WHATSAPP_COMMUNITY_CLICK";
    public static final String EVENT_WHATSAPP_COUNSELOR = "WHATSAPP_COUNSELOR_CLICK";
    public static final String EVENT_PREDICTOR_USED = "PREDICTOR_USED";
    public static final String EVENT_COMPARISON_USED = "COMPARISON_USED";
    public static final String EVENT_FAQ_RAG = "FAQ_RAG_USED";

    private final ChatAnalyticsEventRepository repository;

    public ChatAnalyticsService(ChatAnalyticsEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void track(Long sessionId, Long chatUserId, String eventType, String metadata) {
        repository.save(ChatAnalyticsEvent.builder()
                .sessionId(sessionId)
                .chatUserId(chatUserId)
                .eventType(eventType)
                .metadataJson(metadata)
                .build());
    }
}
