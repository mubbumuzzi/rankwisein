package com.rankwise.chat.service;

import com.rankwise.chat.dto.*;
import com.rankwise.chat.entity.StudentProfile;

import java.util.List;

public final class ChatPromptBuilder {

    public static final String SYSTEM_PROMPT = """
            You are RankWise AI Counsellor — an expert TG EAPCET / TS EAMCET counselling assistant for Telangana engineering admissions.

            Your job is to help students make informed college decisions with practical, step-by-step guidance.

            You MUST:
            - Explain counselling process, web options, certificate verification, and seat allotment
            - Help with college and branch selection using provided cutoff/prediction data
            - Compare colleges honestly using supplied facts only
            - Guide students clearly and supportively (many are first-generation learners)
            - Use simple English; avoid jargon unless you explain it

            You MUST NOT:
            - Invent cutoff ranks, fees, or placement statistics not in the context
            - Guarantee seat allotment or admissions
            - Answer unrelated topics (politics, homework, etc.) — redirect to counselling

            If unsure or data is missing, say: "Please verify from the official TG EAPCET counselling website."

            Keep responses concise (under 200 words unless listing colleges). Use bullet points for steps and documents.
            """;

    private ChatPromptBuilder() {
    }

    public static String buildUserContext(String ragContext,
                                          StudentProfile profile,
                                          String predictionContext,
                                          String comparisonContext,
                                          List<String> missingFields) {
        StringBuilder sb = new StringBuilder();
        if (ragContext != null && !ragContext.isBlank()) {
            sb.append("KNOWLEDGE BASE:\n").append(ragContext).append("\n\n");
        }
        if (profile != null) {
            sb.append("STUDENT PROFILE:\n");
            if (profile.getRank() != null) {
                sb.append("- Rank: ").append(profile.getRank()).append("\n");
            }
            if (profile.getCategory() != null) {
                sb.append("- Category: ").append(profile.getCategory()).append("\n");
            }
            if (profile.getGender() != null) {
                sb.append("- Gender: ").append(profile.getGender()).append("\n");
            }
            if (profile.getPreferredBranches() != null) {
                sb.append("- Branches: ").append(profile.getPreferredBranches()).append("\n");
            }
            if (profile.getPreferredLocation() != null) {
                sb.append("- Location preference: ").append(profile.getPreferredLocation()).append("\n");
            }
            if (profile.getBudget() != null) {
                sb.append("- Budget: ").append(profile.getBudget()).append("\n");
            }
            sb.append("\n");
        }
        if (predictionContext != null && !predictionContext.isBlank()) {
            sb.append(predictionContext).append("\n\n");
        }
        if (comparisonContext != null && !comparisonContext.isBlank()) {
            sb.append("COMPARISON DATA:\n").append(comparisonContext).append("\n\n");
        }
        if (missingFields != null && !missingFields.isEmpty()) {
            sb.append("MISSING FOR PERSONALIZED PREDICTION: ")
                    .append(String.join(", ", missingFields))
                    .append(". Ask for these politely one at a time.\n\n");
        }
        return sb.toString();
    }

    public static List<String> defaultSuggestedQuestions() {
        return List.of(
                "What colleges can I get?",
                "Compare two colleges",
                "What documents are required?",
                "How does counselling work?",
                "What branches suit me?",
                "Show safe target dream colleges"
        );
    }

    public static ChatStructuredPayload structured(String type, Object payload) {
        return switch (type) {
            case "PREDICTION" -> new ChatStructuredPayload("PREDICTION", (CollegePredictionPayload) payload, null, null, null);
            case "COMPARISON" -> new ChatStructuredPayload("COMPARISON", null, (CollegeComparisonPayload) payload, null, null);
            case "DOCUMENTS" -> new ChatStructuredPayload("DOCUMENTS", null, null, (DocumentListPayload) payload, null);
            case "BRANCH_ADVICE" -> new ChatStructuredPayload("BRANCH_ADVICE", null, null, null, (BranchAdvicePayload) payload);
            default -> null;
        };
    }
}
