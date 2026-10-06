package com.inmemdb.server.sql.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Hand-written lexer for Phase 1 SQL.
 * Converts a raw SQL string into a flat list of {@link Token}s.
 *
 * Supported tokens: keywords, identifiers, integer/float/string literals,
 * comparison operators (=, !=, <>, <, <=, >, >=), AND, OR, NOT,
 * and punctuation (, ( ) ; *).
 */
public class Lexer {

    private static final Set<String> KEYWORDS = Set.of(
            "SELECT", "FROM", "WHERE",
            "INSERT", "INTO", "VALUES",
            "UPDATE", "SET",
            "DELETE",
            "CREATE", "TABLE", "DROP",
            "AND", "OR", "NOT",
            "TRUE", "FALSE"
    );

    private final String src;
    private int pos = 0;

    public Lexer(String src) {
        this.src = src.trim();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < src.length()) {
            skipWhitespace();
            if (pos >= src.length()) break;

            char c = src.charAt(pos);

            if (Character.isLetter(c) || c == '_') {
                tokens.add(readWord());
            } else if (Character.isDigit(c) || (c == '-' && isNextDigit())) {
                tokens.add(readNumber());
            } else if (c == '\'') {
                tokens.add(readString());
            } else if (c == '!' || c == '<' || c == '>' || c == '=') {
                tokens.add(readOperator());
            } else {
                tokens.add(readPunctuation());
            }
        }
        tokens.add(new Token(TokenType.EOF));
        return tokens;
    }

    // -------------------------------------------------------------------------
    // Readers
    // -------------------------------------------------------------------------

    private Token readWord() {
        int start = pos;
        while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
            pos++;
        }
        // Handle VARCHAR(n) – consume the (n) part as part of the type token
        String word = src.substring(start, pos).toUpperCase();

        if (word.equals("VARCHAR") && pos < src.length() && src.charAt(pos) == '(') {
            // consume "(50)" so the parser receives "VARCHAR(50)" as one token
            int parenStart = pos;
            pos++; // skip '('
            while (pos < src.length() && src.charAt(pos) != ')') pos++;
            if (pos < src.length()) pos++; // skip ')'
            word = word + src.substring(parenStart, pos);
        }

        return switch (word) {
            case "SELECT"   -> new Token(TokenType.SELECT,    word);
            case "FROM"     -> new Token(TokenType.FROM,      word);
            case "WHERE"    -> new Token(TokenType.WHERE,     word);
            case "INSERT"   -> new Token(TokenType.INSERT,    word);
            case "INTO"     -> new Token(TokenType.INTO,      word);
            case "VALUES"   -> new Token(TokenType.VALUES,    word);
            case "UPDATE"   -> new Token(TokenType.UPDATE,    word);
            case "SET"      -> new Token(TokenType.SET,       word);
            case "DELETE"   -> new Token(TokenType.DELETE,    word);
            case "CREATE"   -> new Token(TokenType.CREATE,    word);
            case "TABLE"    -> new Token(TokenType.TABLE,     word);
            case "DROP"     -> new Token(TokenType.DROP,      word);
            case "AND"      -> new Token(TokenType.AND,       word);
            case "OR"       -> new Token(TokenType.OR,        word);
            case "NOT"      -> new Token(TokenType.NOT,       word);
            case "TRUE"     -> new Token(TokenType.LITERAL_BOOL, "true");
            case "FALSE"    -> new Token(TokenType.LITERAL_BOOL, "false");
            // Phase 9 — multi-database keywords
            case "DATABASE"  -> new Token(TokenType.DATABASE,  word);
            case "USE"       -> new Token(TokenType.USE,       word);
            case "SHOW"      -> new Token(TokenType.SHOW,      word);
            case "DATABASES" -> new Token(TokenType.DATABASES, word);
            case "TABLES"    -> new Token(TokenType.TABLES,    word);
            // Phase 2 — primary key & null handling
            case "PRIMARY"   -> new Token(TokenType.PRIMARY,   word);
            case "KEY"       -> new Token(TokenType.KEY,       word);
            case "NULL"      -> new Token(TokenType.NULL,      word);
            case "IS"        -> new Token(TokenType.IS,        word);
            default          -> new Token(TokenType.IDENTIFIER, word.toLowerCase());
        };
    }

    private Token readNumber() {
        int start = pos;
        if (src.charAt(pos) == '-') pos++; // optional leading minus
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        boolean isFloat = false;
        if (pos < src.length() && src.charAt(pos) == '.' && pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1))) {
            isFloat = true;
            pos++; // skip '.'
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        String raw = src.substring(start, pos);
        return isFloat
                ? new Token(TokenType.LITERAL_FLOAT, raw)
                : new Token(TokenType.LITERAL_INT,   raw);
    }

    private Token readString() {
        pos++; // skip opening '
        int start = pos;
        while (pos < src.length() && src.charAt(pos) != '\'') {
            // simple: no escape sequences in Phase 1
            pos++;
        }
        String value = src.substring(start, pos);
        if (pos < src.length()) pos++; // skip closing '
        return new Token(TokenType.LITERAL_STRING, value);
    }

    private Token readOperator() {
        char c = src.charAt(pos);
        if (c == '=') {
            pos++;
            return new Token(TokenType.EQ, "=");
        }
        if (c == '!') {
            if (pos + 1 < src.length() && src.charAt(pos + 1) == '=') {
                pos += 2;
                return new Token(TokenType.NEQ, "!=");
            }
            throw new RuntimeException("Unexpected character '!' at position " + pos);
        }
        if (c == '<') {
            if (pos + 1 < src.length() && src.charAt(pos + 1) == '=') {
                pos += 2; return new Token(TokenType.LTE, "<=");
            }
            if (pos + 1 < src.length() && src.charAt(pos + 1) == '>') {
                pos += 2; return new Token(TokenType.NEQ, "<>");
            }
            pos++; return new Token(TokenType.LT, "<");
        }
        if (c == '>') {
            if (pos + 1 < src.length() && src.charAt(pos + 1) == '=') {
                pos += 2; return new Token(TokenType.GTE, ">=");
            }
            pos++; return new Token(TokenType.GT, ">");
        }
        throw new RuntimeException("Unknown operator at position " + pos);
    }

    private Token readPunctuation() {
        char c = src.charAt(pos++);
        return switch (c) {
            case ',' -> new Token(TokenType.COMMA,     ",");
            case '(' -> new Token(TokenType.LPAREN,    "(");
            case ')' -> new Token(TokenType.RPAREN,    ")");
            case ';' -> new Token(TokenType.SEMICOLON, ";");
            case '*' -> new Token(TokenType.STAR,      "*");
            default  -> throw new RuntimeException(
                    "Unexpected character '" + c + "' at position " + (pos - 1));
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private boolean isNextDigit() {
        return pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1));
    }
}
