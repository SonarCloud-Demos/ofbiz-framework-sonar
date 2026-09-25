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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public final class ExperienceBffApplication {
    static final String CORRELATION_HEADER = "X-Correlation-ID";
    static final String TRACE_PARENT_HEADER = "traceparent";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String SESSION_COOKIE = "OFBIZ_MODERN_SESSION";
    private static final System.Logger LOGGER = System.getLogger(ExperienceBffApplication.class.getName());
    private static final Map<String, String> SHELL_RESOURCES = Map.of(
            "/", "shell/index.html",
            "/catalog", "shell/index.html",
            "/app.css", "shell/app.css",
            "/app.js", "shell/app.js");
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "/", "text/html; charset=utf-8",
            "/catalog", "text/html; charset=utf-8",
            "/app.css", "text/css; charset=utf-8",
            "/app.js", "text/javascript; charset=utf-8");

    private final URI referenceUri;
    private final URI catalogUri;
    private final URI legacyUri;
    private final HttpClient httpClient;
    private final byte[] expectedAuthorization;
    private final String authenticatedUsername;
    private final boolean modernRoutesEnabled;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    ExperienceBffApplication(URI referenceUri, URI legacyUri, HttpClient httpClient, String username, String password) {
        this(referenceUri, referenceUri, legacyUri, httpClient, username, password, true);
    }

    ExperienceBffApplication(URI referenceUri, URI legacyUri, HttpClient httpClient, String username, String password,
            boolean modernRoutesEnabled) {
        this(referenceUri, referenceUri, legacyUri, httpClient, username, password, modernRoutesEnabled);
    }

    ExperienceBffApplication(URI referenceUri, URI catalogUri, URI legacyUri, HttpClient httpClient, String username,
            String password, boolean modernRoutesEnabled) {
        this.referenceUri = referenceUri;
        this.catalogUri = catalogUri;
        this.legacyUri = legacyUri;
        this.httpClient = httpClient;
        this.authenticatedUsername = validatedUsername(username);
        this.modernRoutesEnabled = modernRoutesEnabled;
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
        server.createContext("/api/catalog/products", this::catalog);
        server.createContext("/api/catalog/categories", this::catalog);
        server.createContext("/api/legacy/health", this::legacyHealth);
        server.createContext("/modern/profile", this::profile);
        server.createContext("/route-manifest.json", this::routeManifest);
        server.createContext("/legacy-session", this::legacySession);
        server.createContext("/", this::shell);
        server.setExecutor(Executors.newCachedThreadPool());
    }

    private void profile(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        if (!modernRoutesEnabled) {
            exchange.getResponseHeaders().set("Location", "/webtools/control/main");
            send(exchange, 307, JSON_CONTENT_TYPE, "{\"route\":\"legacy\",\"reason\":\"failback\"}");
            return;
        }
        send(exchange, 200, JSON_CONTENT_TYPE, "{\"route\":\"modern\",\"subject\":\""
                + authenticatedUsername + "\",\"legacySession\":\"available\"}");
    }

    private void routeManifest(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        String profileRuntime = modernRoutesEnabled ? "modern" : "legacy";
        String catalogRuntime = modernRoutesEnabled ? "modern" : "legacy";
        send(exchange, 200, JSON_CONTENT_TYPE, "{\"default\":\"legacy\",\"routes\":["
                + "{\"path\":\"/modern/profile\",\"runtime\":\"" + profileRuntime + "\"},"
                + "{\"path\":\"/catalog\",\"runtime\":\"" + catalogRuntime + "\"},"
                + "{\"path\":\"/webtools/*\",\"runtime\":\"legacy\"}]}");
    }

    private void legacySession(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            createLegacySession(exchange);
        } else if ("DELETE".equals(exchange.getRequestMethod())) {
            deleteLegacySession(exchange);
        } else {
            methodNotAllowed(exchange, "POST, DELETE");
        }
    }

    private void createLegacySession(HttpExchange exchange) throws IOException {
        String sessionId = UUID.randomUUID().toString();
        String csrfToken = UUID.randomUUID().toString();
        sessions.clear();
        sessions.put(sessionId, csrfToken);
        exchange.getResponseHeaders().set("Set-Cookie", SESSION_COOKIE + '=' + sessionId
                + "; Path=/; Secure; HttpOnly; SameSite=Strict");
        send(exchange, 201, JSON_CONTENT_TYPE, "{\"csrfToken\":\"" + csrfToken
                + "\",\"identity\":\"" + authenticatedUsername + "\"}");
    }

    private void deleteLegacySession(HttpExchange exchange) throws IOException {
        Optional<String> sessionId = sessionId(exchange);
        String suppliedToken = exchange.getRequestHeaders().getFirst("X-CSRF-Token");
        boolean accepted = sessionId.map(sessions::get)
                .filter(expected -> suppliedToken != null && MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.US_ASCII),
                        suppliedToken.getBytes(StandardCharsets.US_ASCII)))
                .isPresent();
        if (!accepted) {
            send(exchange, 403, JSON_CONTENT_TYPE, "{\"error\":\"csrf_validation_failed\"}");
            return;
        }
        sessions.remove(sessionId.orElseThrow());
        exchange.getResponseHeaders().set("Set-Cookie", SESSION_COOKIE
                + "=; Path=/; Secure; HttpOnly; SameSite=Strict; Max-Age=0");
        send(exchange, 204, JSON_CONTENT_TYPE, new byte[0]);
    }

    private static Optional<String> sessionId(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst("Cookie"))
                .stream()
                .flatMap(header -> java.util.Arrays.stream(header.split(";")))
                .map(String::trim)
                .filter(cookie -> cookie.startsWith(SESSION_COOKIE + '='))
                .map(cookie -> cookie.substring(SESSION_COOKIE.length() + 1))
                .filter(value -> value.matches("[0-9a-f-]{36}"))
                .findFirst();
    }

    private static String validatedUsername(String username) {
        if (!username.matches("[A-Za-z0-9._@-]{1,128}")) {
            throw new IllegalArgumentException("The local authentication username contains unsupported characters");
        }
        return username;
    }

    private void reference(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
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

    private void catalog(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        if (!modernRoutesEnabled) {
            exchange.getResponseHeaders().set("Location", "/catalog/control/main");
            send(exchange, 307, JSON_CONTENT_TYPE, "{\"route\":\"legacy\",\"reason\":\"failback\"}");
            return;
        }
        String pathAndQuery = exchange.getRequestURI().getRawPath();
        if (exchange.getRequestURI().getRawQuery() != null) {
            pathAndQuery += '?' + exchange.getRequestURI().getRawQuery();
        }
        proxyCatalog(exchange, catalogUri.resolve(pathAndQuery));
    }

    private void proxyCatalog(HttpExchange exchange, URI target) throws IOException {
        String correlationId = correlationId(exchange);
        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(3))
                .header(CORRELATION_HEADER, correlationId)
                .GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            exchange.getResponseHeaders().set(CORRELATION_HEADER, correlationId);
            send(exchange, response.statusCode(), JSON_CONTENT_TYPE, response.body());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            send(exchange, 503, JSON_CONTENT_TYPE, "{\"error\":\"catalog_unavailable\"}");
        } catch (IOException unavailable) {
            send(exchange, 503, JSON_CONTENT_TYPE, "{\"error\":\"catalog_unavailable\"}");
        }
    }

    private static void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        send(exchange, 405, JSON_CONTENT_TYPE, "{\"error\":\"method_not_allowed\"}");
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
