package com.inmemdb.server.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level server catalog: a registry of named databases.
 * Each database is an independent {@link Catalog} (its own set of tables).
 *
 * Introduced in Phase 9 (multi-database support).
 */
public class ServerCatalog {

    // LinkedHashMap preserves creation order for SHOW DATABASES
    private final Map<String, Catalog> databases = new LinkedHashMap<>();

    public ServerCatalog() {
        createDatabase("default");
    }

    public void createDatabase(String name) {
        String key = name.toLowerCase();
        if (databases.containsKey(key)) {
            throw new RuntimeException("Database already exists: " + name);
        }
        databases.put(key, new Catalog());
    }

    public void dropDatabase(String name) {
        String key = name.toLowerCase();
        if (!databases.containsKey(key)) {
            throw new RuntimeException("Database does not exist: " + name);
        }
        databases.remove(key);
    }

    public Catalog getDatabase(String name) {
        Catalog cat = databases.get(name.toLowerCase());
        if (cat == null) {
            throw new RuntimeException("Database does not exist: " + name);
        }
        return cat;
    }

    public boolean databaseExists(String name) {
        return databases.containsKey(name.toLowerCase());
    }

    public List<String> listDatabases() {
        return new ArrayList<>(databases.keySet());
    }
}
