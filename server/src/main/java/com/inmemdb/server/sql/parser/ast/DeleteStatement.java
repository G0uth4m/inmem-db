package com.inmemdb.server.sql.parser.ast;

/** AST node for: DELETE FROM table [WHERE expr] */
public class DeleteStatement implements Statement {

    private final String tableName;
    private final Expression whereClause;

    public DeleteStatement(String tableName, Expression whereClause) {
        this.tableName   = tableName.toLowerCase();
        this.whereClause = whereClause;
    }

    public String getTableName()       { return tableName;   }
    public Expression getWhereClause() { return whereClause; }
}
