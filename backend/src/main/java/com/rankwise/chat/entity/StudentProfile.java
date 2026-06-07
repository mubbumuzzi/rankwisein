package com.rankwise.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_user_id", nullable = false, unique = true)
    private Long chatUserId;

    @Column(name = "[rank]")
    private Integer rank;

    @Column(length = 16)
    private String category;

    @Column(length = 8)
    private String gender;

    @Column(name = "preferred_branches", length = 512)
    private String preferredBranches;

    @Column(name = "preferred_location", length = 128)
    private String preferredLocation;

    @Column(length = 64)
    private String budget;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
