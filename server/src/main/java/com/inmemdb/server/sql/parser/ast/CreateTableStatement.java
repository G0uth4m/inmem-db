package com.inmemdb.server.sql.parser.ast;

import com.inmemdb.server.catalog.Column;
import java.util.List;

/** AST node for: CREATE TABLE name (col1 TYPE, col2 TYPE, ...) */
public class CreateTableStatement implements Statement {

    private final String tableName;
    private final List<Column> columns;

    public CreateTableStatement(String tableName, List<Column> columns) {
        this.tableName = tableName.toLowerCase();
        this.columns = List.copyOf(columns);
    }

    public String getTableName() { return tableName; }
    public List<Column> getColumns() { return columns; }
}
