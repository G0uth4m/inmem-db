package com.inmemdb.server.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level catalog: holds all tables in the database.
 * In Phase 1 this is a flat map (no multi-database support yet – that is Phase 9).
 */
public class Catalog {

    private final Map<String, Table> tables = new HashMap<>();

    public void createTable(Table table) {
        String key = table.getName().toLowerCase();
        if (tables.containsKey(key)) {
            throw new RuntimeException("Table already exists: " + table.getName());
        }
        tables.put(key, table);
    }

    public void dropTable(String tableName) {
        String key = tableName.toLowerCase();
        if (!tables.containsKey(key)) {
            throw new RuntimeException("Table does not exist: " + tableName);
        }
        tables.remove(key);
    }

    public Table getTable(String tableName) {
        Table t = tables.get(tableName.toLowerCase());
        if (t == null) {
            throw new RuntimeException("Table does not exist: " + tableName);
        }
        return t;
    }

    public boolean tableExists(String tableName) {
        return tables.containsKey(tableName.toLowerCase());
    }

    public List<String> listTables() {
        return new ArrayList<>(tables.keySet());
    }
}
