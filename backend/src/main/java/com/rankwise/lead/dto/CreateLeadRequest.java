package com.rankwise.lead.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateLeadRequest(
        @Size(max = 128) String name,
        @Pattern(regexp = "^$|^[6-9]\\d{9}$", message = "Mobile must be a valid 10-digit Indian number")
        String mobile,
        @Min(1) int rank,
        @NotBlank String category,
        @NotBlank String gender,
        @NotEmpty List<@NotBlank String> preferredBranches,
        Integer year,
        String phase
) {
}
