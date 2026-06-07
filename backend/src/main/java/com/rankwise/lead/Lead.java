package com.rankwise.lead;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lead")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128)
    private String name;

    @Column(length = 16)
    private String mobile;

    @Column(name = "[rank]", nullable = false)
    private Integer rank;

    @Column(nullable = false, length = 16)
    private String category;

    @Column(nullable = false, length = 8)
    private String gender;

    @Column(nullable = false, length = 256)
    private String branch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
