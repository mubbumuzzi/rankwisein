package com.rankwise.branch.dto;

import jakarta.validation.constraints.NotBlank;

public record BranchRequest(
        @NotBlank String code,
        @NotBlank String name
) {
}
