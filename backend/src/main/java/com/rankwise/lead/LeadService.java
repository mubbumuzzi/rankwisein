package com.rankwise.lead;

import com.rankwise.common.csv.CsvExportService;
import com.rankwise.common.dto.PageResponse;
import com.rankwise.lead.dto.CreateLeadRequest;
import com.rankwise.lead.dto.LeadPredictResponse;
import com.rankwise.lead.dto.LeadResponse;
import com.rankwise.predict.PredictService;
import com.rankwise.predict.dto.PredictRequest;
import com.rankwise.predict.dto.PredictResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final PredictService predictService;
    private final CsvExportService csvExportService;

    public LeadService(LeadRepository leadRepository,
                       PredictService predictService,
                       CsvExportService csvExportService) {
        this.leadRepository = leadRepository;
        this.predictService = predictService;
        this.csvExportService = csvExportService;
    }

    @Transactional
    public LeadPredictResponse createAndPredict(CreateLeadRequest request, HttpServletRequest httpRequest) {
        String branches = String.join(",", request.preferredBranches());
        String mobile = normalizeMobile(request.mobile());

        Lead lead = leadRepository.save(Lead.builder()
                .name(blankToNull(request.name()))
                .mobile(mobile)
                .rank(request.rank())
                .category(request.category().trim())
                .gender(request.gender().trim().toUpperCase())
                .branch(branches)
                .build());

        PredictRequest predictRequest = new PredictRequest(
                request.rank(),
                request.category(),
                request.gender(),
                request.preferredBranches(),
                request.year(),
                request.phase()
        );
        PredictResponse recommendations = predictService.predict(predictRequest, httpRequest);
        return new LeadPredictResponse(lead.getId(), recommendations);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> list(int page, int size, String search, String category, String gender) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Specification<Lead> spec = LeadSpecifications.withFilters(search, category, gender);
        Page<Lead> result = leadRepository.findAll(
                spec,
                PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(result.map(LeadResponse::from));
    }

    @Transactional(readOnly = true)
    public String exportCsv(String search, String category, String gender) {
        Specification<Lead> spec = LeadSpecifications.withFilters(search, category, gender);
        List<Lead> leads = leadRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<String> headers = List.of(
                "Lead ID", "Name", "Mobile", "Rank", "Category", "Gender", "Branch", "Created At"
        );
        return csvExportService.toCsv(headers, leads, lead -> List.of(
                lead.getId(),
                lead.getName(),
                lead.getMobile(),
                lead.getRank(),
                lead.getCategory(),
                lead.getGender(),
                lead.getBranch(),
                lead.getCreatedAt()
        ));
    }

    private static String normalizeMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return null;
        }
        return mobile.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
