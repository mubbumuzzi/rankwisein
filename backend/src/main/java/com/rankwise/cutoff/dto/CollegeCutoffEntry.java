package com.rankwise.cutoff.dto;

public record CollegeCutoffEntry(
        int year,
        String phase,
        String branchCode,
        String branchName,
        int closingRank
) {
}
