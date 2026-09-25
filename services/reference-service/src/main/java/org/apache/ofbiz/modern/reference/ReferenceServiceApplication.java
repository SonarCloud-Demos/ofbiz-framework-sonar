package org.apache.ofbiz.modern.reference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class ReferenceServiceApplication {
    static final String CORRELATION_HEADER = "X-Correlation-ID";
    static final String TRACE_PARENT_HEADER = "traceparent";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final System.Logger LOGGER = System.getLogger(ReferenceServiceApplication.class.getName());

    private ReferenceServiceApplication() {
    }

    static HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health/live", exchange -> sendJson(exchange, 200, "{\"status\":\"UP\"}"));
        server.createContext("/health/ready", exchange -> sendJson(exchange, 200, "{\"status\":\"READY\"}"));
        server.createContext("/api/reference", ReferenceServiceApplication::reference);
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    private static void reference(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            sendJson(exchange, 405, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String correlationId = correlationId(exchange);
        String traceParent = traceParent(exchange);
        exchange.getResponseHeaders().set(CORRELATION_HEADER, correlationId);
        exchange.getResponseHeaders().set(TRACE_PARENT_HEADER, traceParent);
        LOGGER.log(System.Logger.Level.INFO, "reference-request correlationId={0} traceId={1}",
                correlationId, traceParent.substring(3, 35));
        sendJson(exchange, 200, "{\"service\":\"reference-service\",\"correlationId\":\""
                + correlationId + "\",\"traceId\":\"" + traceParent.substring(3, 35) + "\"}");
    }

    static String correlationId(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(CORRELATION_HEADER))
                .filter(ReferenceServiceApplication::isValidCorrelationId)
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    static boolean isValidCorrelationId(String value) {
        return value != null && value.matches("[A-Za-z0-9._-]{1,128}");
    }

    static String traceParent(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(TRACE_PARENT_HEADER))
                .filter(ReferenceServiceApplication::isValidTraceParent)
                .orElseGet(ReferenceServiceApplication::newTraceParent);
    }

    static boolean isValidTraceParent(String value) {
        return value != null && value.matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]")
                && !value.substring(3, 35).equals("0".repeat(32))
                && !value.substring(36, 52).equals("0".repeat(16));
    }

    private static String newTraceParent() {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return "00-" + traceId + '-' + spanId + "-01";
    }

    static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON_CONTENT_TYPE);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(bytes);
        }
    }
}
