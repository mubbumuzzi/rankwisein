package com.rankwise.common;

import java.util.List;

/**
 * Shared domain constants. Categories are stored at full granularity (the frontend
 * exposes all of them). Genders use BOYS/GIRLS to match the TG EAPCET PDF columns.
 */
public final class RankWiseConstants {

    private RankWiseConstants() {
    }

    public static final List<String> CATEGORIES = List.of(
            "OC", "BC-A", "BC-B", "BC-C", "BC-D", "BC-E",
            "SC-I", "SC-II", "SC-III", "ST", "EWS"
    );

    public static final List<String> GENDERS = List.of("BOYS", "GIRLS");

    public static final List<String> PHASES = List.of("PHASE_1", "PHASE_2", "FINAL_PHASE");

    public static final List<Integer> SUPPORTED_YEARS = List.of(2024, 2025);

    public static final List<String> DEFAULT_BRANCHES = List.of(
            // These are BRANCH CODES (must match what's stored in the DB / PDF import).
            // Frontend can map codes to friendly labels (e.g. INF -> IT, MEC -> MECH).
            "CSE", "INF", "AIM", "ECE", "EEE", "MEC", "CIV"
    );
}
