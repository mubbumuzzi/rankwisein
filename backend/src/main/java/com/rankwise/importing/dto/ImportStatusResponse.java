package com.rankwise.importing.dto;

public record ImportStatusResponse(
        Long importId,
        String status,
        int inserted,
        int skippedDuplicates,
        int invalidRows,
        long durationMs
) {
}
