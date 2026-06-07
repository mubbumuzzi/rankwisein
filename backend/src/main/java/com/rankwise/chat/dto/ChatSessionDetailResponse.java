package com.rankwise.chat.dto;

import java.util.List;

public record ChatSessionDetailResponse(
        Long id,
        Long chatUserId,
        String title,
        StudentProfileResponse profile,
        List<ChatMessageResponse> messages
) {
}
