package com.rankwise.college.dto;

public record CollegeSummary(
        Long id,
        String code,
        String name,
        String location,
        String district
) {
}
