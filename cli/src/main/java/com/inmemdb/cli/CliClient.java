package com.inmemdb.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * InMemDB CLI Client — a thin REPL that connects to the InMemDB server over TCP.
 *
 * Usage:
 *   java -jar inmemdb-cli.jar [--host <host>] [--port <port>]
 *
 * Defaults: host=localhost, port=5432
 *
 * Protocol:
 *   - Sends each SQL line terminated with '\n'
 *   - Reads response lines until "##END##" is received
 *   - Prints all response lines (except ##END##) to stdout
 */
public class CliClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int    DEFAULT_PORT = 5432;
    private static final String END_MARKER   = "##END##";
    private static final String PROMPT       = "InMemDB> ";

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int    port = DEFAULT_PORT;

        // Parse optional --host and --port arguments
        for (int i = 0; i < args.length - 1; i++) {
            if ("--host".equals(args[i])) host = args[i + 1];
            if ("--port".equals(args[i])) port = Integer.parseInt(args[i + 1]);
        }

        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║   InMemDB CLI  (Phase 9)         ║");
        System.out.println("╚══════════════════════════════════╝");
        System.out.println("Connecting to " + host + ":" + port + " ...");

        try (
            Socket         socket = new Socket(host, port);
            PrintWriter    out    = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner        stdin  = new Scanner(System.in)
        ) {
            System.out.println("Connected! Type SQL statements and press Enter.");
            System.out.println("Type 'exit' or 'quit' to disconnect.\n");

            while (true) {
                System.out.print(PROMPT);
                if (!stdin.hasNextLine()) break;
                String line = stdin.nextLine().trim();

                if (line.isEmpty()) continue;

                // Send to server
                out.println(line);
                out.flush();

                // Read response until ##END##
                String responseLine;
                boolean hasOutput = false;
                while ((responseLine = in.readLine()) != null) {
                    if (END_MARKER.equals(responseLine)) break;
                    System.out.println(responseLine);
                    hasOutput = true;
                }

                if (hasOutput) System.out.println(); // blank line for readability

                // If user typed exit/quit the server replied "Bye!" — stop loop
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            System.err.println("Is the server running?  java -jar inmemdb-server.jar");
        }

        System.out.println("Disconnected. Goodbye!");
    }
}
