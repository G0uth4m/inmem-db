package com.inmemdb.server.sql.lexer;

/**
 * All token types produced by the Lexer.
 */
public enum TokenType {
    // Keywords
    SELECT, FROM, WHERE, INSERT, INTO, VALUES,
    UPDATE, SET, DELETE, CREATE, TABLE, DROP,

    // Phase 9 — multi-database keywords
    DATABASE, USE, SHOW, DATABASES, TABLES,

    // Phase 2 — primary key & null handling
    PRIMARY, KEY, NULL, IS,

    // Identifiers and literals
    IDENTIFIER,
    LITERAL_INT,
    LITERAL_FLOAT,
    LITERAL_STRING,
    LITERAL_BOOL,

    // Operators
    EQ,   // =
    NEQ,  // != or <>
    LT,   // <
    LTE,  // <=
    GT,   // >
    GTE,  // >=
    AND,
    OR,
    NOT,

    // Punctuation
    COMMA,      // ,
    LPAREN,     // (
    RPAREN,     // )
    SEMICOLON,  // ;
    STAR,       // *

    // End of input
    EOF
}
