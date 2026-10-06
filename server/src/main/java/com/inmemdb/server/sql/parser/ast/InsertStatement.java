package com.inmemdb.server.sql.parser.ast;

import java.util.List;

/** AST node for: INSERT INTO table VALUES (v1, v2, ...) */
public class InsertStatement implements Statement {

    private final String tableName;
    private final List<Object> values;

    public InsertStatement(String tableName, List<Object> values) {
        this.tableName = tableName.toLowerCase();
        this.values = new java.util.ArrayList<>(values);
    }

    public String getTableName() { return tableName; }
    public List<Object> getValues() { return values; }
}
