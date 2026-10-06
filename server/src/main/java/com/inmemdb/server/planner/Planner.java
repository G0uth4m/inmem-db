package com.inmemdb.server.planner;

import com.inmemdb.server.catalog.Catalog;
import com.inmemdb.server.catalog.Table;
import com.inmemdb.server.sql.parser.ast.*;

import java.util.Map;

/**
 * Phase 1 planner: validates the AST against the catalog (table/column existence)
 * and returns the same AST node as the execution plan (no cost-based optimisation yet).
 *
 * Throws {@link RuntimeException} with a descriptive message for any validation failure.
 */
public class Planner {

    private final Catalog catalog;

    public Planner(Catalog catalog) {
        this.catalog = catalog;
    }

    public Statement plan(Statement stmt) {
        if (stmt instanceof CreateTableStatement s) {
            validateCreate(s);
        } else if (stmt instanceof InsertStatement s) {
            validateInsert(s);
        } else if (stmt instanceof SelectStatement s) {
            validateSelect(s);
        } else if (stmt instanceof UpdateStatement s) {
            validateUpdate(s);
        } else if (stmt instanceof DeleteStatement s) {
            validateDelete(s);
        } else if (stmt instanceof DropTableStatement s) {
            validateDrop(s);
        } else {
            throw new RuntimeException("Unknown statement type: " + stmt.getClass().getSimpleName());
        }
        return stmt; // pass-through in Phase 1
    }

    // -------------------------------------------------------------------------
    // Validators
    // -------------------------------------------------------------------------

    private void validateCreate(CreateTableStatement s) {
        if (catalog.tableExists(s.getTableName())) {
            throw new RuntimeException("Table already exists: " + s.getTableName());
        }
        if (s.getColumns().isEmpty()) {
            throw new RuntimeException("CREATE TABLE must define at least one column.");
        }
    }

    private void validateInsert(InsertStatement s) {
        Table table = catalog.getTable(s.getTableName());
        int colCount = table.getColumns().size();
        int valCount = s.getValues().size();
        if (colCount != valCount) {
            throw new RuntimeException(
                    "Column count (" + colCount + ") does not match value count (" + valCount + ").");
        }
    }

    private void validateSelect(SelectStatement s) {
        Table table = catalog.getTable(s.getTableName());
        for (String col : s.getColumns()) {
            if (!table.hasColumn(col)) {
                throw new RuntimeException("Unknown column in SELECT: " + col);
            }
        }
        // WHERE columns are validated at execution time for simplicity in Phase 1
    }

    private void validateUpdate(UpdateStatement s) {
        Table table = catalog.getTable(s.getTableName());
        for (String col : s.getAssignments().keySet()) {
            if (!table.hasColumn(col)) {
                throw new RuntimeException("Unknown column in SET: " + col);
            }
        }
    }

    private void validateDelete(DeleteStatement s) {
        catalog.getTable(s.getTableName()); // throws if not found
    }

    private void validateDrop(DropTableStatement s) {
        if (!catalog.tableExists(s.getTableName())) {
            throw new RuntimeException("Table does not exist: " + s.getTableName());
        }
    }
}
