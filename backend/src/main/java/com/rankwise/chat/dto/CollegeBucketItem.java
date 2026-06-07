package com.rankwise.chat.dto;

public record CollegeBucketItem(
        String collegeCode,
        String collegeName,
        String branchCode,
        String branchName,
        int closingRank,
        double ratio,
        String bucket,
        String confidence
) {
}
