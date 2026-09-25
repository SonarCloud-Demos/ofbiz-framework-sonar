package org.apache.ofbiz.modern.catalog;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

final class ProductCatalogApplication {
    static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String GET = "GET";
    private volatile OfbizCatalogSnapshot snapshot;
    private final OfbizCatalogClient ofbizClient;
    private final CatalogStore shadowStore;

    ProductCatalogApplication(OfbizCatalogSnapshot snapshot) {
        this(snapshot, null, null);
    }

    ProductCatalogApplication(OfbizCatalogSnapshot snapshot, OfbizCatalogClient ofbizClient) {
        this(snapshot, ofbizClient, null);
    }

    ProductCatalogApplication(OfbizCatalogSnapshot snapshot, OfbizCatalogClient ofbizClient,
            CatalogStore shadowStore) {
        this.snapshot = snapshot;
        this.ofbizClient = ofbizClient;
        this.shadowStore = shadowStore;
    }

    static ProductCatalogApplication load() throws IOException {
        Clock clock = Clock.systemUTC();
        OfbizCatalogSnapshot fallback = OfbizCatalogSnapshot.load(clock);
        String sourceUrl = System.getenv("OFBIZ_CATALOG_SNAPSHOT_URL");
        String changesUrl = System.getenv("OFBIZ_CATALOG_CHANGES_URL");
        String token = System.getenv("LEGACY_CATALOG_SYNC_TOKEN");
        if (sourceUrl == null || sourceUrl.isBlank() || token == null || token.isBlank()) {
            return new ProductCatalogApplication(fallback);
        }
        URI changesUri = changesUrl == null || changesUrl.isBlank() ? null : URI.create(changesUrl);
        OfbizCatalogClient client = new OfbizCatalogClient(URI.create(sourceUrl), changesUri, token,
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(java.time.Duration.ofSeconds(3)).build(), clock);
        CatalogStore store = configuredStore(clock);
        return new ProductCatalogApplication(store.loadOr(fallback), client, store);
    }

    private static CatalogStore configuredStore(Clock clock) throws IOException {
        String jdbcUrl = System.getenv("CATALOG_DATABASE_URL");
        String username = System.getenv("CATALOG_DATABASE_USER");
        String password = System.getenv("CATALOG_DATABASE_PASSWORD");
        if (jdbcUrl != null && !jdbcUrl.isBlank() && username != null && password != null) {
            return new PostgresCatalogStore(jdbcUrl, username, password, clock);
        }
        return new CatalogShadowStore(Path.of("/var/lib/catalog/shadow.tsv"), clock);
    }

    HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health/live", exchange -> health(exchange, "UP"));
        server.createContext("/health/ready", exchange -> health(exchange, "READY"));
        server.createContext("/api/catalog/products", this::products);
        server.createContext("/api/catalog/categories", this::categories);
        server.createContext("/internal/reconciliation", this::reconciliation);
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    private void health(HttpExchange exchange, String status) throws IOException {
        if (!requireGet(exchange)) {
            return;
        }
        send(exchange, 200, "{\"status\":\"" + status + "\"}");
    }

