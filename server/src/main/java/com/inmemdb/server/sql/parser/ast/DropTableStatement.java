package com.inmemdb.server.sql.parser.ast;

/** AST node for: DROP TABLE name */
public class DropTableStatement implements Statement {

    private final String tableName;

    public DropTableStatement(String tableName) {
        this.tableName = tableName.toLowerCase();
    }

    public String getTableName() { return tableName; }
}
