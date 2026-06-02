package com.rankwise.college;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long>, JpaSpecificationExecutor<College> {
    Optional<College> findByCode(String code);
    boolean existsByCode(String code);
}
