package com.rankwise.importing.dto;

public record ImportStatusResponse(
        Long importId,
        String status,
        int year,
        String phase,
        int totalParsed,
        int validRows,
        int duplicateRows,
        int invalidRows,
        int inserted,
        long durationMs
) {
}
