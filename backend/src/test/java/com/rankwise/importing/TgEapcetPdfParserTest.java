package com.rankwise.importing;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TgEapcetPdfParserTest {

    private final TgEapcetPdfParser parser = new TgEapcetPdfParser();

    @Test
    void stitchIgnoresAndFragmentsBeforeOrphanCollegeName() {
        List<String> cleaned = List.of(
                "VITS MBN COED PVT CSE COMPUTER SCIENCE AND ENGINEERING 44414 52383 44414 52383 44414 94040 44414 52383 44414 98246 44414 93850 44414 52383 44414 52383 44414 52383 145887 145887 44414 52383 JNTUH",
                "AND SCI R",
                "V N R VIGNANA JYOTHI INSTITUTE OF ENGG",
                "VJEC BACHUPALLY MDL COED PVT AID ARTIFICIAL INTELLIGENCE AND DATA SCIENCE 2512 2512 4316 7242 3243 3850 9271 9271 3526 3526 6559 6559 23273 23273 14252 15619 2512 11966 14609 19338 2575 2575 JNTUH"
        );
        List<String> stitched = parser.stitchRowsForTest(cleaned);
        String vjec = stitched.stream().filter(l -> l.startsWith("VJEC ")).findFirst().orElse("");
        assertTrue(vjec.contains("VIGNANA"), () -> vjec);
    }

    @Test
    void parsesVjecRowWhenCollegeNameStitchedBeforePlace() {
        String line = "VJEC V N R VIGNANA JYOTHI INSTITUTE OF ENGG BACHUPALLY MDL COED PVT CSE "
                + "COMPUTER SCIENCE AND ENGINEERING 1652 1933 2901 3726 2043 2043 1652 7365 "
                + "1846 1933 2926 5543 21970 21970 9065 9417 5481 5852 13232 16529 1652 1933 JNTUH";

        TgEapcetPdfParser.ParseResult result =
                parser.parseLines(java.util.List.of(line), TgEapcetPdfParser.PDF_COLUMNS_2024);
        assertFalse(result.rows().isEmpty());
        var row = result.rows().get(0);
        assertTrue(row.collegeName().contains("VIGNANA"));
        assertTrue(row.collegeLocation().contains("BACHUPALLY"));
    }

    @Test
    void parses2024FirstPhaseSampleLine() {
        String line = "AARM AAR MAHAVEER ENGINEERING COLLEGE BANDLAGUDA HYD COED PVT 2010 CSE "
                + "COMPUTER SCIENCE AND ENGINEERING 26588 29938 52666 62471 38568 38568 26588 108434 "
                + "38368 38368 53852 53852 70513 75671 70477 83930 30771 38034 60000 JNTUH";

        TgEapcetPdfParser.ParseResult result =
                parser.parseLines(java.util.List.of(line), TgEapcetPdfParser.PDF_COLUMNS_2024);
        assertFalse(result.rows().isEmpty());
        var row = result.rows().get(0);
        assertTrue(row.collegeCode().equals("AARM"));
        assertTrue(row.branchCode().equals("CSE"));
        assertTrue(row.ranks().size() >= 18);
    }

    @Test
    void parses2024PdfWhenPresent() throws Exception {
        Path pdf = Path.of("..", "pdf", "01_TGEAPCET_2024_FirstPhase_LastRanks.xlsx - TGEAPCET_FIRST PHASE.pdf");
        if (!Files.isRegularFile(pdf)) {
            return;
        }
        try (InputStream in = Files.newInputStream(pdf)) {
            TgEapcetPdfParser.ParseResult result = parser.parse(in);
            assertFalse(result.rows().isEmpty(), "expected parseable rows from 2024 PDF");
        }
    }
}
