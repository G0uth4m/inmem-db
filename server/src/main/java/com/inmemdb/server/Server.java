package com.inmemdb.server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Entry point for the InMemDB server.
 *
 * Usage:
 *   java -jar inmemdb-server.jar [--port <port>]
 *
 * Default port: 5432
 *
 * The server runs single-threaded in Phase 1: it accepts one connection,
 * handles it fully, then waits for the next.
 * (Concurrent connections come in Phase 5 with transaction support.)
 */
public class Server {

    private static final int DEFAULT_PORT = 5432;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;

        // Parse optional --port argument
        for (int i = 0; i < args.length - 1; i++) {
            if ("--port".equals(args[i])) {
                port = Integer.parseInt(args[i + 1]);
            }
        }

        // Shared server catalog
        com.inmemdb.server.catalog.ServerCatalog catalog = new com.inmemdb.server.catalog.ServerCatalog();

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║   InMemDB Server  (Phase 9)      ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println("Listening on port " + port + " ...");
        System.out.println("Connect with:  java -jar inmemdb-cli.jar --port " + port);
        System.out.println();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept();
                // Phase 1 / 9: handle synchronously (one client at a time)
                Database db = new Database(catalog);
                new ClientHandler(client, db).run();
            }
        }
    }
}
