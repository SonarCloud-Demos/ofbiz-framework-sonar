package org.apache.ofbiz.modern.reference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceServiceApplicationTest {
    private HttpServer server;
    private URI baseUri;

    @BeforeEach
    void startServer() throws Exception {
        server = ReferenceServiceApplication.createServer(0);
        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void reportsLivenessAndReadiness() throws Exception {
        assertEquals(200, get("/health/live", null, null).statusCode());
        assertEquals(200, get("/health/ready", null, null).statusCode());
    }

    @Test
    void preservesValidCorrelationId() throws Exception {
        HttpResponse<String> response = get("/api/reference", "trace-123", null);

        assertEquals(200, response.statusCode());
        assertEquals("trace-123", response.headers().firstValue("X-Correlation-ID").orElseThrow());
        assertTrue(response.body().contains("trace-123"));
    }

    @Test
    void replacesInvalidCorrelationId() throws Exception {
        HttpResponse<String> response = get("/api/reference", "bad value", null);

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("bad value"));
    }

    @Test
    void rejectsUnsupportedMethod() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/reference"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        assertEquals("GET", response.headers().firstValue("Allow").orElseThrow());
    }

    @Test
    void validatesCorrelationIds() {
        assertTrue(ReferenceServiceApplication.isValidCorrelationId("valid_A.1-2"));
        assertFalse(ReferenceServiceApplication.isValidCorrelationId(""));
        assertFalse(ReferenceServiceApplication.isValidCorrelationId(null));
        assertFalse(ReferenceServiceApplication.isValidCorrelationId("x".repeat(129)));
    }

    @Test
    void preservesValidTraceContextAndReplacesInvalidContext() throws Exception {
        String traceParent = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        HttpResponse<String> preserved = get("/api/reference", null, traceParent);
        HttpResponse<String> replaced = get("/api/reference", null, "invalid");

        assertEquals(traceParent, preserved.headers().firstValue("traceparent").orElseThrow());
        assertTrue(preserved.body().contains("4bf92f3577b34da6a3ce929d0e0e4736"));
        assertTrue(ReferenceServiceApplication.isValidTraceParent(
                replaced.headers().firstValue("traceparent").orElseThrow()));
        assertFalse(ReferenceServiceApplication.isValidTraceParent(
                "00-00000000000000000000000000000000-00f067aa0ba902b7-01"));
    }

    private HttpResponse<String> get(String path, String correlationId, String traceParent) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path)).GET();
        if (correlationId != null) {
            request.header("X-Correlation-ID", correlationId);
        }
        if (traceParent != null) {
            request.header("traceparent", traceParent);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
