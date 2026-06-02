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
     * Prefer ratio-based tiers when they spread across buckets; otherwise split the sorted
     * list into competitive (dream), mid (target), and easier (safe) thirds by closing rank.
     */
    private static List<CollegeRecommendation> assignBuckets(List<CollegeRecommendation> ranked) {
        if (ranked.isEmpty()) {
            return List.of();
        }

        List<CollegeRecommendation> byRatio = new ArrayList<>(ranked.size());
        int dreamCount = 0;
        int targetCount = 0;
        int safeCount = 0;
        for (CollegeRecommendation rec : ranked) {
            String bucket = classifyByRatio(rec.ratio());
            byRatio.add(withBucket(rec, bucket));
            switch (bucket) {
                case "DREAM" -> dreamCount++;
                case "TARGET" -> targetCount++;
                case "SAFE" -> safeCount++;
                default -> { }
            }
        }

        // If ratio thresholds already produce multiple buckets, keep them.
        // Fallback splitting is only for the case where everything collapses into a single bucket
        // (common for categories like BC-E where closing ranks are far above top ranks).
        int nonEmptyBuckets = (dreamCount > 0 ? 1 : 0) + (targetCount > 0 ? 1 : 0) + (safeCount > 0 ? 1 : 0);
        if (nonEmptyBuckets >= 2) {
            return byRatio;
        }

        List<CollegeRecommendation> fallbackSorted = new ArrayList<>(ranked);
        fallbackSorted.sort(Comparator
                .comparing(CollegeRecommendation::preferredBranch).reversed()
                .thenComparingDouble(CollegeRecommendation::ratio).reversed()
                .thenComparingInt(CollegeRecommendation::closingRank));

        int poolSize = Math.min(fallbackSorted.size(), MAX_DREAM + MAX_TARGET + MAX_SAFE);
        List<CollegeRecommendation> result = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            String bucket = tierForIndex(i, poolSize);
            result.add(withBucket(fallbackSorted.get(i), bucket));
        }
        return result;
    }

    /**
     * Stretch / reach when your rank is worse than the historical closing rank.
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

    private static String tierForIndex(int index, int total) {
        if (total <= 1) {
            return "TARGET";
        }
        if (total == 2) {
            return index == 0 ? "DREAM" : "SAFE";
        }
        // For fallback mode we want a fixed cap per bucket.
        // total is already capped to (MAX_DREAM + MAX_TARGET + MAX_SAFE).
        int dreamEnd = Math.min(MAX_DREAM, total);
        int targetEnd = Math.min(dreamEnd + MAX_TARGET, total);
        if (index < dreamEnd) {
            return "DREAM";
        }
        if (index < targetEnd) {
            return "TARGET";
        }
        return "SAFE";
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
