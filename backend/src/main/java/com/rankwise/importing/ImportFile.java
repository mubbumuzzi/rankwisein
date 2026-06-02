package com.rankwise.importing;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "[year]", nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String phase;

    @Column(nullable = false)
    private String status;

    @Column(name = "records_imported", nullable = false)
    private Integer recordsImported;

    @Column(name = "import_duration")
    private Long importDuration;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (recordsImported == null) {
            recordsImported = 0;
        }
    }
}

