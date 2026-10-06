package com.inmemdb.server.sql.parser.ast;

/** A literal value (Integer, Double, String, Boolean, or null). */
public class Literal implements Expression {

    private final Object value;

    public Literal(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "lit:" + value;
    }
}
