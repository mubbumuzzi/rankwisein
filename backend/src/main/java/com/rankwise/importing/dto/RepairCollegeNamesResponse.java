package com.rankwise.importing.dto;

public record RepairCollegeNamesResponse(
        int filesProcessed,
        int collegesUpdated,
        int collegesSeen
) {
}
