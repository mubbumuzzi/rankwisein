package com.rankwise.importing;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_staging_row")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportStagingRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_file_id", nullable = false)
    private Long importFileId;

    @Column(name = "college_code")
    private String collegeCode;

    @Column(name = "college_name")
    private String collegeName;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "branch_name")
    private String branchName;

    private String category;

    private String gender;

    @Column(name = "closing_rank")
    private Integer closingRank;

    @Column(nullable = false)
    private boolean valid;

    @Column(name = "is_duplicate", nullable = false)
    private boolean duplicate;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

