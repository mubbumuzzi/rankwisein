package com.rankwise.importing.dto;

public record ApproveImportResponse(
        Long importId,
        String status,
        int inserted,
        int skippedDuplicates,
        int invalidRows,
        long durationMs
) {
}

