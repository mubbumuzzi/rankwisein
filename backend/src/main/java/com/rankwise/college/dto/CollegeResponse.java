package com.rankwise.college.dto;

import java.time.LocalDateTime;

public record CollegeResponse(
        Long id,
        String code,
        String name,
        String location,
        String district,
        boolean autonomous,
        String website,
        LocalDateTime createdAt
) {
}
