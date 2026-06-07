package com.rankwise.chat.dto;

import java.util.List;

public record CollegeComparisonPayload(
        List<CollegeCompareCard> colleges,
        String summary
) {
}
