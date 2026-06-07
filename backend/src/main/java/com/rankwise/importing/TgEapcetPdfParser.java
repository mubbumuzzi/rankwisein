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
 * Supports legacy wide format (23 rank columns, SC sub-categories) and
 * 2024+ format (18 rank columns, year of establishment, tuition fee, aggregate SC).
 */
public class TgEapcetPdfParser {

    /** Legacy PDFs with SC-I / SC-II / SC-III split. */
    public static final List<String> PDF_COLUMNS_LEGACY = List.of(
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

    /** 2024 first-phase style: aggregate SC, EWS GEN/GIRLS OU, tuition fee before affiliation. */
    public static final List<String> PDF_COLUMNS_2024 = List.of(
            "OC_BOYS", "OC_GIRLS",
            "BC_A_BOYS", "BC_A_GIRLS",
            "BC_B_BOYS", "BC_B_GIRLS",
            "BC_C_BOYS", "BC_C_GIRLS",
            "BC_D_BOYS", "BC_D_GIRLS",
            "BC_E_BOYS", "BC_E_GIRLS",
            "SC_BOYS", "SC_GIRLS",
            "ST_BOYS", "ST_GIRLS",
            "EWS_GEN_OU", "EWS_GIRLS_OU"
    );

    /** @deprecated use {@link #PDF_COLUMNS_LEGACY} or parse result columns */
    @Deprecated
    public static final List<String> PDF_COLUMNS = PDF_COLUMNS_LEGACY;

    private static final Set<String> AFFILIATIONS = Set.of("JNTUH", "OU", "JNTUK", "KU", "JNTUA");
    private static final Set<String> MANAGEMENT_TYPES = Set.of("PVT", "GOVT", "PRIVATE", "GOVERNMENT");
    private static final Set<String> INSTITUTE_TYPES = Set.of("COED", "GIRLS", "BOYS");
    private static final Set<String> RESERVED_CODES = Set.of(
            "CODE", "INST", "PLACE", "DIST", "COED", "PVT", "GOVT", "BRANCH", "NAME", "TYPE",
            "COLLEGE", "INSTITUTE", "AFFILIATED", "BOYS", "GIRLS", "OC", "ST", "EWS", "GEN", "OU",
            "AND", "THE", "FOR", "NOT", "ALL", "ARE", "BUT", "CAN", "HAD", "HER", "WAS", "ONE", "OUR",
            "OUT", "DAY", "HIS", "HOW", "MAN", "NEW", "NOW", "OLD", "SEE", "WAY", "WHO", "BOY", "DID",
            "GET", "HAS", "HIM", "LET", "MAY", "PUT", "SAY", "SHE", "TOO", "USE"
    );
    private static final Pattern PAGE_MARKER = Pattern.compile("^(--\\s*)?\\d+\\s+of\\s+\\d+(\\s*--)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADER_LINE = Pattern.compile(
            ".*(INST(ITUTE)?\\s+CODE|INSTITUTE\\s+NAME|BRANCH\\s+NAME|AFFILIATED\\s+TO|PLACE\\s+DIST|TUITION\\s+FEE|YEAR\\s+OF).*",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern YEAR_OF_ESTAB_HEADER = Pattern.compile("YEAR\\s+OF", Pattern.CASE_INSENSITIVE);

    public record ParsedWideRow(
            String collegeCode,
            String collegeName,
            String collegeLocation,
            String branchCode,
            String branchName,
            List<Integer> ranks
    ) {
    }

    public record ParseResult(List<String> columns, List<ParsedWideRow> rows) {
    }

    public ParseResult parse(InputStream pdfStream) {
        String text = extractText(pdfStream);
        List<String> lines = normalizeLines(text);
        List<String> cleaned = removeNoise(lines);
        List<String> stitched = stitchRows(cleaned);
        return parseLines(stitched, detectColumnLayout(text));
    }

    List<String> stitchRowsForTest(List<String> cleaned) {
        return stitchRows(cleaned);
    }

    /** Package-visible for unit tests. */
    ParseResult parseLines(List<String> stitched) {
        return parseLines(stitched, PDF_COLUMNS_LEGACY);
    }

    ParseResult parseLines(List<String> stitched, List<String> columns) {
        List<ParsedWideRow> out = new ArrayList<>();
        for (String line : stitched) {
            ParsedWideRow row = tryParseRow(line, columns);
            if (row != null) {
                out.add(row);
            }
        }
        if (out.isEmpty()) {
            throw new ImportException("No parseable data rows found in PDF.");
        }
        out = enrichCollegeNames(out);
        return new ParseResult(columns, out);
    }

    /** Fill missing institute titles using better names from other rows in the same PDF. */
    private static List<ParsedWideRow> enrichCollegeNames(List<ParsedWideRow> rows) {
        Map<String, String> bestNameByCode = new HashMap<>();
        for (ParsedWideRow row : rows) {
            if (hasInstituteKeyword(row.collegeName())) {
                bestNameByCode.merge(row.collegeCode(), row.collegeName(),
                        (a, b) -> a.length() >= b.length() ? a : b);
            }
        }
        List<ParsedWideRow> enriched = new ArrayList<>(rows.size());
        for (ParsedWideRow row : rows) {
            if (!hasInstituteKeyword(row.collegeName()) && bestNameByCode.containsKey(row.collegeCode())) {
                enriched.add(new ParsedWideRow(
                        row.collegeCode(),
                        bestNameByCode.get(row.collegeCode()),
                        row.collegeLocation(),
                        row.branchCode(),
                        row.branchName(),
                        row.ranks()
                ));
            } else {
                enriched.add(row);
            }
        }
        return enriched;
    }

    private static List<String> detectColumnLayout(String text) {
        if (text == null || text.isBlank()) {
            return PDF_COLUMNS_LEGACY;
        }
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("TUITION FEE") || YEAR_OF_ESTAB_HEADER.matcher(upper).find()) {
            return PDF_COLUMNS_2024;
        }
        if (upper.contains("SC_III") || upper.contains("SC III")) {
            return PDF_COLUMNS_LEGACY;
        }
        // 2024 header splits SC without sub-category markers.
        if (upper.contains("TGEAPCET-2024") && !upper.contains("SC_I")) {
            return PDF_COLUMNS_2024;
        }
        return PDF_COLUMNS_LEGACY;
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
            if (l.contains("universityupdates") || l.contains("previousquestionpapers") || l.contains("telegram.me")) {
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
                    || u.equals("DIST") || u.equals("AFFILIATED") || u.equals("TO") || u.equals("ESTAB")) {
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
        String pendingCollegeName = null;

        for (String line : lines) {
            String first = firstToken(line);
            if (isInstituteCode(first)) {
                if (buf.length() > 0) {
                    buf.setLength(0);
                }
                String row = line;
                if (pendingCollegeName != null) {
                    row = first + " " + pendingCollegeName + " " + line.substring(first.length()).trim();
                    pendingCollegeName = null;
                }
                buf.append(row);
                if (endsWithAffiliation(row)) {
                    stitched.add(buf.toString());
                    buf.setLength(0);
                }
            } else if (buf.length() > 0) {
                buf.append(' ').append(line);
                if (endsWithAffiliation(buf.toString())) {
                    stitched.add(buf.toString());
                    buf.setLength(0);
                }
            } else if (isCollegeNameOrphanLine(line)) {
                pendingCollegeName = line;
            } else if (isNameSuffixFragment(line) && pendingCollegeName != null) {
                pendingCollegeName = pendingCollegeName + " " + line;
            } else if (!isIgnorableBetweenRows(line)) {
                pendingCollegeName = null;
            }
        }
        return stitched;
    }

    /** Institute name line printed above the code row in some 2025 PDFs. */
    private static boolean isCollegeNameOrphanLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        if (isInstituteCode(firstToken(line))) {
            return false;
        }
        if (endsWithAffiliation(line) || PAGE_MARKER.matcher(line).matches() || HEADER_LINE.matcher(line).matches()) {
            return false;
        }
        String upper = line.toUpperCase(Locale.ROOT);
        if (upper.contains("COMPUTER SCIENCE AND ENGINEERING")
                && !hasInstituteKeyword(upper)) {
            return false;
        }
        return hasInstituteKeyword(upper);
    }

    private static boolean isNameSuffixFragment(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String t = line.trim().toUpperCase(Locale.ROOT);
        return t.startsWith("AND TECH")
                || t.startsWith("AND ENGINEERING")
                || t.startsWith("AND SCI")
                || t.startsWith("AND ");
    }

    private static boolean isIgnorableBetweenRows(String line) {
        String u = line.toUpperCase(Locale.ROOT);
        return u.equals("N") || u.equals("CO") || u.equals("EDUCATIO") || u.startsWith("TGEAPCET");
    }

    private static boolean hasInstituteKeyword(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String u = text.toUpperCase(Locale.ROOT);
        return u.contains("COLLEGE")
                || u.contains("INSTITUTE")
                || u.contains("UNIVERSITY")
                || u.contains("POLYTECHNIC")
                || u.contains(" ENGG")
                || u.contains("ENGINEERING COLLEGE")
                || u.contains("SCHOOL OF ENGINEERING");
    }

    private ParsedWideRow tryParseRow(String line, List<String> columns) {
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

        int typeIdx = indexOfInstituteType(tokens);
        if (typeIdx < 0) {
            return null;
        }
        if (typeIdx + 2 >= tokens.length) {
            return null;
        }

        String management = tokens[typeIdx + 1].toUpperCase(Locale.ROOT);
        if (!MANAGEMENT_TYPES.contains(management)) {
            return null;
        }

        int branchCodeIdx = typeIdx + 2;
        if (isYear(tokens[branchCodeIdx])) {
            branchCodeIdx++;
        }

        if (branchCodeIdx >= tokens.length) {
            return null;
        }

        String branchCode = tokens[branchCodeIdx].toUpperCase(Locale.ROOT);
        if (!branchCode.matches("[A-Z0-9]{2,5}")) {
            return null;
        }

        int firstNumberIdx = -1;
        for (int i = branchCodeIdx + 1; i < tokens.length - 1; i++) {
            if (isRankToken(tokens[i])) {
                firstNumberIdx = i;
                break;
            }
        }
        if (firstNumberIdx < 0) {
            return null;
        }

        NamePlace namePlace = splitNameAndPlace(tokens, typeIdx);
        String collegeName = namePlace.name();
        String collegeLocation = namePlace.place();
        String branchName = String.join(" ", Arrays.copyOfRange(tokens, branchCodeIdx + 1, firstNumberIdx)).trim();
        if (branchName.isBlank() || collegeName.isBlank()) {
            return null;
        }
        if (HEADER_LINE.matcher(collegeName).matches()) {
            return null;
        }

        int rankEndIdx = tokens.length - 1;
        if (columns == PDF_COLUMNS_2024 && rankEndIdx > firstNumberIdx && isInt(tokens[rankEndIdx - 1])) {
            rankEndIdx--;
        }

        List<Integer> ranks = new ArrayList<>(columns.size());
        for (int i = firstNumberIdx; i < rankEndIdx && ranks.size() < columns.size(); i++) {
            if ("NA".equalsIgnoreCase(tokens[i])) {
                ranks.add(0);
            } else if (isInt(tokens[i])) {
                ranks.add(Integer.parseInt(tokens[i]));
            }
        }
        if (ranks.size() < columns.size()) {
            return null;
        }

        return new ParsedWideRow(
                collegeCode,
                truncate(collegeName, 255),
                truncate(collegeLocation.isBlank() ? null : collegeLocation, 255),
                branchCode,
                truncate(branchName, 255),
                ranks
        );
    }

    private record NamePlace(String name, String place) {
    }

    private static NamePlace splitNameAndPlace(String[] tokens, int typeIdx) {
        String segment = String.join(" ", Arrays.copyOfRange(tokens, 1, typeIdx)).trim();
        int end = findInstituteNameEnd(segment);
        if (end > 0 && end < segment.length()) {
            return new NamePlace(segment.substring(0, end).trim(), segment.substring(end).trim());
        }
        return new NamePlace(segment, "");
    }

    private static int findInstituteNameEnd(String segment) {
        String upper = segment.toUpperCase(Locale.ROOT);
        String[] suffixes = {
                " ENGINEERING COLLEGE",
                " COLLEGE OF ENGINEERING",
                " INSTITUTE OF ENGG",
                " INSTITUTE OF ENGINEERING",
                " INSTITUTE OF TECHNOLOGY",
                " INSTITUTE OF SCIENCE",
                " INSTITUTE OF SCI",
                " INST OF TECHNOLOGY",
                " INST OF ENGG",
                " UNIVERSITY COLLEGE OF ENGINEERING",
                " POLYTECHNIC",
                " SCHOOL OF ENGINEERING",
                " UNIVERSITY"
        };
        int best = -1;
        for (String suffix : suffixes) {
            int idx = upper.indexOf(suffix);
            if (idx >= 0) {
                int end = idx + suffix.length();
                if (end > best) {
                    best = end;
                }
            }
        }
        return best;
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

    private static int indexOfInstituteType(String[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            if (INSTITUTE_TYPES.contains(tokens[i].toUpperCase(Locale.ROOT))) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOf(String[] tokens, String value) {
        for (int i = 0; i < tokens.length; i++) {
            if (value.equalsIgnoreCase(tokens[i])) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isYear(String s) {
        if (!isInt(s) || s.length() != 4) {
            return false;
        }
        int year = Integer.parseInt(s);
        return year >= 1950 && year <= 2035;
    }

    private static boolean isRankToken(String s) {
        return isInt(s) || "NA".equalsIgnoreCase(s);
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
