package com.inmemdb.server.sql.parser;

import com.inmemdb.server.catalog.Column;
import com.inmemdb.server.catalog.DataType;
import com.inmemdb.server.sql.lexer.Token;
import com.inmemdb.server.sql.lexer.TokenType;
import com.inmemdb.server.sql.parser.ast.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recursive-descent parser for Phase 1 SQL.
 *
 * Grammar (simplified):
 *   statement  → createTable | insert | select | update | delete | dropTable
 *   createTable→ CREATE TABLE name LPAREN colDef (COMMA colDef)* RPAREN
 *   insert     → INSERT INTO name VALUES LPAREN literal (COMMA literal)* RPAREN
 *   select     → SELECT (STAR | colList) FROM name [WHERE expr]
 *   update     → UPDATE name SET assignment (COMMA assignment)* [WHERE expr]
 *   delete     → DELETE FROM name [WHERE expr]
 *   dropTable  → DROP TABLE name
 *   expr       → andExpr (OR andExpr)*
 *   andExpr    → comparison (AND comparison)*
 *   comparison → IDENTIFIER op literal
 *   op         → EQ | NEQ | LT | LTE | GT | GTE
 */
public class Parser {

    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public Statement parse() {
        Statement stmt = parseStatement();
        // Optionally consume trailing semicolon
        if (peek().is(TokenType.SEMICOLON)) consume();
        expect(TokenType.EOF);
        return stmt;
    }

    // -------------------------------------------------------------------------
    // Statement parsers
    // -------------------------------------------------------------------------

    private Statement parseStatement() {
        Token t = peek();
        return switch (t.getType()) {
            case CREATE -> parseCreate();
            case INSERT -> parseInsert();
            case SELECT -> parseSelect();
            case UPDATE -> parseUpdate();
            case DELETE -> parseDelete();
            case DROP   -> parseDrop();
            case USE    -> parseUse();
            case SHOW   -> parseShow();
            default     -> throw new RuntimeException(
                    "Unexpected token: " + t + ". Expected CREATE/INSERT/SELECT/UPDATE/DELETE/DROP/USE/SHOW.");
        };
    }

    // CREATE TABLE name (col TYPE, ...)
    // CREATE DATABASE name
    private Statement parseCreate() {
        consume(TokenType.CREATE);
        if (peek().is(TokenType.DATABASE)) {
            consume();
            String dbName = consumeIdentifier();
            return new CreateDatabaseStatement(dbName);
        }
        consume(TokenType.TABLE);
        String tableName = consumeIdentifier();
        consume(TokenType.LPAREN);

        List<Column> columns = new ArrayList<>();
        columns.add(parseColumnDef());
        while (peek().is(TokenType.COMMA)) {
            consume();
            columns.add(parseColumnDef());
        }

        consume(TokenType.RPAREN);
        return new CreateTableStatement(tableName, columns);
    }

    private Column parseColumnDef() {
        String colName = consumeIdentifier();
        String typeToken = consumeTypeToken();
        DataType dt = DataType.fromSql(typeToken);

        boolean isPrimaryKey = false;
        boolean isNullable = true;

        if (peek().is(TokenType.PRIMARY)) {
            consume();
            consume(TokenType.KEY);
            isPrimaryKey = true;
            isNullable = false;
        } else if (peek().is(TokenType.NOT)) {
            consume();
            consume(TokenType.NULL);
            isNullable = false;
        }

        return new Column(colName, dt, isPrimaryKey, isNullable);
    }

    // INSERT INTO name VALUES (v1, v2, ...)
    private Statement parseInsert() {
        consume(TokenType.INSERT);
        consume(TokenType.INTO);
        String tableName = consumeIdentifier();
        consume(TokenType.VALUES);
        consume(TokenType.LPAREN);

        List<Object> values = new ArrayList<>();
        values.add(parseLiteralValue());
        while (peek().is(TokenType.COMMA)) {
            consume();
            values.add(parseLiteralValue());
        }

        consume(TokenType.RPAREN);
        return new InsertStatement(tableName, values);
    }

    // SELECT * | col,... FROM name [WHERE expr]
    private Statement parseSelect() {
        consume(TokenType.SELECT);

        List<String> columns = new ArrayList<>();
        if (peek().is(TokenType.STAR)) {
            consume(); // SELECT * — leave columns empty
        } else {
            columns.add(consumeIdentifier());
            while (peek().is(TokenType.COMMA)) {
                consume();
                columns.add(consumeIdentifier());
            }
        }

        consume(TokenType.FROM);
        String tableName = consumeIdentifier();

        Expression where = null;
        if (peek().is(TokenType.WHERE)) {
            consume();
            where = parseExpression();
        }

        return new SelectStatement(columns, tableName, where);
    }

    // UPDATE name SET col=val [, col=val] [WHERE expr]
    private Statement parseUpdate() {
        consume(TokenType.UPDATE);
        String tableName = consumeIdentifier();
        consume(TokenType.SET);

        Map<String, Object> assignments = new LinkedHashMap<>();
        parseAssignment(assignments);
        while (peek().is(TokenType.COMMA)) {
            consume();
            parseAssignment(assignments);
        }

        Expression where = null;
        if (peek().is(TokenType.WHERE)) {
            consume();
            where = parseExpression();
        }

        return new UpdateStatement(tableName, assignments, where);
    }

    private void parseAssignment(Map<String, Object> assignments) {
        String col = consumeIdentifier();
        consume(TokenType.EQ);
        Object val = parseLiteralValue();
        assignments.put(col.toLowerCase(), val);
    }

