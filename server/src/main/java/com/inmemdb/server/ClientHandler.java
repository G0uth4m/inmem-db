package com.inmemdb.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles a single client connection.
 * Protocol:
 *   - Client sends one SQL statement per line (terminated by '\n').
 *   - Server responds with result lines, then "##END##\n".
 *   - Client sends "exit" or "quit" to close the connection.
 */
public class ClientHandler implements Runnable {

    /** Wire-protocol end-of-response sentinel. */
    public static final String END_MARKER = "##END##";

    private final Socket   socket;
    private final Database db;

    public ClientHandler(Socket socket, Database db) {
        this.socket = socket;
        this.db     = db;
    }

    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        System.out.println("[server] Client connected: " + clientAddr);

        try (
            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter    out = new PrintWriter(socket.getOutputStream(), /* autoFlush */ true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                String sql = line.trim();

                // Handle disconnect commands
                if (sql.equalsIgnoreCase("exit") || sql.equalsIgnoreCase("quit")) {
                    out.println("Bye!");
                    out.println(END_MARKER);
                    break;
                }

                // Skip empty lines / pure semicolons
                if (sql.isEmpty() || sql.equals(";")) {
                    out.println(END_MARKER);
                    continue;
                }

                // Execute and send back result
                String result = db.execute(sql);
                out.println(result);
                out.println(END_MARKER);
            }

        } catch (Exception e) {
            System.err.println("[server] Error handling client " + clientAddr + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
            System.out.println("[server] Client disconnected: " + clientAddr);
        }
    }
}
