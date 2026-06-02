package com.rankwise.college;

import com.rankwise.common.csv.CsvExportService;
import com.rankwise.common.dto.PageResponse;
import com.rankwise.college.dto.CollegeRequest;
import com.rankwise.college.dto.CollegeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/colleges")
@Tag(name = "Colleges", description = "College management")
public class CollegeController {

    private final CollegeService service;
    private final CsvExportService csvExportService;

    public CollegeController(CollegeService service, CsvExportService csvExportService) {
        this.service = service;
        this.csvExportService = csvExportService;
    }

    @GetMapping
    @Operation(summary = "List/search colleges (paginated)")
    public PageResponse<CollegeResponse> list(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) String district,
                                              Pageable pageable) {
        return PageResponse.from(service.search(q, district, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a college")
    public CollegeResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a college")
    public CollegeResponse create(@Valid @RequestBody CollegeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a college")
    public CollegeResponse update(@PathVariable Long id, @Valid @RequestBody CollegeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a college")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @Operation(summary = "Export colleges as CSV")
    public ResponseEntity<String> export(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) String district) {
        List<CollegeResponse> colleges = service.findAll(q, district);
        String csv = csvExportService.toCsv(
                List.of("Code", "Name", "Location", "District", "Autonomous", "Website"),
                colleges,
                c -> List.of(c.code(), c.name(),
                        c.location() == null ? "" : c.location(),
                        c.district() == null ? "" : c.district(),
                        c.autonomous(),
                        c.website() == null ? "" : c.website()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=colleges.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
