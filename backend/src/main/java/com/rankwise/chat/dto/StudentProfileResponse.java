package com.rankwise.chat.dto;

import java.util.List;

public record StudentProfileResponse(
        Integer rank,
        String category,
        String gender,
        List<String> preferredBranches,
        String preferredLocation,
        String budget,
        boolean completeForPrediction
) {
}
