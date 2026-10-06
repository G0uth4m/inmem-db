package com.inmemdb.server.sql.lexer;

/**
 * A single token produced by the Lexer: a type and an optional raw value.
 */
public class Token {

    private final TokenType type;
    private final String value; // raw text from source

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public Token(TokenType type) {
        this(type, null);
    }

    public TokenType getType() {
        return type;
    }

    /** Raw text value; may be null for punctuation/operator tokens. */
    public String getValue() {
        return value;
    }

    public boolean is(TokenType t) {
        return this.type == t;
    }

    @Override
    public String toString() {
        return value != null ? "[" + type + ":" + value + "]" : "[" + type + "]";
    }
}
