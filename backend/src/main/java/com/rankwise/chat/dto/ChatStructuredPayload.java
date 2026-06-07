package com.rankwise.chat.dto;

public record ChatStructuredPayload(
        String type,
        CollegePredictionPayload prediction,
        CollegeComparisonPayload comparison,
        DocumentListPayload documents,
        BranchAdvicePayload branchAdvice
) {
}
