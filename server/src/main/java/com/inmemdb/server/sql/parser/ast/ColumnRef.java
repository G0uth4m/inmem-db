package com.inmemdb.server.sql.parser.ast;

/** Refers to a column by name, e.g. {@code id} in {@code WHERE id = 1}. */
public class ColumnRef implements Expression {

    private final String name;

    public ColumnRef(String name) {
        this.name = name.toLowerCase();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "col:" + name;
    }
}
