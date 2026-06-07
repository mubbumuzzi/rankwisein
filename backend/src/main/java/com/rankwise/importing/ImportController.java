package com.rankwise.importing;

import com.rankwise.importing.dto.ApproveImportResponse;
import com.rankwise.importing.dto.ImportStatusResponse;
import com.rankwise.importing.dto.ImportUploadResponse;
import com.rankwise.importing.dto.RepairCollegeNamesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/imports")
@Tag(name = "Imports", description = "TG EAPCET PDF import (deterministic parser)")
public class ImportController {

    private final PdfImportService importService;

    public ImportController(PdfImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a TG EAPCET cutoff PDF and stage rows for preview")
    public ImportUploadResponse uploadPdf(@RequestPart("file") MultipartFile file,
                                          @RequestParam int year,
                                          @RequestParam String phase,
                                          HttpServletRequest req) {
        return importService.uploadAndParse(file, year, phase, req);
    }

    @GetMapping("/{importId}/staging")
    @Operation(summary = "List staged rows (preview) for an import")
    public Page<ImportStagingRow> listStaging(@PathVariable Long importId, Pageable pageable) {
        return importService.listStaging(importId, pageable);
    }

    @DeleteMapping("/{importId}/staging/{rowId}")
    @Operation(summary = "Delete a staged row before approval")
    public void deleteStaging(@PathVariable Long importId, @PathVariable Long rowId) {
        importService.deleteStagingRow(importId, rowId);
    }

    @PostMapping("/{importId}/approve")
    @Operation(summary = "Approve an import: batch insert staged rows into cutoff (async)")
    public ApproveImportResponse approve(@PathVariable Long importId) {
        return importService.approve(importId);
    }

    @GetMapping("/{importId}")
    @Operation(summary = "Poll import status after async approve")
    public ImportStatusResponse status(@PathVariable Long importId) {
        return importService.getImportStatus(importId);
    }

    @PostMapping("/repair-college-names")
    @Operation(summary = "Re-parse stored import PDFs and fix college names that were saved as place-only")
    public RepairCollegeNamesResponse repairCollegeNames() {
        return importService.repairCollegeNamesFromStoredImports();
    }
}

