package com.inmemdb.server.sql.executor;

import com.inmemdb.server.catalog.Catalog;
import com.inmemdb.server.catalog.Column;
import com.inmemdb.server.catalog.Table;
import com.inmemdb.server.sql.parser.ast.*;
import com.inmemdb.server.storage.Row;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Execution engine for Phase 1.
 * Walks the AST plan and performs operations against the in-memory Catalog.
 *
 * All SELECT queries do a full table scan (no indexes in Phase 1).
 * Returns either a {@link ResultSet} (for SELECT) or a String "OK" / row-count message.
 */
public class Executor {

    private final Catalog catalog;

    public Executor(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Execute any statement.
     *
     * @return ResultSet for SELECT; String acknowledgement for DDL/DML.
     */
    public Object execute(Statement stmt) {
        if (stmt instanceof CreateTableStatement s) return executeCreate(s);
        if (stmt instanceof DropTableStatement   s) return executeDrop(s);
        if (stmt instanceof InsertStatement      s) return executeInsert(s);
        if (stmt instanceof SelectStatement      s) return executeSelect(s);
        if (stmt instanceof UpdateStatement      s) return executeUpdate(s);
        if (stmt instanceof DeleteStatement      s) return executeDelete(s);
        throw new RuntimeException("Unknown statement type: " + stmt.getClass().getSimpleName());
    }

    // -------------------------------------------------------------------------
    // DDL
    // -------------------------------------------------------------------------

    private String executeCreate(CreateTableStatement s) {
        Table table = new Table(s.getTableName(), s.getColumns());
        catalog.createTable(table);
        return "OK";
    }

    private String executeDrop(DropTableStatement s) {
        catalog.dropTable(s.getTableName());
        return "OK";
    }

    // -------------------------------------------------------------------------
    // DML – INSERT
    // -------------------------------------------------------------------------

    private String executeInsert(InsertStatement s) {
        Table table = catalog.getTable(s.getTableName());
        List<Column> cols = table.getColumns();
        List<Object> vals = s.getValues();

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i < cols.size(); i++) {
            Object coercedVal = coerce(vals.get(i), cols.get(i));
            data.put(cols.get(i).getName(), coercedVal);
        }
        table.insertRow(new Row(data));
        return "OK";
    }

    private Object coerce(Object val, Column col) {
        if (val == null) return null;
        return switch (col.getType()) {
            case DOUBLE -> {
                if (val instanceof Integer i) yield i.doubleValue();
                yield val;
            }
            case INTEGER -> {
                if (val instanceof Double d) yield d.intValue();
                yield val;
            }
            default -> val;
        };
    }

    // -------------------------------------------------------------------------
    // DML – SELECT
    // -------------------------------------------------------------------------

    private ResultSet executeSelect(SelectStatement s) {
        Table table = catalog.getTable(s.getTableName());

        // Phase 4: Fast path for indexed primary key lookup
        List<Row> matched = null;
        if (s.getWhereClause() instanceof BinaryExpr b && b.getOp() == BinaryExpr.Op.EQ) {
            if (b.getLeft() instanceof ColumnRef c && b.getRight() instanceof Literal l) {
                if (table.hasIndex(c.getName())) {
                    Object coercedVal = coerce(l.getValue(), table.getColumn(c.getName()));
                    Row row = table.getByIndex(c.getName(), coercedVal);
                    matched = new ArrayList<>();
                    if (row != null) matched.add(row);
                }
            } else if (b.getRight() instanceof ColumnRef c && b.getLeft() instanceof Literal l) {
                if (table.hasIndex(c.getName())) {
                    Object coercedVal = coerce(l.getValue(), table.getColumn(c.getName()));
                    Row row = table.getByIndex(c.getName(), coercedVal);
                    matched = new ArrayList<>();
                    if (row != null) matched.add(row);
                }
            }
        }

        if (matched == null) {
            Predicate<Row> filter = buildFilter(s.getWhereClause());
            matched = table.getRows().stream()
                    .filter(filter)
                    .collect(Collectors.toList());
        }

        // Determine output columns
        List<String> outCols = s.isStar()
                ? table.getColumns().stream().map(Column::getName).collect(Collectors.toList())
                : s.getColumns().stream().map(String::toLowerCase).collect(Collectors.toList());

        // Project: build result rows with only requested columns
        List<Row> projected = new ArrayList<>();
        for (Row src : matched) {
            Row dest = new Row();
            for (String col : outCols) {
                dest.set(col, src.get(col));
            }
            projected.add(dest);
        }

        return new ResultSet(outCols, projected);
    }

