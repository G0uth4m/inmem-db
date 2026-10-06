package com.inmemdb.server.catalog;

/**
 * Supported column data types for Phase 1.
 */
public enum DataType {
    INTEGER,
    VARCHAR,
    BOOLEAN,
    DOUBLE;

    /**
     * Parse a type string from SQL (e.g. "INTEGER", "VARCHAR(50)") into a DataType.
     */
    public static DataType fromSql(String token) {
        String upper = token.toUpperCase();
        if (upper.startsWith("VARCHAR")) return VARCHAR;
        if (upper.equals("INTEGER") || upper.equals("INT")) return INTEGER;
        if (upper.equals("BOOLEAN") || upper.equals("BOOL")) return BOOLEAN;
        if (upper.equals("DOUBLE") || upper.equals("FLOAT")) return DOUBLE;
        throw new IllegalArgumentException("Unsupported data type: " + token);
    }
}
