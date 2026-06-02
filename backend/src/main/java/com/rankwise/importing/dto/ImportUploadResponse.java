package com.rankwise.importing.dto;

public record ImportUploadResponse(
        Long importId,
        String status,
        int year,
        String phase,
        int totalParsed,
        int validRows,
        int duplicateRows,
        int invalidRows
) {
}

