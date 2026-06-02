package com.rankwise.importing;

import com.rankwise.branch.Branch;
import com.rankwise.branch.BranchRepository;
import com.rankwise.college.College;
import com.rankwise.college.CollegeRepository;
import com.rankwise.common.RankWiseConstants;
import com.rankwise.common.exception.ImportException;
import com.rankwise.config.AppProperties;
import com.rankwise.importing.TgEapcetPdfParser.ParsedWideRow;
import com.rankwise.importing.dto.ApproveImportResponse;
import com.rankwise.importing.dto.ImportUploadResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class PdfImportService {

    private final AppProperties props;
    private final ImportFileRepository importFileRepository;
    private final ImportStagingRowRepository stagingRepository;
    private final ImportLogRepository logRepository;
    private final CollegeRepository collegeRepository;
    private final BranchRepository branchRepository;
    private final JdbcTemplate jdbcTemplate;

    private final TgEapcetPdfParser parser = new TgEapcetPdfParser();

    public PdfImportService(AppProperties props,
                            ImportFileRepository importFileRepository,
                            ImportStagingRowRepository stagingRepository,
                            ImportLogRepository logRepository,
                            CollegeRepository collegeRepository,
                            BranchRepository branchRepository,
                            JdbcTemplate jdbcTemplate) {
        this.props = props;
        this.importFileRepository = importFileRepository;
        this.stagingRepository = stagingRepository;
        this.logRepository = logRepository;
        this.collegeRepository = collegeRepository;
        this.branchRepository = branchRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportUploadResponse uploadAndParse(MultipartFile file, int year, String phase, HttpServletRequest req) {
        if (file == null || file.isEmpty()) {
            throw new ImportException("PDF file is required.");
        }
        if (year <= 0) {
            throw new ImportException("Invalid year.");
        }
        if (phase == null || phase.isBlank()) {
            throw new ImportException("Phase is required.");
        }
        String normalizedPhase = phase.trim().toUpperCase(Locale.ROOT);
        if (!RankWiseConstants.PHASES.contains(normalizedPhase)) {
            throw new ImportException("Unsupported phase: " + phase);
        }

        ImportFile importFile = importFileRepository.save(ImportFile.builder()
                .fileName(file.getOriginalFilename() == null ? "upload.pdf" : file.getOriginalFilename())
                .year(year)
                .phase(normalizedPhase)
                .status("UPLOADED")
                .recordsImported(0)
                .build());

        Path stored = storeFile(importFile.getId(), file);
        importFile.setFilePath(stored.toString());
        importFile.setStatus("PARSING");
        importFileRepository.save(importFile);

        Instant start = Instant.now();
        int total = 0;
        int valid = 0;
        int duplicates = 0;
        int invalid = 0;

        try (InputStream in = Files.newInputStream(stored)) {
            List<ParsedWideRow> rows = parser.parse(in);
            total = rows.size() * TgEapcetPdfParser.PDF_COLUMNS.size();

            for (ParsedWideRow wide : rows) {
                College college = findOrCreateCollege(wide.collegeCode(), wide.collegeName());
                Branch branch = findOrCreateBranch(wide.branchCode(), wide.branchName());

                for (int i = 0; i < TgEapcetPdfParser.PDF_COLUMNS.size(); i++) {
                    String col = TgEapcetPdfParser.PDF_COLUMNS.get(i);
                    int closingRank = wide.ranks().get(i);
                    StagingNormalized norm = normalize(col);

                    ImportStagingRow staging = ImportStagingRow.builder()
                            .importFileId(importFile.getId())
                            .collegeCode(college.getCode())
                            .collegeName(college.getName())
                            .branchCode(branch.getCode())
                            .branchName(branch.getName())
                            .category(norm.category)
                            .gender(norm.gender)
                            .closingRank(closingRank)
                            .valid(true)
                            .duplicate(false)
                            .build();

                    String error = validateRow(year, normalizedPhase, college.getId(), branch.getId(), norm.category, norm.gender, closingRank);
                    if (error != null) {
                        staging.setValid(false);
                        staging.setErrorMessage(error);
                        invalid++;
                    } else if (isDuplicate(year, normalizedPhase, college.getId(), branch.getId(), norm.category, norm.gender)) {
                        staging.setDuplicate(true);
                        duplicates++;
                        valid++; // still valid, but duplicate
                    } else {
                        valid++;
                    }
                    stagingRepository.save(staging);
                }
            }

            importFile.setStatus("STAGED");
            importFile.setImportDuration(Duration.between(start, Instant.now()).toMillis());
            importFileRepository.save(importFile);
            log(importFile.getId(), "Parsed and staged rows. total=" + total + " valid=" + valid + " dup=" + duplicates + " invalid=" + invalid);
        } catch (Exception e) {
            importFile.setStatus("FAILED");
            importFile.setImportDuration(Duration.between(start, Instant.now()).toMillis());
            importFileRepository.save(importFile);
            log(importFile.getId(), "FAILED: " + e.getMessage());
            throw e instanceof ImportException ? (ImportException) e : new ImportException("Import failed: " + e.getMessage(), e);
        }

        return new ImportUploadResponse(importFile.getId(), importFile.getStatus(), year, normalizedPhase, total, valid, duplicates, invalid);
    }

    @Transactional(readOnly = true)
    public Page<ImportStagingRow> listStaging(Long importId, Pageable pageable) {
        return stagingRepository.findByImportFileId(importId, pageable);
    }

    @Transactional
    public void deleteStagingRow(Long importId, Long rowId) {
        ImportStagingRow row = stagingRepository.findById(rowId)
                .orElseThrow(() -> new ImportException("Staging row not found: " + rowId));
        if (!Objects.equals(row.getImportFileId(), importId)) {
            throw new ImportException("Row does not belong to this import.");
        }
        stagingRepository.deleteById(rowId);
    }

    @Transactional
    public ApproveImportResponse approve(Long importId) {
        ImportFile importFile = importFileRepository.findById(importId)
                .orElseThrow(() -> new ImportException("Import not found: " + importId));
        if (!"STAGED".equals(importFile.getStatus())) {
            throw new ImportException("Import must be in STAGED status to approve. Current: " + importFile.getStatus());
        }

        Instant start = Instant.now();
        importFile.setStatus("IMPORTING");
        importFileRepository.save(importFile);

        int inserted = 0;
        int skippedDup = 0;
        int invalid = 0;

        List<ImportStagingRow> rows = jdbcTemplate.query(
                "SELECT id, import_file_id, college_code, college_name, branch_code, branch_name, category, gender, closing_rank, valid, is_duplicate, error_message, created_at " +
                        "FROM import_staging_row WHERE import_file_id = ?",
                (rs, i) -> ImportStagingRow.builder()
                        .id(rs.getLong("id"))
                        .importFileId(rs.getLong("import_file_id"))
                        .collegeCode(rs.getString("college_code"))
                        .collegeName(rs.getString("college_name"))
                        .branchCode(rs.getString("branch_code"))
                        .branchName(rs.getString("branch_name"))
                        .category(rs.getString("category"))
                        .gender(rs.getString("gender"))
                        .closingRank((Integer) rs.getObject("closing_rank"))
                        .valid(rs.getBoolean("valid"))
                        .duplicate(rs.getBoolean("is_duplicate"))
                        .errorMessage(rs.getString("error_message"))
                        .build(),
                importId
        );

        List<Object[]> batch = new ArrayList<>();
        for (ImportStagingRow r : rows) {
            if (!r.isValid()) {
                invalid++;
                continue;
            }
            if (r.isDuplicate()) {
                skippedDup++;
                continue;
            }

            College college = collegeRepository.findByCode(r.getCollegeCode())
                    .orElseThrow(() -> new ImportException("College missing for code: " + r.getCollegeCode()));
            Branch branch = branchRepository.findByCode(r.getBranchCode())
                    .orElseThrow(() -> new ImportException("Branch missing for code: " + r.getBranchCode()));

            batch.add(new Object[]{
                    importFile.getYear(),
                    importFile.getPhase(),
                    college.getId(),
                    branch.getId(),
                    r.getCategory(),
                    r.getGender(),
                    r.getClosingRank()
            });
        }

        try {
            int[] counts = jdbcTemplate.batchUpdate(
                    "INSERT INTO cutoff ([year], phase, college_id, branch_id, category, gender, closing_rank) " +
                            "SELECT ?, ?, ?, ?, ?, ?, ? " +
                            "WHERE NOT EXISTS (" +
                            "  SELECT 1 FROM cutoff c WHERE c.[year]=? AND c.phase=? AND c.college_id=? " +
                            "  AND c.branch_id=? AND c.category=? AND c.gender=?" +
                            ")",
                    batch.stream()
                            .map(row -> new Object[]{
                                    row[0], row[1], row[2], row[3], row[4], row[5], row[6],
                                    row[0], row[1], row[2], row[3], row[4], row[5]
                            })
                            .toList()
            );
            for (int c : counts) {
                inserted += c;
            }
        } catch (DataAccessException e) {
            importFile.setStatus("FAILED");
            importFileRepository.save(importFile);
            log(importId, "FAILED during approve: " + e.getMessage());
            throw new ImportException("Failed to insert cutoffs: " + e.getMessage(), e);
        }

        long durationMs = Duration.between(start, Instant.now()).toMillis();
        importFile.setStatus("IMPORTED");
        importFile.setRecordsImported(inserted);
        importFile.setImportDuration(durationMs);
        importFileRepository.save(importFile);
        log(importId, "APPROVED inserted=" + inserted + " skippedDup=" + skippedDup + " invalid=" + invalid);

        return new ApproveImportResponse(importId, importFile.getStatus(), inserted, skippedDup, invalid, durationMs);
    }

    private Path storeFile(Long importId, MultipartFile file) {
        try {
            Path dir = Path.of(props.getPdfImport().getStorageDir());
            Files.createDirectories(dir);
            String safeName = (file.getOriginalFilename() == null ? "upload.pdf" : file.getOriginalFilename())
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            Path dest = dir.resolve(importId + "_" + safeName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return dest;
        } catch (Exception e) {
            throw new ImportException("Failed to store uploaded PDF.", e);
        }
    }

    private void log(Long importId, String msg) {
        logRepository.save(ImportLog.builder().importFileId(importId).message(msg).build());
    }

    private College findOrCreateCollege(String code, String name) {
        return collegeRepository.findByCode(code)
                .orElseGet(() -> collegeRepository.save(College.builder()
                        .code(code)
                        .name(truncate(name, 255))
                        .location(null)
                        .district(null)
                        .autonomous(false)
                        .website(null)
                        .build()));
    }

    private Branch findOrCreateBranch(String code, String name) {
        return branchRepository.findByCode(code)
                .orElseGet(() -> branchRepository.save(Branch.builder()
                        .code(code)
                        .name(truncate(name, 255))
                        .build()));
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private record StagingNormalized(String category, String gender) {
    }

    private static StagingNormalized normalize(String pdfCol) {
        // Example: BC_A_BOYS => category=BC-A, gender=BOYS
        String[] parts = pdfCol.split("_");
        if (parts.length < 2) {
            throw new ImportException("Unexpected PDF column: " + pdfCol);
        }
        String gender = parts[parts.length - 1];
        String catRaw = String.join("_", Arrays.copyOf(parts, parts.length - 1));
        String category = catRaw
                .replace("BC_A", "BC-A")
                .replace("BC_B", "BC-B")
                .replace("BC_C", "BC-C")
                .replace("BC_D", "BC-D")
                .replace("BC_E", "BC-E")
                .replace("SC_I", "SC-I")
                .replace("SC_II", "SC-II")
                .replace("SC_III", "SC-III");
        return new StagingNormalized(category, gender);
    }

    private static String validateRow(int year, String phase, Long collegeId, Long branchId, String category, String gender, int closingRank) {
        if (collegeId == null || branchId == null) return "Missing college/branch id.";
        if (category == null || category.isBlank()) return "Missing category.";
        if (gender == null || gender.isBlank()) return "Missing gender.";
        if (closingRank <= 0) return "Invalid closing rank.";
        return null;
    }

    private boolean isDuplicate(int year, String phase, Long collegeId, Long branchId, String category, String gender) {
        List<Integer> rows = jdbcTemplate.query(
                "SELECT TOP 1 1 FROM cutoff WHERE [year]=? AND phase=? AND college_id=? AND branch_id=? AND category=? AND gender=?",
                (rs, i) -> rs.getInt(1),
                year, phase, collegeId, branchId, category, gender
        );
        return !rows.isEmpty();
    }
}

