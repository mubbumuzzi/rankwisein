package com.rankwise.cutoff.dto;

import com.rankwise.college.dto.CollegeSummary;

import java.util.List;

public record CollegeCutoffResponse(
        CollegeSummary college,
        String category,
        String gender,
        List<CollegeCutoffEntry> cutoffs
) {
}
