package com.rankwise.branch.dto;

import java.time.LocalDateTime;

public record BranchResponse(
        Long id,
        String code,
        String name,
        LocalDateTime createdAt
) {
}
