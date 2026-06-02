package com.rankwise.common.csv;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

/**
 * Generic CSV builder. Caller supplies headers and a row-mapper per record.
 */
@Service
public class CsvExportService {

    public <T> String toCsv(List<String> headers, List<T> rows, Function<T, List<Object>> rowMapper) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinLine(headers.stream().map(Object.class::cast).toList()));
        for (T row : rows) {
            sb.append(joinLine(rowMapper.apply(row)));
        }
        return sb.toString();
    }

    private String joinLine(List<Object> cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(cells.get(i)));
        }
        line.append('\n');
        return line.toString();
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
