package com.huangyiming;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the HTTP layer over a real socket.
 *
 * <p>Port 0 lets the operating system pick a free port, so the tests never
 * collide with a running instance or with each other.
 */
class CalcServerTest {

    private CalcServer server;
    private HttpClient client;
    private String base;

    @BeforeEach
    void startServer() throws IOException {
        server = new CalcServer(0);
        server.start();
        base = "http://127.0.0.1:" + server.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthReportsOk() throws Exception {
        HttpResponse<String> response = get("/api/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"ok\""));
        assertEquals("application/json; charset=utf-8",
                response.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void encodedPlusIsAccepted() throws Exception {
        HttpResponse<String> response = get("/api/calc?a=2&op=%2B&b=3");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"result\":5.0"));
    }

    // A bare plus in the query string must stay a plus. Without this test the
    // form-encoding rule of URLDecoder silently turns it into a space.
    @Test
    void barePlusIsAccepted() throws Exception {
        HttpResponse<String> response = get("/api/calc?a=2&op=+&b=3");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"result\":5.0"));
    }

    @Test
    void divisionByZeroIsUnprocessable() throws Exception {
        HttpResponse<String> response = get("/api/calc?a=8&op=%2F&b=0");

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("division by zero"));
    }

    @Test
    void missingParameterIsABadRequest() throws Exception {
        HttpResponse<String> response = get("/api/calc?a=2&b=3");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"error\""));
    }

    @Test
    void nonNumericOperandIsABadRequest() throws Exception {
        HttpResponse<String> response = get("/api/calc?a=abc&op=%2B&b=1");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("not a valid number"));
    }
}
