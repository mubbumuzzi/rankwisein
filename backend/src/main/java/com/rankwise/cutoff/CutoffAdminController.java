package com.rankwise.cutoff;

import com.rankwise.common.RankWiseConstants;
import com.rankwise.common.exception.ImportException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cutoffs")
public class CutoffAdminController {

    private final JdbcTemplate jdbcTemplate;

    public CutoffAdminController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Danger zone: deletes ALL cutoff rows for a year+phase.
     * Use only to rollback a mistaken import (e.g. PHASE_1 PDF uploaded as FINAL_PHASE).
     */
    @PostMapping("/purge")
    @Transactional
    public Map<String, Object> purge(@RequestParam int year, @RequestParam String phase) {
        if (year <= 0) {
            throw new ImportException("Invalid year.");
        }
        if (phase == null || phase.isBlank()) {
            throw new ImportException("Phase is required.");
        }
        String normalizedPhase = phase.trim().toUpperCase(Locale.ROOT);
        if (!RankWiseConstants.PHASES.contains(normalizedPhase)) {
            throw new ImportException("Unsupported phase: " + phase);
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cutoff WHERE [year]=? AND phase=?",
                Integer.class,
                year, normalizedPhase
        );
        int deleted = jdbcTemplate.update(
                "DELETE FROM cutoff WHERE [year]=? AND phase=?",
                year, normalizedPhase
        );
        return Map.of(
                "year", year,
                "phase", normalizedPhase,
                "matched", count == null ? 0 : count,
                "deleted", deleted
        );
    }
}

