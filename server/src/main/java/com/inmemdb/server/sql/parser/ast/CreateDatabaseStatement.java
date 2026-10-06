package com.inmemdb.server.sql.parser.ast;

/** AST node for: CREATE DATABASE name */
public class CreateDatabaseStatement implements Statement {
    private final String databaseName;
    public CreateDatabaseStatement(String databaseName) { this.databaseName = databaseName.toLowerCase(); }
    public String getDatabaseName() { return databaseName; }
}
