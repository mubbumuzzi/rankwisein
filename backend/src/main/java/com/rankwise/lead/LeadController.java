package com.rankwise.lead;

import com.rankwise.lead.dto.CreateLeadRequest;
import com.rankwise.lead.dto.LeadPredictResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leads")
@Tag(name = "Leads", description = "Student lead capture and college recommendations")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    @Operation(summary = "Save lead and return college recommendations")
    public LeadPredictResponse create(@Valid @RequestBody CreateLeadRequest request,
                                      HttpServletRequest httpRequest) {
        return leadService.createAndPredict(request, httpRequest);
    }
}
