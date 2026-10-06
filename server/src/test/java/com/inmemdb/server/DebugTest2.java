package com.inmemdb.server;

import com.inmemdb.server.sql.parser.*;
import com.inmemdb.server.sql.lexer.*;
import com.inmemdb.server.sql.parser.ast.*;

public class DebugTest2 {
    public static void main(String[] args) {
        Lexer lexer = new Lexer("INSERT INTO users VALUES (NULL, 'Alice', 30)");
        Parser parser = new Parser(lexer.tokenize());
        Statement stmt = parser.parse();
        if (stmt instanceof InsertStatement) {
            InsertStatement ins = (InsertStatement) stmt;
            for (Object v : ins.getValues()) {
                System.out.println("VAL: " + v + " IS_NULL: " + (v == null));
            }
        }
    }
}
