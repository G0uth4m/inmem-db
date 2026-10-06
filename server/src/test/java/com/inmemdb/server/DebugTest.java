package com.inmemdb.server;

import com.inmemdb.server.sql.parser.*;
import com.inmemdb.server.sql.lexer.*;
import com.inmemdb.server.sql.parser.ast.*;

public class DebugTest {
    public static void main(String[] args) {
        Lexer lexer = new Lexer("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(50) NOT NULL, age INTEGER)");
        Parser parser = new Parser(lexer.tokenize());
        Statement stmt = parser.parse();
        if (stmt instanceof CreateTableStatement) {
            CreateTableStatement ct = (CreateTableStatement) stmt;
            for (com.inmemdb.server.catalog.Column c : ct.getColumns()) {
                System.out.println(c.getName() + " type: " + c.getType() + " dt_isnull: " + (c.getType() == null));
            }
        }
    }
}