    // DELETE FROM name [WHERE expr]
    private Statement parseDelete() {
        consume(TokenType.DELETE);
        consume(TokenType.FROM);
        String tableName = consumeIdentifier();

        Expression where = null;
        if (peek().is(TokenType.WHERE)) {
            consume();
            where = parseExpression();
        }

        return new DeleteStatement(tableName, where);
    }

    // DROP TABLE name
    // DROP DATABASE name
    private Statement parseDrop() {
        consume(TokenType.DROP);
        if (peek().is(TokenType.DATABASE)) {
            consume();
            String dbName = consumeIdentifier();
            return new DropDatabaseStatement(dbName);
        }
        consume(TokenType.TABLE);
        String tableName = consumeIdentifier();
        return new DropTableStatement(tableName);
    }

    // USE database_name
    private Statement parseUse() {
        consume(TokenType.USE);
        String dbName = consumeIdentifier();
        return new UseDatabaseStatement(dbName);
    }

    // SHOW DATABASES | SHOW TABLES
    private Statement parseShow() {
        consume(TokenType.SHOW);
        Token next = peek();
        if (next.is(TokenType.DATABASES)) {
            consume();
            return new ShowDatabasesStatement();
        }
        if (next.is(TokenType.TABLES)) {
            consume();
            return new ShowTablesStatement();
        }
        throw new RuntimeException("Expected DATABASES or TABLES after SHOW, got: " + next);
    }

    // -------------------------------------------------------------------------
    // Expression parsers (WHERE clause)
    // -------------------------------------------------------------------------

    // expr → andExpr (OR andExpr)*
    private Expression parseExpression() {
        Expression left = parseAndExpr();
        while (peek().is(TokenType.OR)) {
            consume();
            Expression right = parseAndExpr();
            left = new BinaryExpr(left, BinaryExpr.Op.OR, right);
        }
        return left;
    }

    // andExpr → comparison (AND comparison)*
    private Expression parseAndExpr() {
        Expression left = parseComparison();
        while (peek().is(TokenType.AND)) {
            consume();
            Expression right = parseComparison();
            left = new BinaryExpr(left, BinaryExpr.Op.AND, right);
        }
        return left;
    }

    // comparison → IDENTIFIER op literal | IDENTIFIER IS [NOT] NULL
    private Expression parseComparison() {
        // Handle parenthesised sub-expressions
        if (peek().is(TokenType.LPAREN)) {
            consume();
            Expression inner = parseExpression();
            consume(TokenType.RPAREN);
            return inner;
        }
        Expression left = new ColumnRef(consumeIdentifier());

        if (peek().is(TokenType.IS)) {
            consume();
            BinaryExpr.Op op = BinaryExpr.Op.IS;
            if (peek().is(TokenType.NOT)) {
                consume();
                op = BinaryExpr.Op.IS_NOT;
            }
            consume(TokenType.NULL);
            return new BinaryExpr(left, op, new Literal(null));
        }

        BinaryExpr.Op op = parseOperator();
        Expression right = new Literal(parseLiteralValue());
        return new BinaryExpr(left, op, right);
    }

    private BinaryExpr.Op parseOperator() {
        Token t = consume();
        return switch (t.getType()) {
            case EQ  -> BinaryExpr.Op.EQ;
            case NEQ -> BinaryExpr.Op.NEQ;
            case LT  -> BinaryExpr.Op.LT;
            case LTE -> BinaryExpr.Op.LTE;
            case GT  -> BinaryExpr.Op.GT;
            case GTE -> BinaryExpr.Op.GTE;
            default  -> throw new RuntimeException("Expected comparison operator, got: " + t);
        };
    }

    // -------------------------------------------------------------------------
    // Value helpers
    // -------------------------------------------------------------------------

    private Object parseLiteralValue() {
        Token t = consume();
        return switch (t.getType()) {
            case LITERAL_INT    -> Integer.parseInt(t.getValue());
            case LITERAL_FLOAT  -> Double.parseDouble(t.getValue());
            case LITERAL_STRING -> t.getValue();
            case LITERAL_BOOL   -> Boolean.parseBoolean(t.getValue());
            case NULL           -> null;
            default             -> throw new RuntimeException(
                    "Expected a literal value (integer, float, string, boolean, null), got: " + t);
        };
    }

    // -------------------------------------------------------------------------
    // Token consumption helpers
    // -------------------------------------------------------------------------

    private String consumeIdentifier() {
        Token t = consume();
        if (t.getType() != TokenType.IDENTIFIER) {
            throw new RuntimeException("Expected identifier, got: " + t);
        }
        return t.getValue();
    }

    /**
     * Consume a data-type token. Identifiers like "INTEGER", "VARCHAR(50)"
     * are emitted by the lexer as IDENTIFIER tokens (uppercased in the lexer).
     */
    private String consumeTypeToken() {
        Token t = consume();
        if (t.getType() == TokenType.IDENTIFIER) {
            return t.getValue().toUpperCase();
        }
        throw new RuntimeException("Expected data type, got: " + t);
    }

    private void consume(TokenType expected) {
        Token t = consume();
        if (t.getType() != expected) {
            throw new RuntimeException(
                    "Expected " + expected + " but got " + t);
        }
    }

    private Token consume() {
        Token t = tokens.get(pos);
        if (!t.is(TokenType.EOF)) pos++;
        return t;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private void expect(TokenType type) {
        Token t = peek();
        if (!t.is(type)) {
            throw new RuntimeException("Expected " + type + " at end, got: " + t);
        }
    }
}
