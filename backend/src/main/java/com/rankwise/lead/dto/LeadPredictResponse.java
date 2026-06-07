package com.rankwise.lead.dto;

import com.rankwise.predict.dto.PredictResponse;

public record LeadPredictResponse(
        Long leadId,
        PredictResponse recommendations
) {
}
