package com.rankwise.predict;

import com.rankwise.common.RankWiseConstants;
import com.rankwise.cutoff.Cutoff;
import com.rankwise.cutoff.CutoffRepository;
import com.rankwise.predict.dto.CollegeRecommendation;
import com.rankwise.predict.dto.PredictRequest;
import com.rankwise.predict.dto.PredictResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PredictService {

    private static final double COMFORTABLE_RATIO = 0.90;
    private static final double STRETCH_RATIO = 1.10;
    private static final int MAX_DREAM = 30;
    private static final int MAX_TARGET = 30;
    private static final int MAX_SAFE = 10;

    private final CutoffRepository cutoffRepository;
    private final StudentSearchRepository studentSearchRepository;

    public PredictService(CutoffRepository cutoffRepository,
                          StudentSearchRepository studentSearchRepository) {
        this.cutoffRepository = cutoffRepository;
        this.studentSearchRepository = studentSearchRepository;
    }

    @Transactional
    public PredictResponse predict(PredictRequest request, HttpServletRequest httpRequest) {
        int year = request.year() != null ? request.year() : RankWiseConstants.SUPPORTED_YEARS.getLast();
        String phase = request.phase() != null && !request.phase().isBlank()
                ? request.phase()
                : "FINAL_PHASE";

        String gender = normalizeGender(request.gender());
        Set<String> preferred = request.preferredBranches().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        logSearch(request, gender, httpRequest);

        List<Cutoff> cutoffs = cutoffRepository.findForPrediction(
                request.category(),
                gender,
                new ArrayList<>(preferred),
                year,
                phase);

        List<CollegeRecommendation> ranked = new ArrayList<>();
        for (Cutoff c : cutoffs) {
            if (c.getClosingRank() <= 0) {
                continue;
            }
            double ratio = (double) request.rank() / c.getClosingRank();
            boolean pref = preferred.contains(c.getBranch().getCode().toUpperCase());
            ranked.add(new CollegeRecommendation(
                    c.getCollege().getCode(),
                    c.getCollege().getName(),
                    c.getBranch().getCode(),
                    c.getBranch().getName(),
                    c.getClosingRank(),
                    c.getCategory(),
                    c.getGender(),
                    c.getYear(),
                    c.getPhase(),
                    "",
                    Math.round(ratio * 1000.0) / 1000.0,
                    pref
            ));
        }

        Comparator<CollegeRecommendation> sort = Comparator
                .comparing(CollegeRecommendation::preferredBranch).reversed()
                .thenComparingInt(CollegeRecommendation::closingRank)
                .thenComparing(CollegeRecommendation::collegeName, String.CASE_INSENSITIVE_ORDER);
        ranked.sort(sort);

        List<CollegeRecommendation> tiered = assignBuckets(ranked);

        List<CollegeRecommendation> dream = new ArrayList<>();
        List<CollegeRecommendation> target = new ArrayList<>();
        List<CollegeRecommendation> safe = new ArrayList<>();
        for (CollegeRecommendation rec : tiered) {
            switch (rec.bucket()) {
                case "DREAM" -> {
                    if (dream.size() < MAX_DREAM) {
                        dream.add(rec);
                    }
                }
                case "TARGET" -> {
                    if (target.size() < MAX_TARGET) {
                        target.add(rec);
                    }
                }
                case "SAFE" -> {
                    if (safe.size() < MAX_SAFE) {
                        safe.add(rec);
                    }
                }
                default -> { }
            }
        }

        dream.sort(dreamOrder());
        target.sort(targetOrder());
        safe.sort(safeOrder());

        return new PredictResponse(
                request.rank(),
                request.category(),
                gender,
                year,
                phase,
                dream,
                target,
                safe
        );
    }

    /**
     * Classify each college by how your rank compares to its historical closing rank.
     * Ratio = yourRank / closingRank (lower rank number = better performance).
     */
    private static List<CollegeRecommendation> assignBuckets(List<CollegeRecommendation> ranked) {
        List<CollegeRecommendation> result = new ArrayList<>(ranked.size());
        for (CollegeRecommendation rec : ranked) {
            result.add(withBucket(rec, classifyByRatio(rec.ratio())));
        }
        return result;
    }

    /**
     * ratio &lt;= 0.90 → SAFE (closing rank well above yours — easier admit)
     * ratio 0.90–1.10 → TARGET (close match)
     * ratio &gt; 1.10 → DREAM (closing rank better than yours — stretch / reach)
     */
    private static String classifyByRatio(double ratio) {
        if (ratio <= COMFORTABLE_RATIO) {
            return "SAFE";
        }
        if (ratio <= STRETCH_RATIO) {
            return "TARGET";
        }
        return "DREAM";
    }

    /** Dream: hardest first (lowest closing rank / highest ratio). */
    private static Comparator<CollegeRecommendation> dreamOrder() {
        return Comparator
                .comparing(CollegeRecommendation::preferredBranch).reversed()
                .thenComparingDouble(CollegeRecommendation::ratio).reversed()
                .thenComparingInt(CollegeRecommendation::closingRank);
    }

    /** Target: closest to cutoff first. */
    private static Comparator<CollegeRecommendation> targetOrder() {
        return Comparator
                .comparing(CollegeRecommendation::preferredBranch).reversed()
                .thenComparingDouble(r -> Math.abs(r.ratio() - 1.0))
                .thenComparingInt(CollegeRecommendation::closingRank);
    }

    /** Safe: most competitive safe options first (highest ratio, still &lt;= 0.90). */
    private static Comparator<CollegeRecommendation> safeOrder() {
        return Comparator
                .comparing(CollegeRecommendation::preferredBranch).reversed()
                .thenComparingDouble(CollegeRecommendation::ratio).reversed()
                .thenComparingInt(CollegeRecommendation::closingRank);
    }

    private static CollegeRecommendation withBucket(CollegeRecommendation rec, String bucket) {
        return new CollegeRecommendation(
                rec.collegeCode(),
                rec.collegeName(),
                rec.branchCode(),
                rec.branchName(),
                rec.closingRank(),
                rec.category(),
                rec.gender(),
                rec.year(),
                rec.phase(),
                bucket,
                rec.ratio(),
                rec.preferredBranch()
        );
    }

    private static String normalizeGender(String gender) {
        return switch (gender.trim().toUpperCase()) {
            case "MALE", "M" -> "BOYS";
            case "FEMALE", "F" -> "GIRLS";
            default -> gender.trim().toUpperCase();
        };
    }

    private void logSearch(PredictRequest request, String gender, HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            return;
        }
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }
        studentSearchRepository.save(StudentSearch.builder()
                .rank(request.rank())
                .category(request.category())
                .gender(gender)
                .preferredBranches(String.join(",", request.preferredBranches()))
                .ipAddress(ip)
                .build());
    }
}
