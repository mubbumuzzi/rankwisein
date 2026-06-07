package com.rankwise.chat.dto;

import java.util.List;

public record BranchAdvicePayload(
        List<String> recommendedBranches,
        String reasoning
) {
}
