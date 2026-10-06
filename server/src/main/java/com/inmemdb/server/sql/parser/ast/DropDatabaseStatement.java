package com.inmemdb.server.sql.parser.ast;

/** AST node for: DROP DATABASE name */
public class DropDatabaseStatement implements Statement {
    private final String databaseName;
    public DropDatabaseStatement(String databaseName) { this.databaseName = databaseName.toLowerCase(); }
    public String getDatabaseName() { return databaseName; }
}
