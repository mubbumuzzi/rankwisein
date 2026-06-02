package com.rankwise.cutoff;

import com.rankwise.branch.Branch;
import com.rankwise.college.College;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cutoff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cutoff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "[year]", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String phase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "college_id", nullable = false)
    private College college;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String gender;

    @Column(name = "closing_rank", nullable = false)
    private Integer closingRank;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
