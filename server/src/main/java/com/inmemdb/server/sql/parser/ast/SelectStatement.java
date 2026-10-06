package com.inmemdb.server.sql.parser.ast;

import java.util.List;

/**
 * AST node for: SELECT col1, col2 FROM table [WHERE expr]
 * columns is empty when SELECT * is used.
 */
public class SelectStatement implements Statement {

    private final List<String> columns; // empty = SELECT *
    private final String tableName;
    private final Expression whereClause; // null = no WHERE

    public SelectStatement(List<String> columns, String tableName, Expression whereClause) {
        this.columns = List.copyOf(columns);
        this.tableName = tableName.toLowerCase();
        this.whereClause = whereClause;
    }

    public List<String> getColumns()      { return columns;     }
    public String getTableName()          { return tableName;   }
    public Expression getWhereClause()    { return whereClause; }
    public boolean isStar()               { return columns.isEmpty(); }
}
