package com.rankwise.college.dto;

import jakarta.validation.constraints.NotBlank;

public record CollegeRequest(
        @NotBlank String code,
        @NotBlank String name,
        String location,
        String district,
        boolean autonomous,
        String website
) {
}
