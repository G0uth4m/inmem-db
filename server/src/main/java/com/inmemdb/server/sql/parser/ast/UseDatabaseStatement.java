package com.inmemdb.server.sql.parser.ast;

/** AST node for: USE database_name */
public class UseDatabaseStatement implements Statement {
    private final String databaseName;
    public UseDatabaseStatement(String databaseName) { this.databaseName = databaseName.toLowerCase(); }
    public String getDatabaseName() { return databaseName; }
}
