package com.rankwise.cutoff;

import com.rankwise.branch.Branch;
import com.rankwise.college.College;
import com.rankwise.college.CollegeRepository;
import com.rankwise.common.exception.ResourceNotFoundException;
import com.rankwise.cutoff.dto.CollegeCutoffResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollegeCutoffServiceTest {

    @Mock
    CutoffRepository cutoffRepository;

    @Mock
    CollegeRepository collegeRepository;

    CollegeCutoffService service;

    College college;
    Branch branch;

    @BeforeEach
    void setUp() {
        service = new CollegeCutoffService(cutoffRepository, collegeRepository);
        college = College.builder()
                .id(1L)
                .code("TST")
                .name("Test Engineering College")
                .location("Hyderabad")
                .district("Hyderabad")
                .autonomous(false)
                .build();
        branch = Branch.builder().id(10L).code("CSE").name("Computer Science").build();
    }

    @Test
    void lookupCutoffs_returnsAllYearsAndPhases() {
        when(collegeRepository.findById(1L)).thenReturn(Optional.of(college));
        when(cutoffRepository.findByCollegeCategoryGender(1L, "OC", "BOYS")).thenReturn(List.of(
                cutoff(2024, "PHASE_1", 5000),
                cutoff(2024, "FINAL_PHASE", 4500),
                cutoff(2025, "FINAL_PHASE", 4200)
        ));

        CollegeCutoffResponse response = service.lookupCutoffs(1L, "OC", "BOYS");

        assertThat(response.college().code()).isEqualTo("TST");
        assertThat(response.cutoffs()).hasSize(3);
        assertThat(response.cutoffs())
                .extracting(e -> e.year() + ":" + e.phase())
                .containsExactly("2025:FINAL_PHASE", "2024:PHASE_1", "2024:FINAL_PHASE");
    }

    @Test
    void lookupCutoffs_unknownCollegeThrows() {
        when(collegeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookupCutoffs(99L, "OC", "BOYS"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchColleges_findsByName() {
        when(collegeRepository.searchByNameOrCode(eq("Test Engineering"), any())).thenReturn(List.of(college));

        assertThat(service.searchColleges("Test Engineering", 10))
                .extracting(c -> c.code())
                .containsExactly("TST");
    }

    private Cutoff cutoff(int year, String phase, int closingRank) {
        return Cutoff.builder()
                .year(year)
                .phase(phase)
                .college(college)
                .branch(branch)
                .category("OC")
                .gender("BOYS")
                .closingRank(closingRank)
                .build();
    }
}
