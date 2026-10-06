package com.inmemdb.server;

public class DebugTest3 {
    public static void main(String[] args) {
        Database db = new Database(new com.inmemdb.server.catalog.ServerCatalog());
        db.execute("CREATE TABLE t (id INTEGER, val DOUBLE)");
        db.execute("INSERT INTO t VALUES (1, 10.0)");
        db.execute("INSERT INTO t VALUES (2, NULL)");
        System.out.println("RES1: " + db.execute("SELECT id FROM t WHERE val IS NULL"));
    }
}
