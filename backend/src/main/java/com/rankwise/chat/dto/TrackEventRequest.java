package com.rankwise.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrackEventRequest(
        @NotBlank @Size(max = 64) String eventType,
        @Size(max = 512) String metadata
) {
}
