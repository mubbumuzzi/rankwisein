package com.rankwise.chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ChatReplyAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ChatReplyAsyncService.class);

    private final ChatCounsellorService counsellorService;

    public ChatReplyAsyncService(ChatCounsellorService counsellorService) {
        this.counsellorService = counsellorService;
    }

    @Async
    public void runReply(Long sessionId, String userContent) {
        log.info("Async chat reply starting for session {}", sessionId);
        try {
            counsellorService.executeReply(sessionId, userContent);
        } catch (Exception e) {
            log.error("Async chat reply failed for session {}", sessionId, e);
        }
    }
}
