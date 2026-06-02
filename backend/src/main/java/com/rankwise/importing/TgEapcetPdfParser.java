package com.rankwise.importing;

import com.rankwise.common.exception.ImportException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Deterministic parser for TG EAPCET last rank statement PDFs.
 */
public class TgEapcetPdfParser {

    public static final List<String> PDF_COLUMNS = List.of(
            "OC_BOYS", "OC_GIRLS",
            "BC_A_BOYS", "BC_A_GIRLS",
            "BC_B_BOYS", "BC_B_GIRLS",
            "BC_C_BOYS", "BC_C_GIRLS",
            "BC_D_BOYS", "BC_D_GIRLS",
            "BC_E_BOYS", "BC_E_GIRLS",
            "SC_I_BOYS", "SC_I_GIRLS",
            "SC_II_BOYS", "SC_II_GIRLS",
            "SC_III_BOYS", "SC_III_GIRLS",
            "ST_BOYS", "ST_GIRLS",
            "EWS_BOYS", "EWS_GIRLS"
    );

    private static final Set<String> AFFILIATIONS = Set.of("JNTUH", "OU", "JNTUK", "KU", "JNTUA");
    private static final Set<String> MANAGEMENT_TYPES = Set.of("PVT", "GOVT", "PRIVATE", "GOVERNMENT");
    private static final Set<String> RESERVED_CODES = Set.of(
            "CODE", "INST", "PLACE", "DIST", "COED", "PVT", "GOVT", "BRANCH", "NAME", "TYPE",
            "COLLEGE", "INSTITUTE", "AFFILIATED", "BOYS", "GIRLS", "OC", "ST", "EWS"
    );
    private static final Pattern PAGE_MARKER = Pattern.compile("^(--\\s*)?\\d+\\s+of\\s+\\d+(\\s*--)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADER_LINE = Pattern.compile(
            ".*(INST(ITUTE)?\\s+CODE|INSTITUTE\\s+NAME|BRANCH\\s+NAME|AFFILIATED\\s+TO|PLACE\\s+DIST).*",
            Pattern.CASE_INSENSITIVE
    );

    public record ParsedWideRow(
            String collegeCode,
            String collegeName,
            String branchCode,
            String branchName,
            List<Integer> ranks
    ) {
    }

    public List<ParsedWideRow> parse(InputStream pdfStream) {
        String text = extractText(pdfStream);
        List<String> lines = normalizeLines(text);
        List<String> cleaned = removeNoise(lines);
        List<String> stitched = stitchRows(cleaned);

        List<ParsedWideRow> out = new ArrayList<>();
        for (String line : stitched) {
            ParsedWideRow row = tryParseRow(line);
            if (row != null) {
                out.add(row);
            }
        }
        if (out.isEmpty()) {
            throw new ImportException("No parseable data rows found in PDF.");
        }
        return out;
    }

    private String extractText(InputStream pdfStream) {
        try (PDDocument doc = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        } catch (Exception e) {
            throw new ImportException("Failed to read PDF text.", e);
        }
    }

    private static List<String> normalizeLines(String text) {
        String[] raw = text.split("\\r?\\n");
        List<String> lines = new ArrayList<>(raw.length);
        for (String s : raw) {
            String t = s == null ? "" : s.trim();
            if (!t.isEmpty()) {
                t = t.replaceAll("\\s{2,}", " ");
                lines.add(t);
            }
        }
        return lines;
    }

