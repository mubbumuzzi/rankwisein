package com.rankwise.lead;

import com.rankwise.common.dto.PageResponse;
import com.rankwise.lead.dto.LeadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/leads")
@Tag(name = "Admin Leads", description = "Lead management for administrators")
public class LeadAdminController {

    private final LeadService leadService;

    public LeadAdminController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    @Operation(summary = "List leads with search and filters")
    public PageResponse<LeadResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String gender) {
        return leadService.list(page, size, search, category, gender);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export leads as CSV")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String gender) {
        String csv = leadService.exportCsv(search, category, gender);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rankwise-leads.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
