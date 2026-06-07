package com.rankwise.chat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProfileRequest(
        @Min(1) Integer rank,
        @Size(max = 16) String category,
        @Size(max = 8) String gender,
        List<String> preferredBranches,
        @Size(max = 128) String preferredLocation,
        @Size(max = 64) String budget
) {
}
