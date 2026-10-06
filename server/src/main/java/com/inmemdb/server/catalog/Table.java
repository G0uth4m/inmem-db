package com.inmemdb.server.catalog;

import com.inmemdb.server.storage.Row;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single table: its schema (ordered columns) and its rows.
 * All operations are unsynchronized in Phase 1 (single-threaded assumption).
 */
public class Table {

    private final String name;
    // Ordered map preserves column declaration order
    private final LinkedHashMap<String, Column> schema = new LinkedHashMap<>();
    private final List<Row> rows = new ArrayList<>();
    
    // Phase 4: Hash index for PRIMARY KEY
    private final java.util.Map<String, java.util.Map<Object, Row>> indexes = new java.util.HashMap<>();

    public Table(String name, List<Column> columns) {
        this.name = name.toLowerCase();
        for (Column col : columns) {
            schema.put(col.getName(), col);
            if (col.isPrimaryKey()) {
                indexes.put(col.getName(), new java.util.HashMap<>());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Schema helpers
    // -------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public List<Column> getColumns() {
        return new ArrayList<>(schema.values());
    }

    public boolean hasColumn(String colName) {
        return schema.containsKey(colName.toLowerCase());
    }

    public Column getColumn(String colName) {
        Column col = schema.get(colName.toLowerCase());
        if (col == null) {
            throw new RuntimeException("Column not found: " + colName);
        }
        return col;
    }
    
    // -------------------------------------------------------------------------
    // Index Helpers
    // -------------------------------------------------------------------------
    
    public boolean hasIndex(String colName) {
        return indexes.containsKey(colName.toLowerCase());
    }
    
    public Row getByIndex(String colName, Object value) {
        if (!hasIndex(colName)) return null;
        return indexes.get(colName.toLowerCase()).get(value);
    }

    // -------------------------------------------------------------------------
    // Row operations
    // -------------------------------------------------------------------------

    public void insertRow(Row row) {
        // Constraints
        for (Column col : schema.values()) {
            Object val = row.get(col.getName());
            if (val == null && !col.isNullable()) {
                throw new RuntimeException("Column " + col.getName() + " cannot be null");
            }
        }
        for (java.util.Map.Entry<String, java.util.Map<Object, Row>> entry : indexes.entrySet()) {
            Object val = row.get(entry.getKey());
            if (val != null && entry.getValue().containsKey(val)) {
                throw new RuntimeException("Duplicate entry for primary key column " + entry.getKey() + ": " + val);
            }
        }

        rows.add(row);
        for (java.util.Map.Entry<String, java.util.Map<Object, Row>> entry : indexes.entrySet()) {
            Object val = row.get(entry.getKey());
            if (val != null) {
                entry.getValue().put(val, row);
            }
        }
    }

    /** Returns a snapshot list of all rows (caller must not mutate). */
    public List<Row> getRows() {
        return rows;
    }

    public int deleteRows(java.util.function.Predicate<Row> predicate) {
        int count = 0;
        java.util.Iterator<Row> it = rows.iterator();
        while (it.hasNext()) {
            Row row = it.next();
            if (predicate.test(row)) {
                it.remove();
                for (java.util.Map.Entry<String, java.util.Map<Object, Row>> entry : indexes.entrySet()) {
                    Object val = row.get(entry.getKey());
                    if (val != null) {
                        entry.getValue().remove(val);
                    }
                }
                count++;
            }
        }
        return count;
    }

    public int updateRows(java.util.function.Predicate<Row> predicate,
                          Map<String, Object> assignments) {
        int count = 0;
        for (Row row : rows) {
            if (predicate.test(row)) {
                // Check constraints
                for (Map.Entry<String, Object> assignment : assignments.entrySet()) {
                    String colName = assignment.getKey();
                    Object newVal = assignment.getValue();
                    Column col = getColumn(colName);
                    if (newVal == null && !col.isNullable()) {
                        throw new RuntimeException("Column " + colName + " cannot be null");
                    }
                    if (indexes.containsKey(colName)) {
                        if (newVal != null && indexes.get(colName).containsKey(newVal)) {
                            if (!newVal.equals(row.get(colName))) {
                                throw new RuntimeException("Duplicate entry for primary key: " + newVal);
                            }
                        }
                    }
                }

                // Apply
                for (Map.Entry<String, Object> assignment : assignments.entrySet()) {
                    String colName = assignment.getKey();
                    Object newVal = assignment.getValue();
                    Object oldVal = row.get(colName);
                    if (indexes.containsKey(colName)) {
                        if (oldVal != null) indexes.get(colName).remove(oldVal);
                        if (newVal != null) indexes.get(colName).put(newVal, row);
                    }
                    row.set(colName, newVal);
                }
                count++;
            }
        }
        return count;
    }
}
