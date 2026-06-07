package com.rankwise.predict;

import com.rankwise.predict.dto.CollegeRecommendation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredictServiceBucketTest {

    @Test
    void rank1000Closing2542IsSafeNotDream() throws Exception {
        String bucket = invokeClassifyByRatio(1000.0 / 2542.0);
        assertEquals("SAFE", bucket);
    }

    @Test
    void rank1000Closing900IsDream() throws Exception {
        String bucket = invokeClassifyByRatio(1000.0 / 900.0);
        assertEquals("DREAM", bucket);
    }

    @Test
    void rank1000Closing1050IsTarget() throws Exception {
        String bucket = invokeClassifyByRatio(1000.0 / 1050.0);
        assertEquals("TARGET", bucket);
    }

    @Test
    void assignBucketsNeverLabelsLowRatioAsDream() throws Exception {
        @SuppressWarnings("unchecked")
        List<CollegeRecommendation> tiered = (List<CollegeRecommendation>) invokeAssignBuckets(List.of(
                sample(2542, 1000.0 / 2542.0),
                sample(900, 1000.0 / 900.0),
                sample(1050, 1000.0 / 1050.0)
        ));

        assertEquals("SAFE", findBucket(tiered, 2542));
        assertEquals("DREAM", findBucket(tiered, 900));
        assertEquals("TARGET", findBucket(tiered, 1050));
    }

    private static String findBucket(List<CollegeRecommendation> tiered, int closingRank) {
        return tiered.stream()
                .filter(r -> r.closingRank() == closingRank)
                .map(CollegeRecommendation::bucket)
                .findFirst()
                .orElseThrow();
    }

    private static CollegeRecommendation sample(int closingRank, double ratio) {
        return new CollegeRecommendation(
                "COLL",
                "College",
                "CSE",
                "Computer Science and Engineering",
                closingRank,
                "BC-E",
                "BOYS",
                2025,
                "PHASE_1",
                "",
                Math.round(ratio * 1000.0) / 1000.0,
                true
        );
    }

    private static String invokeClassifyByRatio(double ratio) throws Exception {
        Method method = PredictService.class.getDeclaredMethod("classifyByRatio", double.class);
        method.setAccessible(true);
        return (String) method.invoke(null, ratio);
    }

    private static Object invokeAssignBuckets(List<CollegeRecommendation> ranked) throws Exception {
        Method method = PredictService.class.getDeclaredMethod("assignBuckets", List.class);
        method.setAccessible(true);
        return method.invoke(null, ranked);
    }
}
