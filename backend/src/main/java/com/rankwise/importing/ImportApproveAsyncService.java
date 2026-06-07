package com.rankwise.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ImportApproveAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ImportApproveAsyncService.class);

    private final PdfImportService importService;

    public ImportApproveAsyncService(PdfImportService importService) {
        this.importService = importService;
    }

    @Async
    public void runApprove(Long importId) {
        log.info("Async approve starting for import {}", importId);
        try {
            importService.executeApprove(importId);
        } catch (Exception e) {
            log.error("Async approve failed for import {}", importId, e);
        }
    }
}
