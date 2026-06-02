package com.rankwise.predict.dto;

public record CollegeRecommendation(
        String collegeCode,
        String collegeName,
        String branchCode,
        String branchName,
        int closingRank,
        String category,
        String gender,
        int year,
        String phase,
        String bucket,
        double ratio,
        boolean preferredBranch
) {
}
