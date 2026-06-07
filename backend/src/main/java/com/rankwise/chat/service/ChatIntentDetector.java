package com.rankwise.chat.service;

import com.rankwise.chat.dto.*;
import com.rankwise.chat.entity.StudentProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatIntentDetector {

    private static final Pattern RANK = Pattern.compile(
            "(?i)(?:rank\\s*(?:is|:)?\\s*|my\\s+rank\\s*(?:is|:)?\\s*)(\\d{1,7})"
    );
    private static final Pattern COMPARE = Pattern.compile(
            "(?i)compare\\s+(.+?)\\s+(?:and|vs|versus|with)\\s+(.+)"
    );

    private ChatIntentDetector() {
    }

    public static DetectedIntent detect(String message, StudentProfile profile) {
        String lower = message.toLowerCase(Locale.ROOT);
        DetectedIntent intent = new DetectedIntent();

        Matcher rankMatcher = RANK.matcher(message);
        if (rankMatcher.find()) {
            intent.detectedRank = Integer.parseInt(rankMatcher.group(1));
        } else if (profile != null && profile.getRank() != null) {
            intent.detectedRank = profile.getRank();
        }

        Matcher compareMatcher = COMPARE.matcher(message);
        if (compareMatcher.find()) {
            intent.compareLeft = cleanCollegeQuery(compareMatcher.group(1));
            intent.compareRight = cleanCollegeQuery(compareMatcher.group(2));
            intent.wantsComparison = true;
        }

        intent.wantsDocuments = lower.contains("document") || lower.contains("certificate")
                || lower.contains("what to carry") || lower.contains("required for");

        intent.wantsPrediction = intent.detectedRank != null
                || lower.contains("college can i get")
                || lower.contains("safe target dream")
                || lower.contains("show colleges")
                || lower.contains("my colleges");

        intent.wantsBranchAdvice = lower.contains("branch") || lower.contains("coding")
                || lower.contains("electronics") || lower.contains("government job")
                || lower.contains("what branch");

        if (lower.contains("coding") || lower.contains("software") || lower.contains("programming")) {
            intent.branchInterest = "coding";
        } else if (lower.contains("electronic")) {
            intent.branchInterest = "electronics";
        } else if (lower.contains("government") || lower.contains("govt job") || lower.contains("psu")) {
            intent.branchInterest = "govt";
        }

        intent.wantsCounsellingGuide = lower.contains("counselling") || lower.contains("counseling")
                || lower.contains("web option") || lower.contains("how does") || lower.contains("phase");

        return intent;
    }

    public static List<String> missingProfileFields(StudentProfile profile) {
        List<String> missing = new ArrayList<>();
        if (profile == null || profile.getRank() == null) {
            missing.add("rank");
        }
        if (profile == null || profile.getCategory() == null || profile.getCategory().isBlank()) {
            missing.add("category");
        }
        if (profile == null || profile.getGender() == null || profile.getGender().isBlank()) {
            missing.add("gender");
        }
        if (profile == null || profile.getPreferredBranches() == null || profile.getPreferredBranches().isBlank()) {
            missing.add("preferredBranches");
        }
        return missing;
    }

    public static boolean profileCompleteForPrediction(StudentProfile profile) {
        return missingProfileFields(profile).isEmpty();
    }

    private static String cleanCollegeQuery(String raw) {
        return raw.replaceAll("(?i)college|institute|engineering", "").trim();
    }

    public static class DetectedIntent {
        public Integer detectedRank;
        public boolean wantsComparison;
        public String compareLeft;
        public String compareRight;
        public boolean wantsDocuments;
        public boolean wantsPrediction;
        public boolean wantsBranchAdvice;
        public boolean wantsCounsellingGuide;
        public String branchInterest;
    }
}
