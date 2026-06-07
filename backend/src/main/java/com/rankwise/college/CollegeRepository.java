package com.rankwise.college;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long>, JpaSpecificationExecutor<College> {
    Optional<College> findByCode(String code);
    boolean existsByCode(String code);

    @Query("""
            SELECT c FROM College c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY c.name
            """)
    List<College> searchByNameOrCode(@Param("q") String q, org.springframework.data.domain.Pageable pageable);
}
