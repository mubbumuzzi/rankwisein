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
import com.rankwise.importing.dto.ImportStatusResponse;
import com.rankwise.importing.dto.ImportUploadResponse;
import com.rankwise.importing.dto.RepairCollegeNamesResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
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
    private final ImportApproveAsyncService approveAsyncService;

    private final TgEapcetPdfParser parser = new TgEapcetPdfParser();

    public PdfImportService(AppProperties props,
                            ImportFileRepository importFileRepository,
                            ImportStagingRowRepository stagingRepository,
                            ImportLogRepository logRepository,
                            CollegeRepository collegeRepository,
                            BranchRepository branchRepository,
                            JdbcTemplate jdbcTemplate,
                            @Lazy ImportApproveAsyncService approveAsyncService) {
        this.props = props;
        this.importFileRepository = importFileRepository;
        this.stagingRepository = stagingRepository;
        this.logRepository = logRepository;
        this.collegeRepository = collegeRepository;
        this.branchRepository = branchRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.approveAsyncService = approveAsyncService;
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
            TgEapcetPdfParser.ParseResult parsed = parser.parse(in);
            List<ParsedWideRow> rows = parsed.rows();
            List<String> pdfColumns = parsed.columns();
            total = rows.size() * pdfColumns.size();

            for (ParsedWideRow wide : rows) {
                College college = findOrCreateCollege(wide.collegeCode(), wide.collegeName(), wide.collegeLocation());
                Branch branch = findOrCreateBranch(wide.branchCode(), wide.branchName());

                for (int i = 0; i < pdfColumns.size(); i++) {
                    String col = pdfColumns.get(i);
                    int closingRank = wide.ranks().get(i);
                    if (closingRank <= 0) {
                        invalid++;
                        continue;
                    }
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

        if ("IMPORTING".equals(importFile.getStatus())) {
            return toApproveResponse(importFile, stagingCounts(importId));
        }
        if ("IMPORTED".equals(importFile.getStatus())) {
            throw new ImportException("Import already approved. Current: IMPORTED");
        }
        if (!"STAGED".equals(importFile.getStatus())) {
            throw new ImportException("Import must be in STAGED status to approve. Current: " + importFile.getStatus());
        }

        importFile.setStatus("IMPORTING");
        importFileRepository.save(importFile);
        log(importId, "APPROVE queued (async)");

        approveAsyncService.runApprove(importId);

        StagingCounts counts = stagingCounts(importId);
        return new ApproveImportResponse(importId, "IMPORTING", 0, counts.skippedDuplicates(), counts.invalidRows(), 0);
    }

    @Transactional
    public void executeApprove(Long importId) {
        ImportFile importFile = importFileRepository.findById(importId)
                .orElseThrow(() -> new ImportException("Import not found: " + importId));
        if (!"IMPORTING".equals(importFile.getStatus())) {
            return;
        }

        Instant start = Instant.now();
        StagingCounts counts = stagingCounts(importId);

        try {
            validateResolvableCodes(importId);
            int inserted = bulkInsertCutoffs(importId);

            long durationMs = Duration.between(start, Instant.now()).toMillis();
            importFile.setStatus("IMPORTED");
            importFile.setRecordsImported(inserted);
            importFile.setImportDuration(durationMs);
            importFileRepository.save(importFile);
            log(importId, "APPROVED inserted=" + inserted + " skippedDup=" + counts.skippedDuplicates()
                    + " invalid=" + counts.invalidRows() + " durationMs=" + durationMs);
        } catch (Exception e) {
            importFile.setStatus("FAILED");
            importFileRepository.save(importFile);
            log(importId, "FAILED during approve: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ImportStatusResponse getImportStatus(Long importId) {
        ImportFile importFile = importFileRepository.findById(importId)
                .orElseThrow(() -> new ImportException("Import not found: " + importId));
        return toStatusResponse(importFile, stagingCounts(importId));
    }

    private int bulkInsertCutoffs(Long importId) {
        return jdbcTemplate.update("""
                INSERT INTO cutoff ([year], phase, college_id, branch_id, category, gender, closing_rank)
                SELECT i.[year], i.phase, c.id, b.id, s.category, s.gender, s.closing_rank
                FROM import_staging_row s
                INNER JOIN import_file i ON i.id = s.import_file_id
                INNER JOIN college c ON c.code = s.college_code
                INNER JOIN branch b ON b.code = s.branch_code
                WHERE s.import_file_id = ?
                  AND s.valid = 1
                  AND s.is_duplicate = 0
                  AND NOT EXISTS (
                    SELECT 1 FROM cutoff co
                    WHERE co.[year] = i.[year]
                      AND co.phase = i.phase
                      AND co.college_id = c.id
                      AND co.branch_id = b.id
                      AND co.category = s.category
                      AND co.gender = s.gender
                  )
                """, importId);
    }

    private void validateResolvableCodes(Long importId) {
        Integer orphans = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM import_staging_row s
                WHERE s.import_file_id = ?
                  AND s.valid = 1
                  AND s.is_duplicate = 0
                  AND (
                    NOT EXISTS (SELECT 1 FROM college c WHERE c.code = s.college_code)
                    OR NOT EXISTS (SELECT 1 FROM branch b WHERE b.code = s.branch_code)
                  )
                """, Integer.class, importId);
        if (orphans != null && orphans > 0) {
            throw new ImportException(orphans + " valid rows reference unknown college or branch codes.");
        }
    }

    private StagingCounts stagingCounts(Long importId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                  COALESCE(SUM(CASE WHEN valid = 0 THEN 1 ELSE 0 END), 0) AS invalid_cnt,
                  COALESCE(SUM(CASE WHEN valid = 1 AND is_duplicate = 1 THEN 1 ELSE 0 END), 0) AS dup_cnt
                FROM import_staging_row
                WHERE import_file_id = ?
                """, importId);
        return new StagingCounts(
                ((Number) row.get("invalid_cnt")).intValue(),
                ((Number) row.get("dup_cnt")).intValue()
        );
    }

    private ApproveImportResponse toApproveResponse(ImportFile importFile, StagingCounts counts) {
        return new ApproveImportResponse(
                importFile.getId(),
                importFile.getStatus(),
                importFile.getRecordsImported() != null ? importFile.getRecordsImported() : 0,
                counts.skippedDuplicates(),
                counts.invalidRows(),
                importFile.getImportDuration() != null ? importFile.getImportDuration() : 0
        );
    }

    private ImportStatusResponse toStatusResponse(ImportFile importFile, StagingCounts counts) {
        return new ImportStatusResponse(
                importFile.getId(),
                importFile.getStatus(),
                importFile.getRecordsImported() != null ? importFile.getRecordsImported() : 0,
                counts.skippedDuplicates(),
                counts.invalidRows(),
                importFile.getImportDuration() != null ? importFile.getImportDuration() : 0
        );
    }

    private record StagingCounts(int invalidRows, int skippedDuplicates) {
    }

    @Transactional
    public RepairCollegeNamesResponse repairCollegeNamesFromStoredImports() {
        Map<String, ParsedWideRow> bestByCode = new LinkedHashMap<>();
        List<ImportFile> imports = importFileRepository.findAll();
        imports.sort(Comparator.comparing(ImportFile::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        int filesProcessed = 0;
        for (ImportFile importFile : imports) {
            if (importFile.getFilePath() == null || importFile.getFilePath().isBlank()) {
                continue;
            }
            Path path = Path.of(importFile.getFilePath());
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try (InputStream in = Files.newInputStream(path)) {
                for (ParsedWideRow row : parser.parse(in).rows()) {
                    bestByCode.merge(row.collegeCode(), row, PdfImportService::preferBetterCollegeRow);
                }
                filesProcessed++;
            } catch (Exception e) {
                log(importFile.getId(), "Skipped during college name repair: " + e.getMessage());
            }
        }

        int updated = 0;
        for (ParsedWideRow row : bestByCode.values()) {
            Optional<College> existing = collegeRepository.findByCode(row.collegeCode());
            if (existing.isEmpty()) {
                continue;
            }
            College college = updateCollegeIfBetter(existing.get(), row.collegeName(), row.collegeLocation());
            if (!Objects.equals(existing.get().getName(), college.getName())
                    || !Objects.equals(existing.get().getLocation(), college.getLocation())) {
                updated++;
            }
        }

        return new RepairCollegeNamesResponse(filesProcessed, updated, bestByCode.size());
    }

    private static ParsedWideRow preferBetterCollegeRow(ParsedWideRow current, ParsedWideRow candidate) {
        if (hasInstituteKeyword(candidate.collegeName()) && !hasInstituteKeyword(current.collegeName())) {
            return candidate;
        }
        if (hasInstituteKeyword(current.collegeName()) && !hasInstituteKeyword(candidate.collegeName())) {
            return current;
        }
        return candidate.collegeName().length() >= current.collegeName().length() ? candidate : current;
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

    private College findOrCreateCollege(String code, String name, String location) {
        return collegeRepository.findByCode(code)
                .map(existing -> updateCollegeIfBetter(existing, name, location))
                .orElseGet(() -> collegeRepository.save(College.builder()
                        .code(code)
                        .name(truncate(name, 255))
                        .location(truncate(location, 255))
                        .district(null)
                        .autonomous(false)
                        .website(null)
                        .build()));
    }

    private College updateCollegeIfBetter(College existing, String name, String location) {
        boolean changed = false;
        if (shouldReplaceName(existing.getName(), name)) {
            existing.setName(truncate(name, 255));
            changed = true;
        }
        if (location != null && !location.isBlank()
                && (existing.getLocation() == null || existing.getLocation().isBlank())) {
            existing.setLocation(truncate(location, 255));
            changed = true;
        }
        return changed ? collegeRepository.save(existing) : existing;
    }

    private static boolean shouldReplaceName(String current, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        if (current == null || current.isBlank()) {
            return true;
        }
        if (looksLikePlaceOnly(current) && !looksLikePlaceOnly(candidate)) {
            return true;
        }
        return hasInstituteKeyword(candidate)
                && candidate.length() > current.length()
                && !looksLikePlaceOnly(candidate);
    }

    private static boolean looksLikePlaceOnly(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        if (hasInstituteKeyword(name)) {
            return false;
        }
        String u = name.toUpperCase(Locale.ROOT);
        return u.endsWith(" MDL") || u.endsWith(" RR") || u.endsWith(" HYD") || u.endsWith(" URBAN")
                || u.matches("^[A-Z0-9\\s\\-]{2,40}$");
    }

    private static boolean hasInstituteKeyword(String text) {
        String u = text.toUpperCase(Locale.ROOT);
        return u.contains("COLLEGE")
                || u.contains("INSTITUTE")
                || u.contains("UNIVERSITY")
                || u.contains("POLYTECHNIC")
                || u.contains(" ENGG")
                || u.contains("ENGINEERING COLLEGE");
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
        return switch (pdfCol) {
            case "SC_BOYS" -> new StagingNormalized("SC-I", "BOYS");
            case "SC_GIRLS" -> new StagingNormalized("SC-I", "GIRLS");
            case "EWS_GEN_OU" -> new StagingNormalized("EWS", "BOYS");
            case "EWS_GIRLS_OU" -> new StagingNormalized("EWS", "GIRLS");
            default -> normalizeLegacyColumn(pdfCol);
        };
    }

    private static StagingNormalized normalizeLegacyColumn(String pdfCol) {
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

