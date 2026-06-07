package com.rankwise.chat.repository;

import com.rankwise.chat.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByChatUserId(Long chatUserId);
}
