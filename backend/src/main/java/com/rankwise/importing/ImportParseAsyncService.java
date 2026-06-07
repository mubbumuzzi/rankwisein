package com.rankwise.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ImportParseAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ImportParseAsyncService.class);

    private final PdfImportService importService;

    public ImportParseAsyncService(PdfImportService importService) {
        this.importService = importService;
    }

    @Async
    public void runParse(Long importId) {
        log.info("Async parse starting for import {}", importId);
        try {
            importService.executeParse(importId);
        } catch (Exception e) {
            log.error("Async parse failed for import {}", importId, e);
        }
    }
}
