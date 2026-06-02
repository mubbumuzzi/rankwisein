package com.rankwise.predict.dto;

import java.util.List;

public record PredictResponse(
        int rank,
        String category,
        String gender,
        int year,
        String phase,
        List<CollegeRecommendation> dream,
        List<CollegeRecommendation> target,
        List<CollegeRecommendation> safe
) {
}
