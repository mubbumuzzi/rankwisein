package com.rankwise.chat.dto;

import java.util.List;

public record ChatMessageResponse(
        Long id,
        String role,
        String content,
        ChatStructuredPayload structured,
        String createdAt,
        List<String> suggestedQuestions,
        boolean showLeadCta,
        List<String> missingProfileFields
) {
}
