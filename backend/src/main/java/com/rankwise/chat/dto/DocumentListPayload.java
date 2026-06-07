package com.rankwise.chat.dto;

import java.util.List;

public record DocumentListPayload(
        List<String> required,
        List<String> optional,
        String note
) {
}
