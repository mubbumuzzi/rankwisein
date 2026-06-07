package com.rankwise.chat.dto;

public record ChatSessionSummaryResponse(
        Long id,
        Long chatUserId,
        String title,
        int messageCount,
        boolean leadCtaShown,
        String createdAt,
        String updatedAt,
        Integer profileRank,
        String profileCategory
) {
}
