package com.inmemdb.server;

import com.inmemdb.server.catalog.Catalog;
import com.inmemdb.server.catalog.ServerCatalog;
import com.inmemdb.server.planner.Planner;
import com.inmemdb.server.sql.executor.Executor;
import com.inmemdb.server.sql.executor.ResultSet;
import com.inmemdb.server.sql.lexer.Lexer;
import com.inmemdb.server.sql.lexer.Token;
import com.inmemdb.server.sql.parser.Parser;
import com.inmemdb.server.sql.parser.ast.*;

import java.util.List;

/**
 * Per-session database context.
 *
 * Holds a reference to the shared {@link ServerCatalog} (all databases on the server)
 * and tracks the {@code currentDatabase} selected by this session via {@code USE <name>}.
 *
 * Each connected client gets its own {@code Database} instance; the {@link ServerCatalog}
 * is shared across all sessions (created once in {@link Server}).
 *
 * Phase 9 (multi-database support):
 *   - CREATE DATABASE / DROP DATABASE operate on {@link ServerCatalog}.
 *   - USE sets this session's {@code currentDatabase}.
 *   - SHOW DATABASES / SHOW TABLES are catalog-level queries.
 *   - All table-level statements (CREATE TABLE, SELECT, INSERT, UPDATE, DELETE, DROP TABLE)
 *     require a database to be selected first.
 *
 * Backward-compatibility (unit tests):
 *   The no-arg constructor auto-creates and selects a "default" database so existing
 *   Phase 1 tests continue to work without calling USE.
 */
public class Database {

    private static final String DEFAULT_DB = "default";

    private final ServerCatalog serverCatalog;
    private String currentDatabase; // null = no database selected

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * No-arg constructor for unit tests: creates an internal ServerCatalog.
     */
    public Database() {
        this.serverCatalog = new ServerCatalog();
        this.currentDatabase = DEFAULT_DB;
    }

    /**
     * Production constructor: shares the server-wide {@link ServerCatalog}.
     * By default, the 'default' database is selected.
     */
    public Database(ServerCatalog serverCatalog) {
        this.serverCatalog   = serverCatalog;
        this.currentDatabase = DEFAULT_DB;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    public String execute(String sql) {
        try {
            List<Token> tokens = new Lexer(sql).tokenize();
            Statement   stmt   = new Parser(tokens).parse();

            // ── Phase 9: DB-level statements (no database selection required) ──
            if (stmt instanceof CreateDatabaseStatement s)  return handleCreateDatabase(s);
            if (stmt instanceof DropDatabaseStatement   s)  return handleDropDatabase(s);
            if (stmt instanceof UseDatabaseStatement    s)  return handleUse(s);
            if (stmt instanceof ShowDatabasesStatement)     return handleShowDatabases();
            if (stmt instanceof ShowTablesStatement)        return handleShowTables();

            // ── Table-level statements: require USE to have been called ──────
            if (currentDatabase == null) {
                return "ERROR: No database selected. Run: USE <database>;";
            }

            Catalog  catalog  = serverCatalog.getDatabase(currentDatabase);
            Planner  planner  = new Planner(catalog);
            Executor executor = new Executor(catalog);

            Statement plan   = planner.plan(stmt);
            Object    result = executor.execute(plan);

            if (result instanceof ResultSet rs) return rs.format();
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    // Phase 9 handlers
    // -------------------------------------------------------------------------

    private String handleCreateDatabase(CreateDatabaseStatement s) {
        serverCatalog.createDatabase(s.getDatabaseName());
        return "Database '" + s.getDatabaseName() + "' created.";
    }

    private String handleDropDatabase(DropDatabaseStatement s) {
        if (s.getDatabaseName().equals(currentDatabase)) {
            currentDatabase = null; // deselect if we just dropped the active DB
        }
        serverCatalog.dropDatabase(s.getDatabaseName());
        return "Database '" + s.getDatabaseName() + "' dropped.";
    }

    private String handleUse(UseDatabaseStatement s) {
        if (!serverCatalog.databaseExists(s.getDatabaseName())) {
            return "ERROR: Database does not exist: " + s.getDatabaseName();
        }
        currentDatabase = s.getDatabaseName();
        return "Database changed to: " + currentDatabase;
    }

    private String handleShowDatabases() {
        List<String> dbs = serverCatalog.listDatabases();
        if (dbs.isEmpty()) return "(no databases)\n";

        int maxLen = dbs.stream().mapToInt(String::length).max().orElse(8);
        maxLen = Math.max(maxLen, "database".length());

        StringBuilder sb = new StringBuilder();
        String border = "+" + "-".repeat(maxLen + 2) + "+";
        sb.append(border).append("\n");
        sb.append("| ").append(pad("database", maxLen)).append(" |").append("\n");
        sb.append(border).append("\n");
        for (String db : dbs) {
            sb.append("| ").append(pad(db, maxLen)).append(" |").append("\n");
        }
        sb.append(border).append("\n");
        sb.append("(").append(dbs.size()).append(dbs.size() == 1 ? " database)" : " databases)").append("\n");
        return sb.toString();
    }

    private String handleShowTables() {
        if (currentDatabase == null) {
            return "ERROR: No database selected. Run: USE <database>;";
        }
        Catalog catalog = serverCatalog.getDatabase(currentDatabase);
        List<String> tables = catalog.listTables();

        if (tables.isEmpty()) return "(no tables in '" + currentDatabase + "')\n";

        int maxLen = tables.stream().mapToInt(String::length).max().orElse(5);
        maxLen = Math.max(maxLen, "table".length());

        StringBuilder sb = new StringBuilder();
        String border = "+" + "-".repeat(maxLen + 2) + "+";
        sb.append(border).append("\n");
        sb.append("| ").append(pad("table", maxLen)).append(" |").append("\n");
        sb.append(border).append("\n");
        for (String t : tables) {
            sb.append("| ").append(pad(t, maxLen)).append(" |").append("\n");
        }
        sb.append(border).append("\n");
        sb.append("(").append(tables.size()).append(tables.size() == 1 ? " table)" : " tables)").append("\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String pad(String s, int width) {
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    /** Exposed for testing: returns the currently selected database name, or null. */
    public String getCurrentDatabase() {
        return currentDatabase;
    }
}
