package com.huangyiming;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP front end of the calculator.
 *
 * <p>The class only deals with the transport: it binds a port, routes requests
 * and writes JSON. All arithmetic stays in {@link Calculator}, so the logic
 * tested in the earlier labs is reused unchanged.
 */
public final class CalcServer {

    private static final String JSON = "application/json; charset=utf-8";

    private final HttpServer server;
    private final Calculator calculator = new Calculator();

    /**
     * Binds the port and registers the routes, without starting the server.
     * Binding separately from starting keeps the object testable: a test can
     * bind port 0, ask {@link #port()} which port the system picked, and stop
     * it again.
     */
    public CalcServer(int port) throws IOException {
        // InetSocketAddress(port) listens on 0.0.0.0. Writing "127.0.0.1"
        // here would only accept connections from inside the container, and
        // Docker's port forwarding would never reach it.
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/calc", this::handleCalc);
    }

    public void start() {
        // Returns immediately; the dispatcher thread keeps the JVM alive.
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    /** The port actually bound, which matters when 0 was requested. */
    public int port() {
        return server.getAddress().getPort();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                sendJson(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"ok\"}");
        } finally {
            exchange.close();
        }
    }

    /**
     * GET /api/calc?a=2&op=%2B&b=3 -> {"a":"2","op":"+","b":"3","result":5.0}
     *
     * <p>Invalid input is a 400, an impossible calculation (division by zero)
     * is a 422. The distinction tells the caller whether to fix the request or
     * the values inside it.
     */
    private void handleCalc(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                sendJson(exchange, 405, error("method not allowed"));
                return;
            }

            Map<String, String> params = queryParameters(exchange.getRequestURI().getRawQuery());
            String a = params.get("a");
            String op = params.get("op");
            String b = params.get("b");
            if (a == null || op == null || b == null) {
                sendJson(exchange, 400, error("parameters a, op and b are required"));
                return;
            }

            try {
                double result = calculator.eval(a, op, b);
                if (!Double.isFinite(result)) {
                    // Infinity and NaN are not valid JSON numbers.
                    sendJson(exchange, 422, error("result is not a finite number"));
                    return;
                }
                sendJson(exchange, 200, "{\"a\":" + quote(a)
                        + ",\"op\":" + quote(op)
                        + ",\"b\":" + quote(b)
                        + ",\"result\":" + result + "}");
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, error(e.getMessage()));
            } catch (ArithmeticException e) {
                sendJson(exchange, 422, error(e.getMessage()));
            }
        } finally {
            exchange.close();
        }
    }

    /** Splits "a=2&op=%2B" into a map; the first value of a name wins. */
    private static Map<String, String> queryParameters(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String name = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            params.putIfAbsent(decode(name), decode(value));
        }
        return params;
    }

    /** Percent-decodes one query parameter. */
    private static String decode(String value) {
        // URLDecoder implements form encoding, where "+" stands for a space.
        // In a query string the plus is the addition operator, so it is
        // protected before decoding; a real space must arrive as %20.
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static String error(String message) {
        return "{\"error\":" + quote(message == null ? "unknown error" : message) + "}";
    }

    /** Escapes a string for JSON; user input may contain quotes or control characters. */
    private static String quote(String value) {
        StringBuilder json = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        return json.append('"').toString();
    }

    /**
     * Writes a complete JSON response.
     *
     * <p>The length passed to sendResponseHeaders is a number of bytes, not
     * characters. Passing a wrong value makes the client wait for data that
     * never arrives, which looks like a hung server.
     */
    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        // Health checks run every few seconds, so they would flood the log.
        if (!"/api/health".equals(exchange.getRequestURI().getPath())) {
            System.out.println(exchange.getRequestMethod() + " "
                    + exchange.getRequestURI() + " -> " + status);
        }

        exchange.getResponseHeaders().set("Content-Type", JSON);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
