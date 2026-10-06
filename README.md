# InMemDB

A simple, H2-inspired **in-memory relational database** built from scratch in Java.
It runs as a **TCP server** and ships with an interactive **CLI client** — no external dependencies.

---

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Build](#build)
- [Running the Server](#running-the-server)
- [Running the CLI Client](#running-the-cli-client)
- [SQL Reference](#sql-reference)
- [Running Tests](#running-tests)
- [Wire Protocol](#wire-protocol)
- [Architecture](#architecture)
- [Roadmap](#roadmap)

---

## Features

- Hand-written **SQL Lexer → Parser → Planner → Executor** pipeline
- Supported statements: `CREATE TABLE`, `INSERT`, `SELECT`, `UPDATE`, `DELETE`, `DROP TABLE`
- `WHERE` clause with `=`, `!=`, `<`, `<=`, `>`, `>=`, `AND`, `OR`
- Column projection (`SELECT col1, col2 FROM …`)
- Data types: `INTEGER`, `VARCHAR(n)`, `DOUBLE`, `BOOLEAN`
- Pretty-printed, auto-sized table output
- TCP server with a text wire protocol (debuggable with `telnet`)
- Interactive CLI REPL with `InMemDB>` prompt

---

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| JDK  | 17             |
| Maven | 3.8+          |

Verify your setup:

```bash
java -version
mvn -version
```

---

## Project Structure

```
inmem-db/
├── pom.xml                          # Parent POM (multi-module)
├── server/                          # Database server module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/inmemdb/server/
│       │   ├── Server.java          # Entry point — ServerSocket loop
│       │   ├── ClientHandler.java   # Per-connection handler
│       │   ├── Database.java        # Pipeline orchestrator
│       │   ├── catalog/             # Catalog, Table, Column, DataType
│       │   ├── storage/             # Row
│       │   ├── sql/
│       │   │   ├── lexer/           # Lexer, Token, TokenType
│       │   │   ├── parser/          # Parser + AST nodes
│       │   │   └── executor/        # Executor, ResultSet
│       │   └── planner/             # Planner (schema validation)
│       └── test/java/com/inmemdb/server/
│           └── DatabaseTest.java    # 21 JUnit 5 tests
└── cli/                             # CLI client module
    ├── pom.xml
    └── src/main/java/com/inmemdb/cli/
        └── CliClient.java           # Interactive REPL over TCP
```

---

## Build

Clone the repo and build both modules with Maven:

```bash
cd inmem-db
mvn clean package
```

This produces two JARs:

| JAR | Location |
|-----|----------|
| `inmemdb-server.jar` | `server/target/inmemdb-server.jar` |
| `inmemdb-cli.jar`    | `cli/target/inmemdb-cli.jar`       |

---

## Running the Server

```bash
# Default port 5432
java -jar server/target/inmemdb-server.jar

# Custom port
java -jar server/target/inmemdb-server.jar --port 9090
```

Expected output:

```
╔══════════════════════════════════╗
║   InMemDB Server  (Phase 1)      ║
╚══════════════════════════════════╝
Listening on port 5432 ...
Connect with:  java -jar inmemdb-cli.jar --port 5432
```

> **Note:** Keep the server running in a separate terminal before starting the CLI.

---

## Running the CLI Client

```bash
# Connect to localhost:5432 (defaults)
java -jar cli/target/inmemdb-cli.jar

# Custom host / port
java -jar cli/target/inmemdb-cli.jar --host localhost --port 9090
```

Expected output:

```
╔══════════════════════════════════╗
║   InMemDB CLI  (Phase 1)         ║
╚══════════════════════════════════╝
Connecting to localhost:5432 ...
Connected! Type SQL statements and press Enter.
Type 'exit' or 'quit' to disconnect.

InMemDB>
```

---

## SQL Reference

### Data Types

| Type | Example | Notes |
|------|---------|-------|
| `INTEGER` | `42` | 32-bit signed integer |
| `VARCHAR(n)` | `'hello'` | Variable-length string; `n` is informational |
| `DOUBLE` | `3.14` | 64-bit floating point |
| `BOOLEAN` | `true` / `false` | Case-insensitive |

---

### CREATE TABLE

```sql
CREATE TABLE users (id INTEGER, name VARCHAR(50), age INTEGER);
```

---

### INSERT INTO

Values must match the table's column order exactly.

```sql
INSERT INTO users VALUES (1, 'Alice', 30);
INSERT INTO users VALUES (2, 'Bob', 25);
```

---

### SELECT

```sql
-- All columns
SELECT * FROM users;

-- Specific columns (projection)
SELECT id, name FROM users;

-- With a WHERE clause
SELECT * FROM users WHERE age = 30;
SELECT * FROM users WHERE age > 25 AND name != 'Bob';
SELECT * FROM users WHERE id = 1 OR id = 3;
```

**Supported WHERE operators:** `=`  `!=`  `<`  `<=`  `>`  `>=`  `AND`  `OR`

**Example output:**

```
 id | name    | age
----+---------+-----
 1  | Alice   | 30
 3  | Charlie | 30
(2 rows)
```

---

### UPDATE

```sql
-- Update matching rows
UPDATE users SET name = 'Alicia' WHERE id = 1;

-- Update all rows (no WHERE)
UPDATE users SET age = 0;
```

Response: `n row(s) updated`

---

### DELETE

```sql
-- Delete matching rows
DELETE FROM users WHERE age < 18;

-- Delete all rows (no WHERE)
DELETE FROM users;
```

Response: `n row(s) deleted`

---

### DROP TABLE

```sql
DROP TABLE users;
```

---

### Session Commands

| Command | Effect |
|---------|--------|
| `exit` or `quit` | Disconnect from server |

---

## Running Tests

```bash
cd inmem-db
mvn test
```

Expected output:

```
[INFO] Running com.inmemdb.server.DatabaseTest
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The test suite covers:
- DDL: `CREATE TABLE` (including duplicate error), `DROP TABLE`
- `INSERT` (including wrong column count error)
- `SELECT *`, projection, `WHERE` with all operators, AND/OR
- Empty result sets
- `UPDATE` and `DELETE` with and without `WHERE`
- `DOUBLE` and `BOOLEAN` data types
- Trailing semicolons
- Case-insensitive keywords

---

## Wire Protocol

The server and CLI communicate over plain TCP using a line-delimited text protocol.
You can test it directly with `telnet` or `netcat`:

```bash
telnet localhost 5432
```

| Direction | Format |
|-----------|--------|
| Client → Server | `<SQL statement>\n` |
| Server → Client (query result) | Result lines, then `##END##\n` |
| Server → Client (DDL/DML) | `OK\n##END##\n` or `n row(s) …\n##END##\n` |
| Server → Client (error) | `ERROR: <message>\n##END##\n` |

The `##END##` sentinel tells the client when a full server response has been received.

---

## Architecture

```
CLI Client
    │
    │  TCP (line-delimited text)
    ▼
Server (ServerSocket)
    └── ClientHandler (per connection)
            │
            ▼  raw SQL string
        Lexer  →  Token list
            │
            ▼
        Parser  →  AST (Statement node)
            │
            ▼
        Planner  →  validated AST (schema checks)
            │
            ▼
        Executor  →  operates on Catalog / Table / Row
            │
            ▼
        ResultSet / "OK" / "n row(s) …"
            │
            ▼  serialized to text
        ClientHandler  →  TCP socket
```

---

## Roadmap

| Phase | Status | What's included |
|-------|--------|-----------------|
| **1** | ✅ Done | Basic CRUD, WHERE with all operators, TCP server + CLI |
| **2** | ✅ Done | PRIMARY KEY constraint, NULL handling, type coercion |
| **3** | Planned | AND/OR in WHERE, ORDER BY, LIMIT |
| **4** | ✅ Done | Hash index on columns for O(1) lookups |
| **5** | Planned | Transaction support (BEGIN / COMMIT / ROLLBACK) |
| **6** | Planned | JOIN support (INNER JOIN, LEFT JOIN) |
| **7** | Planned | Aggregate functions (COUNT, SUM, AVG, GROUP BY) |
| **8** | Planned | Persistence layer (serialize to disk) |
| **9** | ✅ Done | Multi-database support (CREATE DATABASE, USE, SHOW DATABASES) |