    private static List<String> removeNoise(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String l : lines) {
            String u = l.toUpperCase(Locale.ROOT);
            if (u.startsWith("TGEAPCET-") || u.startsWith("TGEAPCET ")) {
                continue;
            }
            if (u.contains("LAST RANK STATEMENT")) {
                continue;
            }
            if (PAGE_MARKER.matcher(l).matches()) {
                continue;
            }
            if (HEADER_LINE.matcher(l).matches()) {
                continue;
            }
            if (u.equals("INST") || u.startsWith("INST CODE") || u.startsWith("CODE INSTITUTE NAME")) {
                continue;
            }
            if (u.equals("OC") || u.equals("BOYS") || u.equals("GIRLS")
                    || u.startsWith("BC_") || u.startsWith("SC_") || u.equals("ST") || u.equals("EWS")) {
                continue;
            }
            if (u.equals("EDUCATIO") || u.equals("N") || u.equals("CO") || u.equals("COLLEGE")
                    || u.equals("TYPE") || u.equals("BRANCH") || u.equals("CODE") || u.equals("PLACE")
                    || u.equals("DIST") || u.equals("AFFILIATED") || u.equals("TO")) {
                continue;
            }
            if (u.contains("AFFILIATED TO") && !isInstituteCode(firstToken(l))) {
                continue;
            }
            out.add(l);
        }
        return out;
    }

    private static List<String> stitchRows(List<String> lines) {
        List<String> stitched = new ArrayList<>();
        StringBuilder buf = new StringBuilder();

        for (String line : lines) {
            String first = firstToken(line);
            if (isInstituteCode(first)) {
                if (buf.length() > 0) {
                    buf.setLength(0);
                }
                buf.append(line);
                if (endsWithAffiliation(line)) {
                    stitched.add(buf.toString());
                    buf.setLength(0);
                }
            } else if (buf.length() > 0) {
                buf.append(' ').append(line);
                if (endsWithAffiliation(buf.toString())) {
                    stitched.add(buf.toString());
                    buf.setLength(0);
                }
            }
        }
        return stitched;
    }

    private ParsedWideRow tryParseRow(String line) {
        String[] tokens = line.split(" ");
        if (tokens.length < 15) {
            return null;
        }

        String collegeCode = tokens[0].toUpperCase(Locale.ROOT);
        if (!isInstituteCode(collegeCode)) {
            return null;
        }

        String affiliation = tokens[tokens.length - 1].toUpperCase(Locale.ROOT);
        if (!AFFILIATIONS.contains(affiliation)) {
            return null;
        }

        int coedIdx = indexOf(tokens, "COED");
        if (coedIdx < 0) {
            return null;
        }
        if (coedIdx + 2 >= tokens.length) {
            return null;
        }

        String management = tokens[coedIdx + 1].toUpperCase(Locale.ROOT);
        if (!MANAGEMENT_TYPES.contains(management)) {
            return null;
        }

        int branchCodeIdx = coedIdx + 2;
        String branchCode = tokens[branchCodeIdx].toUpperCase(Locale.ROOT);
        if (!branchCode.matches("[A-Z0-9]{2,5}")) {
            return null;
        }

        int firstNumberIdx = -1;
        for (int i = branchCodeIdx + 1; i < tokens.length - 1; i++) {
            if (isInt(tokens[i])) {
                firstNumberIdx = i;
                break;
            }
        }
        if (firstNumberIdx < 0) {
            return null;
        }

        String branchName = String.join(" ", Arrays.copyOfRange(tokens, branchCodeIdx + 1, firstNumberIdx)).trim();
        String collegeName = String.join(" ", Arrays.copyOfRange(tokens, 1, coedIdx)).trim();
        if (branchName.isBlank() || collegeName.isBlank()) {
            return null;
        }
        if (HEADER_LINE.matcher(collegeName).matches()) {
            return null;
        }

        List<Integer> ranks = new ArrayList<>();
        for (int i = firstNumberIdx; i < tokens.length - 1; i++) {
            if (isInt(tokens[i])) {
                ranks.add(Integer.parseInt(tokens[i]));
            }
        }
        if (ranks.size() < PDF_COLUMNS.size()) {
            return null;
        }
        if (ranks.size() > PDF_COLUMNS.size()) {
            ranks = ranks.subList(0, PDF_COLUMNS.size());
        }

        return new ParsedWideRow(
                collegeCode,
                truncate(collegeName, 255),
                branchCode,
                truncate(branchName, 255),
                ranks
        );
    }

    private static boolean isInstituteCode(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String u = token.toUpperCase(Locale.ROOT);
        return u.matches("[A-Z]{3,5}") && !RESERVED_CODES.contains(u);
    }

    private static boolean endsWithAffiliation(String line) {
        String last = lastToken(line);
        return last != null && AFFILIATIONS.contains(last.toUpperCase(Locale.ROOT));
    }

    private static String firstToken(String s) {
        String[] parts = s.trim().split(" ");
        return parts.length == 0 ? null : parts[0];
    }

    private static String lastToken(String s) {
        String[] parts = s.trim().split(" ");
        return parts.length == 0 ? null : parts[parts.length - 1];
    }

    private static int indexOf(String[] tokens, String value) {
        for (int i = 0; i < tokens.length; i++) {
            if (value.equalsIgnoreCase(tokens[i])) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isInt(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
