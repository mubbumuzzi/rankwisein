package com.rankwise.chat.dto;

public record CollegeCompareCard(
        String code,
        String name,
        String location,
        String affiliation,
        String popularBranches,
        String cutoffSummary,
        String pros,
        String cons,
        String feesNote,
        String placementsNote
) {
}
