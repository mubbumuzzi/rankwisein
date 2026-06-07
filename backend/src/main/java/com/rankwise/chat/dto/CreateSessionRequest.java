package com.rankwise.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank @Size(max = 64) String visitorToken
) {
}
