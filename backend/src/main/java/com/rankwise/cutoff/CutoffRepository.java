package com.rankwise.cutoff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CutoffRepository extends JpaRepository<Cutoff, Long> {

    @Query("""
            SELECT c FROM Cutoff c
            JOIN FETCH c.college
            JOIN FETCH c.branch
            WHERE c.category = :category
              AND c.gender = :gender
              AND c.branch.code IN :branchCodes
              AND c.year = :year
              AND c.phase = :phase
            """)
    List<Cutoff> findForPrediction(@Param("category") String category,
                                   @Param("gender") String gender,
                                   @Param("branchCodes") List<String> branchCodes,
                                   @Param("year") int year,
                                   @Param("phase") String phase);
}