    // -------------------------------------------------------------------------
    // DML – UPDATE
    // -------------------------------------------------------------------------

    private String executeUpdate(UpdateStatement s) {
        Table table = catalog.getTable(s.getTableName());
        Predicate<Row> filter = buildFilter(s.getWhereClause());
        
        Map<String, Object> assignments = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : s.getAssignments().entrySet()) {
            Column col = table.getColumn(entry.getKey());
            assignments.put(entry.getKey(), coerce(entry.getValue(), col));
        }

        int count = table.updateRows(filter, assignments);
        return count + " row(s) updated";
    }

    // -------------------------------------------------------------------------
    // DML – DELETE
    // -------------------------------------------------------------------------

    private String executeDelete(DeleteStatement s) {
        Table table = catalog.getTable(s.getTableName());
        Predicate<Row> filter = buildFilter(s.getWhereClause());
        int count = table.deleteRows(filter);
        return count + " row(s) deleted";
    }

    // -------------------------------------------------------------------------
    // Expression evaluator (WHERE clause)
    // -------------------------------------------------------------------------

    /**
     * Builds a Java Predicate<Row> from an Expression AST node.
     * null expression → always true (no WHERE clause).
     */
    private Predicate<Row> buildFilter(Expression expr) {
        if (expr == null) return row -> true;
        return row -> evaluateBool(expr, row);
    }

    private boolean evaluateBool(Expression expr, Row row) {
        if (expr instanceof BinaryExpr b) {
            return switch (b.getOp()) {
                case AND -> evaluateBool(b.getLeft(), row) && evaluateBool(b.getRight(), row);
                case OR  -> evaluateBool(b.getLeft(), row) || evaluateBool(b.getRight(), row);
                default  -> evaluateComparison(b, row);
            };
        }
        throw new RuntimeException("Cannot evaluate expression as boolean: " + expr);
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateComparison(BinaryExpr b, Row row) {
        Object left  = resolveValue(b.getLeft(),  row);
        Object right = resolveValue(b.getRight(), row);

        if (b.getOp() == BinaryExpr.Op.IS) {
            return left == null;
        } else if (b.getOp() == BinaryExpr.Op.IS_NOT) {
            return left != null;
        }

        int cmp = compare(left, right);
        return switch (b.getOp()) {
            case EQ  -> cmp == 0;
            case NEQ -> cmp != 0;
            case LT  -> cmp <  0;
            case LTE -> cmp <= 0;
            case GT  -> cmp >  0;
            case GTE -> cmp >= 0;
            default  -> throw new RuntimeException("Unexpected op in comparison: " + b.getOp());
        };
    }

    private Object resolveValue(Expression expr, Row row) {
        if (expr instanceof ColumnRef c) {
            if (!row.has(c.getName())) {
                throw new RuntimeException("Unknown column in WHERE: " + c.getName());
            }
            return row.get(c.getName());
        }
        if (expr instanceof Literal l) {
            return l.getValue();
        }
        throw new RuntimeException("Cannot resolve value from expression: " + expr);
    }

    /**
     * Compares two values of potentially mixed numeric types.
     * Supports: Integer, Double, String, Boolean.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;

        // Numeric promotion: Integer vs Double
        if (a instanceof Integer && b instanceof Double) a = ((Integer) a).doubleValue();
        if (a instanceof Double  && b instanceof Integer) b = ((Integer) b).doubleValue();

        if (a instanceof Comparable ca && a.getClass() == b.getClass()) {
            return ca.compareTo(b);
        }
        // Fallback: string comparison
        return a.toString().compareTo(b.toString());
    }
}
