package com.inmemdb.server.sql.parser.ast;

import java.util.Map;

/** AST node for: UPDATE table SET col1=val1, col2=val2 [WHERE expr] */
public class UpdateStatement implements Statement {

    private final String tableName;
    private final Map<String, Object> assignments; // col -> new value
    private final Expression whereClause;

    public UpdateStatement(String tableName, Map<String, Object> assignments, Expression whereClause) {
        this.tableName   = tableName.toLowerCase();
        this.assignments = new java.util.LinkedHashMap<>(assignments);
        this.whereClause = whereClause;
    }

    public String getTableName()           { return tableName;   }
    public Map<String, Object> getAssignments() { return assignments; }
    public Expression getWhereClause()     { return whereClause; }
}
