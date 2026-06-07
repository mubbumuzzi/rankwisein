package com.rankwise.chat.service;

import com.rankwise.chat.dto.*;
import com.rankwise.chat.entity.StudentProfile;
import com.rankwise.college.College;
import com.rankwise.college.CollegeRepository;
import com.rankwise.common.RankWiseConstants;
import com.rankwise.cutoff.Cutoff;
import com.rankwise.cutoff.CutoffRepository;
import com.rankwise.predict.PredictService;
import com.rankwise.predict.dto.CollegeRecommendation;
import com.rankwise.predict.dto.PredictRequest;
import com.rankwise.predict.dto.PredictResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatCollegeContextService {

    private static final int DISPLAY_LIMIT = 5;

    private final PredictService predictService;
    private final CollegeRepository collegeRepository;
    private final CutoffRepository cutoffRepository;

    public ChatCollegeContextService(PredictService predictService,
                                     CollegeRepository collegeRepository,
                                     CutoffRepository cutoffRepository) {
        this.predictService = predictService;
        this.collegeRepository = collegeRepository;
        this.cutoffRepository = cutoffRepository;
    }

    public CollegePredictionPayload buildPrediction(StudentProfile profile) {
        List<String> branches = parseBranches(profile.getPreferredBranches());
        PredictRequest request = new PredictRequest(
                profile.getRank(),
                profile.getCategory(),
                profile.getGender(),
                branches,
                RankWiseConstants.SUPPORTED_YEARS.getLast(),
                "FINAL_PHASE"
        );
        PredictResponse response = predictService.predict(request, null);
        return new CollegePredictionPayload(
                mapBucket(response.dream(), profile.getRank()),
                mapBucket(response.target(), profile.getRank()),
                mapBucket(response.safe(), profile.getRank()),
                "Based on " + RankWiseConstants.SUPPORTED_YEARS.getLast()
                        + " Final Phase cutoffs for " + profile.getCategory()
                        + " · " + profile.getGender() + ". Verify on official portal."
        );
    }

    public CollegeComparisonPayload buildComparison(String leftQuery, String rightQuery) {
        College left = findCollege(leftQuery);
        College right = findCollege(rightQuery);
        List<CollegeCompareCard> cards = new ArrayList<>();
        if (left != null) {
            cards.add(toCompareCard(left));
        }
        if (right != null) {
            cards.add(toCompareCard(right));
        }
        String summary = cards.size() == 2
                ? "Compare closing ranks, location, and branches. Verify placement and fee details from official college sources."
                : "Could not find both colleges. Try using college codes like VJEC, CBIT, or full names.";
        return new CollegeComparisonPayload(cards, summary);
    }

    public DocumentListPayload buildDocuments() {
        return new DocumentListPayload(
                List.of(
                        "TG EAPCET rank card",
                        "Hall ticket",
                        "Aadhaar card",
                        "SSC / 10th memo",
                        "Intermediate memo",
                        "Study certificates (Class VI to Intermediate)",
                        "Transfer certificate (TC)",
                        "Income certificate (if applicable)",
                        "Caste certificate (BC/SC/ST if applicable)"
                ),
                List.of("Local status certificate", "Passport size photos", "Parent ID proof"),
                "Requirements may vary by category. Please verify from the official counselling notification."
        );
    }

    public BranchAdvicePayload buildBranchAdvice(String interest) {
        if ("coding".equals(interest)) {
            return new BranchAdvicePayload(
                    List.of("CSE", "CSM", "CSD", "INF", "CSO"),
                    "For coding and software careers, CSE is most popular. AI/ML (CSM), Data Science (CSD), and IT (INF) are strong alternatives with slightly different cutoffs."
            );
        }
        if ("electronics".equals(interest)) {
            return new BranchAdvicePayload(
                    List.of("ECE", "EEE", "EIE"),
                    "ECE covers communications, embedded systems, and VLSI. EEE focuses on power and electrical systems."
            );
        }
        if ("govt".equals(interest)) {
            return new BranchAdvicePayload(
                    List.of("CIV", "MEC", "EEE", "ECE"),
                    "Civil and Mechanical align with state/central engineering services. EEE/ECE for PSU technical roles."
            );
        }
        return new BranchAdvicePayload(
                RankWiseConstants.DEFAULT_BRANCHES,
                "Share your interests (coding, electronics, core engineering) for tailored branch suggestions."
        );
    }

    public String formatPredictionForPrompt(CollegePredictionPayload payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("PREDICTION DATA (use in response):\n");
        appendBucket(sb, "DREAM", payload.dream());
        appendBucket(sb, "TARGET", payload.target());
        appendBucket(sb, "SAFE", payload.safe());
        sb.append("Note: ").append(payload.confidenceNote());
        return sb.toString();
    }

    public String formatComparisonForPrompt(CollegeComparisonPayload payload) {
        return payload.colleges().stream()
                .map(c -> c.name() + " (" + c.code() + ") @ " + c.location()
                        + " | Branches: " + c.popularBranches()
                        + " | Cutoffs: " + c.cutoffSummary())
                .collect(Collectors.joining("\n"));
    }

    private void appendBucket(StringBuilder sb, String label, List<CollegeBucketItem> items) {
        sb.append(label).append(": ");
        if (items == null || items.isEmpty()) {
            sb.append("none\n");
            return;
        }
        sb.append(items.stream()
                .map(i -> i.collegeName() + " " + i.branchCode() + " (closing " + i.closingRank() + ")")
                .collect(Collectors.joining(", ")));
        sb.append("\n");
    }

    private List<CollegeBucketItem> mapBucket(List<CollegeRecommendation> recs, int rank) {
        return recs.stream().limit(DISPLAY_LIMIT).map(r -> {
            double ratio = r.ratio();
            String confidence = ratio <= 0.85 || ratio >= 1.15 ? "HIGH" : "MEDIUM";
            return new CollegeBucketItem(
                    r.collegeCode(),
                    r.collegeName(),
                    r.branchCode(),
                    r.branchName(),
                    r.closingRank(),
                    r.ratio(),
                    r.bucket(),
                    confidence
            );
        }).toList();
    }

    private College findCollege(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String q = query.trim();
        Optional<College> byCode = collegeRepository.findByCode(q.toUpperCase(Locale.ROOT));
        if (byCode.isPresent()) {
            return byCode.get();
        }
        List<College> matches = collegeRepository.searchByNameOrCode(q, PageRequest.of(0, 1));
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private CollegeCompareCard toCompareCard(College college) {
        List<Cutoff> cutoffs = cutoffRepository.findForPrediction(
                "OC", "BOYS",
                List.of("CSE", "ECE", "EEE", "MEC", "CIV"),
                RankWiseConstants.SUPPORTED_YEARS.getLast(),
                "FINAL_PHASE"
        ).stream()
                .filter(c -> c.getCollege().getId().equals(college.getId()))
                .sorted(Comparator.comparingInt(Cutoff::getClosingRank))
                .limit(5)
                .toList();

        String cutoffSummary = cutoffs.isEmpty()
                ? "No recent cutoff data in RankWise"
                : cutoffs.stream()
                .map(c -> c.getBranch().getCode() + ":" + c.getClosingRank())
                .collect(Collectors.joining(", "));

        String branches = cutoffs.stream()
                .map(c -> c.getBranch().getCode())
                .distinct()
                .collect(Collectors.joining(", "));

        return new CollegeCompareCard(
                college.getCode(),
                college.getName(),
                college.getLocation() != null ? college.getLocation() : "Telangana",
                "JNTUH/OU — verify affiliation",
                branches.isBlank() ? "CSE, ECE, EEE common" : branches,
                cutoffSummary,
                "Strong option if cutoff matches your rank; verify campus and branch intake.",
                "Cutoffs vary by category/gender; visit campus if possible.",
                "Check tuition fee on official website; reimbursement may apply for eligible categories.",
                "Verify placement reports from official/college sources — RankWise does not guarantee placement data."
        );
    }

    private static List<String> parseBranches(String csv) {
        if (csv == null || csv.isBlank()) {
            return RankWiseConstants.DEFAULT_BRANCHES;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .toList();
    }
}
