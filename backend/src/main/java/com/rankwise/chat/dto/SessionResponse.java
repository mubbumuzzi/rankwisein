package com.rankwise.chat.dto;

import java.util.List;

public record SessionResponse(
        Long sessionId,
        Long chatUserId,
        String visitorToken,
        String title,
        int messageCount,
        StudentProfileResponse profile,
        List<String> suggestedQuestions
) {
}
