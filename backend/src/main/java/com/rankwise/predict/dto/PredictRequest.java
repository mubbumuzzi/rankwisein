package com.rankwise.predict.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PredictRequest(
        @Min(1) int rank,
        @NotBlank String category,
        @NotBlank String gender,
        @NotEmpty List<@NotBlank String> preferredBranches,
        Integer year,
        String phase
) {
}
