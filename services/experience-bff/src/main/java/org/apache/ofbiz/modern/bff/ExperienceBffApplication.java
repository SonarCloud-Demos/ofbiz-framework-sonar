package org.apache.ofbiz.modern.bff;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

public final class ExperienceBffApplication {
    static final String CORRELATION_HEADER = "X-Correlation-ID";
    static final String TRACE_PARENT_HEADER = "traceparent";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final System.Logger LOGGER = System.getLogger(ExperienceBffApplication.class.getName());
    private static final Map<String, String> SHELL_RESOURCES = Map.of(
            "/", "shell/index.html",
            "/app.css", "shell/app.css",
            "/app.js", "shell/app.js");
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "/", "text/html; charset=utf-8",
            "/app.css", "text/css; charset=utf-8",
            "/app.js", "text/javascript; charset=utf-8");

    private final URI referenceUri;
    private final URI legacyUri;
    private final HttpClient httpClient;
    private final byte[] expectedAuthorization;

    ExperienceBffApplication(URI referenceUri, URI legacyUri, HttpClient httpClient, String username, String password) {
        this.referenceUri = referenceUri;
        this.legacyUri = legacyUri;
        this.httpClient = httpClient;
        this.expectedAuthorization = ("Basic " + Base64.getEncoder().encodeToString(
                (username + ':' + password).getBytes(StandardCharsets.UTF_8))).getBytes(StandardCharsets.US_ASCII);
    }

    HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        configure(server);
        return server;
    }

    void configure(HttpServer server) {
        server.createContext("/health/live", exchange -> send(exchange, 200, JSON_CONTENT_TYPE,
                "{\"status\":\"UP\"}"));
        server.createContext("/api/reference", this::reference);
        server.createContext("/api/legacy/health", this::legacyHealth);
        server.createContext("/", this::shell);
        server.setExecutor(Executors.newCachedThreadPool());
    }

    private void reference(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            send(exchange, 405, JSON_CONTENT_TYPE, "{\"error\":\"method_not_allowed\"}");
            return;
        }
        String correlationId = correlationId(exchange);
        String traceParent = traceParent(exchange);
        HttpRequest request = HttpRequest.newBuilder(referenceUri.resolve("/api/reference"))
                .timeout(Duration.ofSeconds(3))
                .header(CORRELATION_HEADER, correlationId)
                .header(TRACE_PARENT_HEADER, traceParent)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseTraceParent = response.headers().firstValue(TRACE_PARENT_HEADER)
                    .filter(ExperienceBffApplication::isValidTraceParent)
                    .orElse(traceParent);
            exchange.getResponseHeaders().set(CORRELATION_HEADER, correlationId);
            exchange.getResponseHeaders().set(TRACE_PARENT_HEADER, responseTraceParent);
            LOGGER.log(System.Logger.Level.INFO, "reference-response correlationId={0} traceId={1} status={2}",
                    correlationId, responseTraceParent.substring(3, 35), response.statusCode());
            send(exchange, response.statusCode(), JSON_CONTENT_TYPE, response.body());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            sendUnavailable(exchange, correlationId);
        } catch (IOException unavailable) {
            sendUnavailable(exchange, correlationId);
        }
    }

    private void shell(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String resourceName = SHELL_RESOURCES.get(path);
        if (!"GET".equals(exchange.getRequestMethod()) || resourceName == null) {
            send(exchange, 404, JSON_CONTENT_TYPE, "{\"error\":\"not_found\"}");
            return;
        }
        try (InputStream resource = ExperienceBffApplication.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (resource == null) {
                send(exchange, 500, JSON_CONTENT_TYPE, "{\"error\":\"shell_unavailable\"}");
                return;
            }
            send(exchange, 200, CONTENT_TYPES.get(path), resource.readAllBytes());
        }
    }

    private void legacyHealth(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        String correlationId = correlationId(exchange);
        HttpRequest request = HttpRequest.newBuilder(legacyUri)
                .timeout(Duration.ofSeconds(3))
                .header(CORRELATION_HEADER, correlationId)
                .GET()
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            exchange.getResponseHeaders().set(CORRELATION_HEADER, correlationId);
            send(exchange, 200, JSON_CONTENT_TYPE, "{\"runtime\":\"legacy\",\"reachable\":"
                    + (response.statusCode() < 500) + '}');
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            sendUnavailable(exchange, correlationId);
        } catch (IOException unavailable) {
            sendUnavailable(exchange, correlationId);
        }
    }

    static String correlationId(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(CORRELATION_HEADER))
                .filter(value -> value.matches("[A-Za-z0-9._-]{1,128}"))
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    static String traceParent(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(TRACE_PARENT_HEADER))
                .filter(ExperienceBffApplication::isValidTraceParent)
                .orElseGet(() -> "00-" + UUID.randomUUID().toString().replace("-", "") + '-'
                        + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01");
    }

    private static boolean isValidTraceParent(String value) {
        return value.matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[01]");
    }

    private boolean authorize(HttpExchange exchange) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        boolean accepted = authorization != null && MessageDigest.isEqual(expectedAuthorization,
                authorization.getBytes(StandardCharsets.US_ASCII));
        if (!accepted) {
            exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"OFBiz local development\"");
            send(exchange, 401, JSON_CONTENT_TYPE, "{\"error\":\"authentication_required\"}");
        }
        return accepted;
    }

    private static void sendUnavailable(HttpExchange exchange, String correlationId) throws IOException {
        exchange.getResponseHeaders().set(CORRELATION_HEADER, correlationId);
        send(exchange, 503, JSON_CONTENT_TYPE, "{\"error\":\"reference_unavailable\"}");
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        send(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(body);
        }
    }
}
