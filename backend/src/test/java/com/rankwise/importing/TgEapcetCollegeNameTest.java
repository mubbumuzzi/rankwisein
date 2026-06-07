package com.rankwise.importing;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TgEapcetCollegeNameTest {

    private final TgEapcetPdfParser parser = new TgEapcetPdfParser();

    @Test
    void collegeNamesShouldIncludeInstituteTitleNotOnlyPlace() throws Exception {
        Path pdf = Path.of("..", "pdf", "TGEAPCET_2025_FINALPHASE_LASTRANKS.pdf");
        assumeTrue(Files.isRegularFile(pdf), "2025 PDF not in workspace");

        try (InputStream in = Files.newInputStream(pdf)) {
            TgEapcetPdfParser.ParseResult result = parser.parse(in);
            assertFalse(result.rows().isEmpty());

            Set<String> codes = Set.of("VJEC", "MVSR", "GRRR", "SNIS", "VNR");
            List<TgEapcetPdfParser.ParsedWideRow> samples = result.rows().stream()
                    .filter(r -> codes.contains(r.collegeCode()))
                    .limit(20)
                    .collect(Collectors.toList());

            for (TgEapcetPdfParser.ParsedWideRow row : samples) {
                String name = row.collegeName().toUpperCase();
                assertTrue(
                        name.contains("COLLEGE")
                                || name.contains("INSTITUTE")
                                || name.contains("UNIVERSITY")
                                || name.contains("ENGG"),
                        () -> row.collegeCode() + " name looks like place only: " + row.collegeName());
            }
        }
    }
}
