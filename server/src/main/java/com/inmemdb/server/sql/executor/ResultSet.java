package com.inmemdb.server.sql.executor;

import com.inmemdb.server.catalog.Column;
import com.inmemdb.server.storage.Row;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the result of a SELECT query: an ordered list of column names and rows.
 * Also formats the result as a human-readable table for the CLI.
 */
public class ResultSet {

    private final List<String> columns;
    private final List<Row> rows;

    public ResultSet(List<String> columns, List<Row> rows) {
        this.columns = List.copyOf(columns);
        this.rows = List.copyOf(rows);
    }

    public List<String> getColumns() { return columns; }
    public List<Row> getRows()       { return rows;    }
    public int size()                { return rows.size(); }

    // -------------------------------------------------------------------------
    // Formatting
    // -------------------------------------------------------------------------

    /**
     * Renders the result set as a pipe-separated text table suitable for
     * sending over the wire to the CLI client.
     *
     * Example:
     *   id  | name
     *   ----+-------
     *   1   | Alice
     *   (2 rows)
     */
    public String format() {
        if (columns.isEmpty()) return "(0 rows)\n";

        // Compute column widths
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
        }
        for (Row row : rows) {
            for (int i = 0; i < columns.size(); i++) {
                Object val = row.get(columns.get(i));
                int len = val == null ? 4 : val.toString().length(); // "null" = 4
                widths[i] = Math.max(widths[i], len);
            }
        }

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(formatRow(columns.stream().map(c -> c).toList(), widths));
        sb.append("\n");

        // Separator
        for (int i = 0; i < columns.size(); i++) {
            sb.append("-".repeat(widths[i] + 2));
            if (i < columns.size() - 1) sb.append("+");
        }
        sb.append("\n");

        // Data rows
        for (Row row : rows) {
            List<String> vals = new ArrayList<>();
            for (String col : columns) {
                Object v = row.get(col);
                vals.add(v == null ? "null" : v.toString());
            }
            sb.append(formatRow(vals, widths));
            sb.append("\n");
        }

        // Footer
        sb.append("(").append(rows.size())
          .append(rows.size() == 1 ? " row)" : " rows)");
        sb.append("\n");

        return sb.toString();
    }

    private String formatRow(List<String> values, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            String val = values.get(i);
            sb.append(" ").append(pad(val, widths[i])).append(" ");
            if (i < values.size() - 1) sb.append("|");
        }
        return sb.toString();
    }

    private String pad(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }
}
