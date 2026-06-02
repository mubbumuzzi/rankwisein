package com.rankwise.importing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportStagingRowRepository extends JpaRepository<ImportStagingRow, Long> {
    Page<ImportStagingRow> findByImportFileId(Long importFileId, Pageable pageable);
}

