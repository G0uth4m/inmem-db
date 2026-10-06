package com.inmemdb.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for Phase 1 — run entirely in-process via the Database class.
 * No server socket needed for unit tests.
 */
class DatabaseTest {

    private Database db;

    @BeforeEach
    void setUp() {
        db = new Database(); // fresh in-memory state per test
    }

    // -------------------------------------------------------------------------
    // DDL
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CREATE TABLE succeeds and is idempotent-error on duplicate")
    void testCreateTable() {
        assertEquals("OK", db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))"));
        // Duplicate should error
        assertTrue(db.execute("CREATE TABLE users (id INTEGER)").startsWith("ERROR:"));
    }

    @Test
    @DisplayName("DROP TABLE removes the table")
    void testDropTable() {
        db.execute("CREATE TABLE t1 (id INTEGER)");
        assertEquals("OK", db.execute("DROP TABLE t1"));
        // Second drop should error
        assertTrue(db.execute("DROP TABLE t1").startsWith("ERROR:"));
    }

    @Test
    @DisplayName("DROP TABLE on non-existent table returns ERROR")
    void testDropNonExistent() {
        assertTrue(db.execute("DROP TABLE ghost").startsWith("ERROR:"));
    }

    // -------------------------------------------------------------------------
    // INSERT
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("INSERT inserts a row successfully")
    void testInsert() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        assertEquals("OK", db.execute("INSERT INTO users VALUES (1, 'Alice')"));
    }

    @Test
    @DisplayName("INSERT with wrong column count returns ERROR")
    void testInsertWrongColCount() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        assertTrue(db.execute("INSERT INTO users VALUES (1)").startsWith("ERROR:"));
    }

    // -------------------------------------------------------------------------
    // SELECT
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("SELECT * returns all rows")
    void testSelectStar() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");
        db.execute("INSERT INTO users VALUES (2, 'Bob')");

        String result = db.execute("SELECT * FROM users");
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Bob"));
        assertTrue(result.contains("2 rows"));
    }

    @Test
    @DisplayName("SELECT with WHERE = filter")
    void testSelectWhereEq() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");
        db.execute("INSERT INTO users VALUES (2, 'Bob')");

        String result = db.execute("SELECT * FROM users WHERE id = 1");
        assertTrue(result.contains("Alice"));
        assertFalse(result.contains("Bob"));
        assertTrue(result.contains("1 row)"));
    }

    @Test
    @DisplayName("SELECT with WHERE != filter")
    void testSelectWhereNeq() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");
        db.execute("INSERT INTO users VALUES (2, 'Bob')");

        String result = db.execute("SELECT * FROM users WHERE id != 1");
        assertFalse(result.contains("Alice"));
        assertTrue(result.contains("Bob"));
    }

    @Test
    @DisplayName("SELECT specific columns (projection)")
    void testSelectProjection() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50), age INTEGER)");
        db.execute("INSERT INTO users VALUES (1, 'Alice', 30)");

        String result = db.execute("SELECT name FROM users");
        assertTrue(result.contains("Alice"));
        // 'id' and 'age' should not appear in the header row
        assertFalse(result.lines().findFirst().orElse("").contains("id"));
    }

    @Test
    @DisplayName("SELECT on non-existent table returns ERROR")
    void testSelectNoTable() {
        assertTrue(db.execute("SELECT * FROM ghost").startsWith("ERROR:"));
    }

    @Test
    @DisplayName("SELECT with AND condition")
    void testSelectWhereAnd() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50), age INTEGER)");
        db.execute("INSERT INTO users VALUES (1, 'Alice', 25)");
        db.execute("INSERT INTO users VALUES (2, 'Bob',   30)");
        db.execute("INSERT INTO users VALUES (3, 'Charlie', 25)");

        String result = db.execute("SELECT * FROM users WHERE age = 25 AND id = 1");
        assertTrue(result.contains("Alice"));
        assertFalse(result.contains("Bob"));
        assertFalse(result.contains("Charlie"));
    }

    @Test
    @DisplayName("SELECT with OR condition")
    void testSelectWhereOr() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");
        db.execute("INSERT INTO users VALUES (2, 'Bob')");
        db.execute("INSERT INTO users VALUES (3, 'Charlie')");

        String result = db.execute("SELECT * FROM users WHERE id = 1 OR id = 3");
        assertTrue(result.contains("Alice"));
        assertFalse(result.contains("Bob"));
        assertTrue(result.contains("Charlie"));
    }

    @Test
    @DisplayName("SELECT with comparison operators (<, >, <=, >=)")
    void testSelectWhereComparisons() {
        db.execute("CREATE TABLE nums (n INTEGER)");
        db.execute("INSERT INTO nums VALUES (1)");
        db.execute("INSERT INTO nums VALUES (5)");
        db.execute("INSERT INTO nums VALUES (10)");

        assertTrue(db.execute("SELECT * FROM nums WHERE n > 4").contains("5"));
        assertTrue(db.execute("SELECT * FROM nums WHERE n < 5").contains("1"));
        assertTrue(db.execute("SELECT * FROM nums WHERE n >= 5").contains("10"));
        assertTrue(db.execute("SELECT * FROM nums WHERE n <= 5").contains("1"));
    }

    @Test
    @DisplayName("SELECT returns empty result when no rows match")
    void testSelectNoMatch() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");

        String result = db.execute("SELECT * FROM users WHERE id = 99");
        assertTrue(result.contains("0 rows"));
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("UPDATE modifies matching rows")
    void testUpdate() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");
        db.execute("INSERT INTO users VALUES (2, 'Bob')");

        String updateResult = db.execute("UPDATE users SET name = 'Alicia' WHERE id = 1");
        assertEquals("1 row(s) updated", updateResult);

        String select = db.execute("SELECT * FROM users WHERE id = 1");
        assertTrue(select.contains("Alicia"));
        assertFalse(select.contains("Alice"));
    }

    @Test
    @DisplayName("UPDATE without WHERE updates all rows")
    void testUpdateNoWhere() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");
        db.execute("INSERT INTO users VALUES (2, 'Bob')");

        String updateResult = db.execute("UPDATE users SET name = 'Everyone'");
        assertEquals("2 row(s) updated", updateResult);
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE removes matching rows")
    void testDelete() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");
        db.execute("INSERT INTO users VALUES (2, 'Bob')");

        String deleteResult = db.execute("DELETE FROM users WHERE id = 1");
        assertEquals("1 row(s) deleted", deleteResult);

        String select = db.execute("SELECT * FROM users");
        assertFalse(select.contains("Alice"));
        assertTrue(select.contains("Bob"));
    }

    @Test
    @DisplayName("DELETE without WHERE clears all rows")
    void testDeleteNoWhere() {
        db.execute("CREATE TABLE users (id INTEGER, name VARCHAR(50))");
        db.execute("INSERT INTO users VALUES (1, 'Alice')");
        db.execute("INSERT INTO users VALUES (2, 'Bob')");

        String deleteResult = db.execute("DELETE FROM users");
        assertEquals("2 row(s) deleted", deleteResult);
        assertTrue(db.execute("SELECT * FROM users").contains("0 rows"));
    }

    // -------------------------------------------------------------------------
    // Data types
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DOUBLE and BOOLEAN columns work correctly")
    void testDataTypes() {
        db.execute("CREATE TABLE products (id INTEGER, price DOUBLE, available BOOLEAN)");
        db.execute("INSERT INTO products VALUES (1, 9.99, true)");
        db.execute("INSERT INTO products VALUES (2, 14.99, false)");

        String result = db.execute("SELECT * FROM products WHERE available = true");
        assertTrue(result.contains("9.99"));
        assertFalse(result.contains("14.99"));
    }

    // -------------------------------------------------------------------------
    // Lexer edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Statements with trailing semicolon are accepted")
    void testTrailingSemicolon() {
        assertEquals("OK", db.execute("CREATE TABLE t (id INTEGER);"));
    }

    @Test
    @DisplayName("Keywords are case-insensitive")
    void testCaseInsensitiveKeywords() {
        assertEquals("OK", db.execute("create table emp (id integer, name varchar(30))"));
        assertEquals("OK", db.execute("INSERT into emp VALUES (1, 'Dave')"));
        assertTrue(db.execute("select * from emp").contains("Dave"));
    }

    // -------------------------------------------------------------------------
    // Phase 9: Multi-database support
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CREATE DATABASE and USE switch active database")
    void testCreateAndUseDatabase() {
        assertEquals("Database 'testdb' created.", db.execute("CREATE DATABASE testdb"));
        assertEquals("Database changed to: testdb", db.execute("USE testdb"));

        // Now creating a table goes to 'testdb'
        assertEquals("OK", db.execute("CREATE TABLE dt (id INTEGER)"));
        String tables = db.execute("SHOW TABLES");
        assertTrue(tables.contains("dt"));
    }

    @Test
    @DisplayName("Table operations fail if no database selected")
    void testTableOpsFailWithoutDb() {
        Database freshDb = new Database(new com.inmemdb.server.catalog.ServerCatalog());
        freshDb.execute("DROP DATABASE default"); // deselects active
        assertTrue(freshDb.execute("CREATE TABLE t (id INTEGER)").startsWith("ERROR: No database selected"));
        assertTrue(freshDb.execute("SELECT * FROM t").startsWith("ERROR: No database selected"));
        assertTrue(freshDb.execute("SHOW TABLES").startsWith("ERROR: No database selected"));
    }

    @Test
    @DisplayName("SHOW DATABASES lists available databases")
    void testShowDatabases() {
        db.execute("CREATE DATABASE db1");
        db.execute("CREATE DATABASE db2");
        String out = db.execute("SHOW DATABASES");
        assertTrue(out.contains("db1"));
        assertTrue(out.contains("db2"));
        // "default" is also there from the test Database constructor
        assertTrue(out.contains("default"));
    }

    @Test
    @DisplayName("DROP DATABASE removes database and deselects if active")
    void testDropDatabase() {
        db.execute("CREATE DATABASE testdb");
        db.execute("USE testdb");
        assertEquals("testdb", db.getCurrentDatabase());

        assertEquals("Database 'testdb' dropped.", db.execute("DROP DATABASE testdb"));
        assertNull(db.getCurrentDatabase());
        assertTrue(db.execute("SHOW DATABASES").contains("default"));
        assertFalse(db.execute("SHOW DATABASES").contains("testdb"));
    }

    // -------------------------------------------------------------------------
    // Phase 2 & 4: Primary Key, Nulls, Indexing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PRIMARY KEY constraints and Nulls")
    void testPrimaryKeyAndNulls() {
        assertEquals("OK", db.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50) NOT NULL, age INTEGER)"));
        
        // Null checks
        String res = db.execute("INSERT INTO users VALUES (NULL, 'Alice', 30)");
        System.out.println("DEBUG RES: " + res);
        assertTrue(res.startsWith("ERROR: Column id cannot be null"));
        assertTrue(db.execute("INSERT INTO users VALUES (1, NULL, 30)").startsWith("ERROR: Column name cannot be null"));
        
        // Normal insert
        assertEquals("OK", db.execute("INSERT INTO users VALUES (1, 'Alice', 30)"));
        
        // PK Duplicate insert
        assertTrue(db.execute("INSERT INTO users VALUES (1, 'Bob', 25)").startsWith("ERROR: Duplicate entry for primary key column id: 1"));

        // PK Duplicate update
        assertEquals("OK", db.execute("INSERT INTO users VALUES (2, 'Bob', 25)"));
        assertTrue(db.execute("UPDATE users SET id = 1 WHERE id = 2").startsWith("ERROR: Duplicate entry for primary key: 1"));
    }

    @Test
    @DisplayName("IS NULL and IS NOT NULL")
    void testIsNull() {
        db.execute("CREATE TABLE t (id INTEGER, val DOUBLE)");
        db.execute("INSERT INTO t VALUES (1, 10.0)");
        db.execute("INSERT INTO t VALUES (2, NULL)");
        
        String res1 = db.execute("SELECT id FROM t WHERE val IS NULL");
        assertTrue(res1.contains(" 2 "));
        assertFalse(res1.contains(" 1 "));

        String res2 = db.execute("SELECT id FROM t WHERE val IS NOT NULL");
        assertTrue(res2.contains(" 1 "));
        assertFalse(res2.contains(" 2 "));
    }

    @Test
    @DisplayName("Type Coercion")
    void testTypeCoercion() {
        db.execute("CREATE TABLE metrics (id INTEGER PRIMARY KEY, score DOUBLE)");
        // insert int into double
        assertEquals("OK", db.execute("INSERT INTO metrics VALUES (1.0, 5)"));
        String res = db.execute("SELECT * FROM metrics");
        assertTrue(res.contains("5.0")); // coerced to double
    }
}