    private void products(HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) {
            return;
        }
        if (!refreshFromOfbiz()) {
            send(exchange, 503, "{\"error\":\"ofbiz_catalog_unavailable\"}");
            return;
        }
        Map<String, String> query = query(exchange);
        String search = query.getOrDefault("q", "").toLowerCase(Locale.ROOT);
        String categoryId = query.get("categoryId");
        List<CatalogItem> matches = snapshot.items().stream()
                .filter(item -> "ACTIVE".equals(item.status()))
                .filter(item -> categoryId == null || categoryId.equals(item.categoryId()))
                .filter(item -> search.isBlank() || item.id().toLowerCase(Locale.ROOT).contains(search)
                        || item.name().toLowerCase(Locale.ROOT).contains(search))
                .toList();
        send(exchange, 200, "{\"items\":[" + matches.stream().map(ProductCatalogApplication::itemJson)
                .collect(Collectors.joining(",")) + "],\"count\":" + matches.size()
                + ",\"refreshedAt\":\"" + snapshot.refreshedAt() + "\"}");
    }

    private void categories(HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) {
            return;
        }
        if (!refreshFromOfbiz()) {
            send(exchange, 503, "{\"error\":\"ofbiz_catalog_unavailable\"}");
            return;
        }
        Map<String, String> categories = snapshot.items().stream()
                .filter(item -> "ACTIVE".equals(item.status()))
                .collect(Collectors.toMap(CatalogItem::categoryId, CatalogItem::categoryName,
                        (first, ignored) -> first, LinkedHashMap::new));
        String json = categories.entrySet().stream()
                .map(entry -> "{\"id\":\"" + json(entry.getKey()) + "\",\"name\":\""
                        + json(entry.getValue()) + "\"}")
                .collect(Collectors.joining(","));
        send(exchange, 200, "{\"items\":[" + json + "]}");
    }

    private void reconciliation(HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) {
            return;
        }
        if (ofbizClient == null) {
            send(exchange, 200, reconciliationJson(snapshot.items().size(), 0, false));
            return;
        }
        try {
            OfbizCatalogSnapshot source = ofbizClient.fetch();
            send(exchange, 200, reconciliationJson(source.items().size(), snapshot.mismatchesWith(source), true));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            send(exchange, 503, "{\"error\":\"ofbiz_catalog_unavailable\"}");
        } catch (IOException unavailable) {
            send(exchange, 503, "{\"error\":\"ofbiz_catalog_unavailable\"}");
        }
    }

    private boolean refreshFromOfbiz() {
        if (ofbizClient == null) {
            return true;
        }
        try {
            if (ofbizClient.supportsChanges() && shadowStore != null) {
                refreshFromChanges();
                return true;
            }
            OfbizCatalogSnapshot refreshed = ofbizClient.fetch();
            if (shadowStore != null) {
                shadowStore.save(refreshed);
            }
            snapshot = refreshed;
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException unavailable) {
            return false;
        }
    }

    private void refreshFromChanges() throws IOException, InterruptedException {
        long cursor = shadowStore.sourceCursor();
        if (cursor < 0) {
            OfbizCatalogSnapshot bootstrap = ofbizClient.fetch();
            shadowStore.bootstrapSource(bootstrap);
            snapshot = bootstrap;
            cursor = 0;
        }
        List<OfbizCatalogChange> changes = ofbizClient.fetchChanges(cursor);
        if (!changes.isEmpty()) {
            shadowStore.applySourceChanges(changes, java.time.Instant.now());
            snapshot = shadowStore.loadOr(snapshot);
        }
    }

    private String reconciliationJson(int checked, int mismatches, boolean liveSource) {
        return "{\"source\":\"ofbiz\",\"liveSource\":" + liveSource + ",\"checked\":" + checked
                + ",\"mismatches\":" + mismatches
                + ",\"threshold\":\"not-configured\",\"refreshedAt\":\""
                + snapshot.refreshedAt() + "\"}";
    }

    private static boolean requireGet(HttpExchange exchange) throws IOException {
        if (GET.equals(exchange.getRequestMethod())) {
            return true;
        }
        exchange.getResponseHeaders().set("Allow", GET);
        send(exchange, 405, "{\"error\":\"method_not_allowed\"}");
        return false;
    }

    private static Map<String, String> query(HttpExchange exchange) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(decode(parts[0]), decode(parts[1]));
            }
        }
        return values;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String itemJson(CatalogItem item) {
        return "{\"id\":\"" + json(item.id()) + "\",\"name\":\"" + json(item.name())
                + "\",\"categoryId\":\"" + json(item.categoryId()) + "\",\"status\":\""
                + json(item.status()) + "\",\"displayedPriceReference\":\""
                + json(item.displayedPriceReference()) + "\"}";
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", JSON_CONTENT_TYPE);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set(CORRELATION_HEADER, correlationId(exchange));
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(bytes);
        }
    }

    private static String correlationId(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst(CORRELATION_HEADER))
                .filter(value -> value.matches("[A-Za-z0-9._-]{1,128}"))
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}
