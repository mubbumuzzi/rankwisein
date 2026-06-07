package com.rankwise.college;

import com.rankwise.college.dto.CollegeSummary;
import com.rankwise.cutoff.CollegeCutoffService;
import com.rankwise.cutoff.dto.CollegeCutoffResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colleges")
@Tag(name = "Colleges (public)", description = "College search and cutoff lookup for students")
public class PublicCollegeController {

    private final CollegeCutoffService collegeCutoffService;

    public PublicCollegeController(CollegeCutoffService collegeCutoffService) {
        this.collegeCutoffService = collegeCutoffService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search colleges by name or code")
    public List<CollegeSummary> search(@RequestParam String q,
                                       @RequestParam(defaultValue = "25") int limit) {
        return collegeCutoffService.searchColleges(q, limit);
    }

    @GetMapping("/{collegeId}/cutoffs")
    @Operation(summary = "Cutoff ranks for a college across all years and phases")
    public CollegeCutoffResponse cutoffs(@PathVariable Long collegeId,
                                         @RequestParam String category,
                                         @RequestParam String gender) {
        return collegeCutoffService.lookupCutoffs(collegeId, category, gender);
    }
}
