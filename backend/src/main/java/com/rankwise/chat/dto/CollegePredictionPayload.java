package com.rankwise.chat.dto;

import java.util.List;

public record CollegePredictionPayload(
        List<CollegeBucketItem> dream,
        List<CollegeBucketItem> target,
        List<CollegeBucketItem> safe,
        String confidenceNote
) {
}
