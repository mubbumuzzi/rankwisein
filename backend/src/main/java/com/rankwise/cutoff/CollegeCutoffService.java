package com.rankwise.cutoff;

import com.rankwise.college.College;
import com.rankwise.college.CollegeRepository;
import com.rankwise.college.dto.CollegeSummary;
import com.rankwise.common.RankWiseConstants;
import com.rankwise.common.exception.ResourceNotFoundException;
import com.rankwise.common.exception.ValidationException;
import com.rankwise.cutoff.dto.CollegeCutoffEntry;
import com.rankwise.cutoff.dto.CollegeCutoffResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class CollegeCutoffService {

    private static final Comparator<CollegeCutoffEntry> ENTRY_ORDER = Comparator
            .comparing(CollegeCutoffEntry::year).reversed()
            .thenComparing(e -> phaseOrder(e.phase()))
            .thenComparing(CollegeCutoffEntry::branchCode);

    private final CutoffRepository cutoffRepository;
    private final CollegeRepository collegeRepository;

    public CollegeCutoffService(CutoffRepository cutoffRepository, CollegeRepository collegeRepository) {
        this.cutoffRepository = cutoffRepository;
        this.collegeRepository = collegeRepository;
    }

    @Transactional(readOnly = true)
    public List<CollegeSummary> searchColleges(String query, int limit) {
        if (query == null || query.trim().length() < 2) {
            throw new ValidationException("Enter at least 2 characters to search colleges.");
        }
        int capped = Math.min(Math.max(limit, 1), 50);
        return collegeRepository.searchByNameOrCode(query.trim(),
                        org.springframework.data.domain.PageRequest.of(0, capped))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CollegeCutoffResponse lookupCutoffs(Long collegeId, String category, String gender) {
        College college = collegeRepository.findById(collegeId)
                .orElseThrow(() -> new ResourceNotFoundException("College", collegeId));

        String normalizedCategory = requireCategory(category);
        String normalizedGender = requireGender(gender);

        List<CollegeCutoffEntry> entries = cutoffRepository
                .findByCollegeCategoryGender(collegeId, normalizedCategory, normalizedGender)
                .stream()
                .map(c -> new CollegeCutoffEntry(
                        c.getYear(),
                        c.getPhase(),
                        c.getBranch().getCode(),
                        c.getBranch().getName(),
                        c.getClosingRank()))
                .sorted(ENTRY_ORDER)
                .toList();

        return new CollegeCutoffResponse(
                toSummary(college),
                normalizedCategory,
                normalizedGender,
                entries
        );
    }

    private CollegeSummary toSummary(College college) {
        return new CollegeSummary(
                college.getId(),
                college.getCode(),
                college.getName(),
                college.getLocation(),
                college.getDistrict()
        );
    }

    private static String requireCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new ValidationException("Category is required.");
        }
        String value = category.trim();
        if (!RankWiseConstants.CATEGORIES.contains(value)) {
            throw new ValidationException("Invalid category: " + value);
        }
        return value;
    }

    private static String requireGender(String gender) {
        if (gender == null || gender.isBlank()) {
            throw new ValidationException("Gender is required.");
        }
        String normalized = switch (gender.trim().toUpperCase()) {
            case "MALE", "M" -> "BOYS";
            case "FEMALE", "F" -> "GIRLS";
            default -> gender.trim().toUpperCase();
        };
        if (!RankWiseConstants.GENDERS.contains(normalized)) {
            throw new ValidationException("Invalid gender: " + gender);
        }
        return normalized;
    }

    private static int phaseOrder(String phase) {
        return switch (phase) {
            case "PHASE_1" -> 1;
            case "PHASE_2" -> 2;
            case "FINAL_PHASE" -> 3;
            default -> 99;
        };
    }
}
