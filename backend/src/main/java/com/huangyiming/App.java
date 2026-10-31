package com.huangyiming;

import java.io.IOException;

/**
 * Entry point with two modes.
 *
 * <p>No arguments starts the HTTP service, which is what the container runs.
 * Three arguments keep the command-line behaviour of Labs 3-6, so the old
 * {@code docker run ... 2 + 2} usage and its CI smoke test still work.
 */
public class App {

    private static final int DEFAULT_PORT = 8000;

    public static void main(String[] args) {
        if (args.length == 0) {
            startServer();
            return;
        }
        if (args.length != 3) {
            printUsage();
            return;
        }

        Calculator calculator = new Calculator();
        try {
            double result = calculator.eval(args[0], args[1], args[2]);
            System.out.println(result);
        } catch (IllegalArgumentException | ArithmeticException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void startServer() {
        int port = configuredPort();
        try {
            CalcServer server = new CalcServer(port);
            server.start();
            System.out.println("calculator backend listening on 0.0.0.0:" + server.port());
        } catch (IOException e) {
            // Exit non-zero so the container stops and its restart policy
            // (restart: on-failure) can decide what to do next.
            System.err.println("Cannot listen on port " + port + ": " + e.getMessage());
            System.exit(1);
        }
    }

    /** Reads PORT from the environment, falling back to 8000. */
    private static int configuredPort() {
        String value = System.getenv("PORT");
        if (value == null || value.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // A bad value must not crash the container at start-up.
            System.err.println("Ignoring invalid PORT=" + value + ", using " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar calculator-1.0.jar                    start the HTTP service");
        System.out.println("  java -jar calculator-1.0.jar <number> <operator> <number>");
        System.out.println("Example: java -jar calculator-1.0.jar 2 + 2");
    }
}
