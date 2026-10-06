package com.inmemdb.server.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A single database row: a mutable map of column name → value.
 * Values are plain Java objects (Integer, String, Boolean, Double, or null).
 */
public class Row {

    private final LinkedHashMap<String, Object> data;

    public Row() {
        this.data = new LinkedHashMap<>();
    }

    public Row(LinkedHashMap<String, Object> data) {
        this.data = new LinkedHashMap<>(data);
    }

    public void set(String column, Object value) {
        data.put(column.toLowerCase(), value);
    }

    public Object get(String column) {
        return data.get(column.toLowerCase());
    }

    public boolean has(String column) {
        return data.containsKey(column.toLowerCase());
    }

    public Set<String> columns() {
        return data.keySet();
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
