package com.inmemdb.server.catalog;

/**
 * Represents a single column definition in a table schema.
 */
public class Column {

    private final String name;
    private final DataType type;
    private final boolean isPrimaryKey;
    private final boolean isNullable;

    public Column(String name, DataType type) {
        this(name, type, false, true);
    }

    public Column(String name, DataType type, boolean isPrimaryKey, boolean isNullable) {
        this.name = name.toLowerCase();
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isNullable = isNullable;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public boolean isNullable() {
        return isNullable;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name).append(" ").append(type);
        if (isPrimaryKey) sb.append(" PRIMARY KEY");
        else if (!isNullable) sb.append(" NOT NULL");
        return sb.toString();
    }
}
