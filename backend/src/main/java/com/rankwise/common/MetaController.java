package com.rankwise.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
@Tag(name = "Meta", description = "Reference data for form dropdowns")
public class MetaController {

    @GetMapping
    @Operation(summary = "Categories, genders, phases and supported years")
    public Map<String, List<?>> meta() {
        return Map.of(
                "categories", RankWiseConstants.CATEGORIES,
                "genders", RankWiseConstants.GENDERS,
                "phases", RankWiseConstants.PHASES,
                "years", RankWiseConstants.SUPPORTED_YEARS,
                "branches", RankWiseConstants.DEFAULT_BRANCHES
        );
    }
}
