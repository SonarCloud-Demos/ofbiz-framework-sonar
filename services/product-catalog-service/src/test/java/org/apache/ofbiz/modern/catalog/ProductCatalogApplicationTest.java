package org.apache.ofbiz.modern.catalog;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCatalogApplicationTest {
    private HttpServer server;
    private URI baseUri;

    @BeforeEach
    void start() throws Exception {
        server = ProductCatalogApplication.load().createServer(0);
        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void exposesReadinessAndCatalogCategories() throws Exception {
        assertEquals(200, get("/health/ready").statusCode());
        HttpResponse<String> response = get("/api/catalog/categories");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("CATALOG_DEMO"));
        assertTrue(response.body().contains("SERVICES"));
    }

    @Test
    void filtersProductsByCategoryAndSearchText() throws Exception {
        HttpResponse<String> response = get("/api/catalog/products?categoryId=CATALOG_DEMO&q=premium");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("WG-1112"));
        assertFalse(response.body().contains("WG-1111"));
        assertFalse(response.body().contains("SV-1000"));
        assertTrue(response.body().contains("displayedPriceReference"));
    }

    @Test
    void reportsShadowReconciliationWithoutInventingThreshold() throws Exception {
        HttpResponse<String> response = get("/internal/reconciliation");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"mismatches\":0"));
        assertTrue(response.body().contains("\"threshold\":\"not-configured\""));
    }

    @Test
    void rejectsWritesAndReplacesUnsafeCorrelationIds() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/catalog/products"))
                .header("X-Correlation-ID", "unsafe value")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
        assertEquals("GET", response.headers().firstValue("Allow").orElseThrow());
        assertFalse(response.headers().firstValue("X-Correlation-ID").orElseThrow().contains(" "));
    }

    @Test
    void refreshesTheShadowFromTheAuthenticatedOfbizAdapter() throws Exception {
        HttpServer ofbiz = HttpServer.create(new InetSocketAddress(0), 0);
        ofbiz.createContext("/snapshot", exchange -> {
            assertEquals("sync-token", exchange.getRequestHeaders().getFirst("X-Modern-Catalog-Token"));
            byte[] body = encodedRow("LIVE-1", "Changed in OFBiz", "LIVE", "Live products", "ACTIVE", "USD 8.00")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        ofbiz.start();
        HttpServer liveCatalog = null;
        try {
            URI snapshotUri = URI.create("http://localhost:" + ofbiz.getAddress().getPort() + "/snapshot");
            OfbizCatalogClient client = new OfbizCatalogClient(snapshotUri, "sync-token",
                    HttpClient.newHttpClient(), Clock.systemUTC());
            liveCatalog = new ProductCatalogApplication(OfbizCatalogSnapshot.load(Clock.systemUTC()), client)
                    .createServer(0);
            liveCatalog.start();
            URI catalogUri = URI.create("http://localhost:" + liveCatalog.getAddress().getPort());
            HttpRequest request = HttpRequest.newBuilder(catalogUri.resolve("/api/catalog/products")).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("LIVE-1"));
            assertTrue(response.body().contains("Changed in OFBiz"));
            assertFalse(response.body().contains("WG-1111"));
        } finally {
            if (liveCatalog != null) {
                liveCatalog.stop(0);
            }
            ofbiz.stop(0);
        }
    }

    @Test
    void reportsUnavailableInsteadOfServingFallbackDataWhenLiveRefreshFails() throws Exception {
        HttpServer ofbiz = HttpServer.create(new InetSocketAddress(0), 0);
        ofbiz.createContext("/snapshot", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        ofbiz.start();
        HttpServer liveCatalog = null;
        try {
            URI snapshotUri = URI.create("http://localhost:" + ofbiz.getAddress().getPort() + "/snapshot");
            OfbizCatalogClient client = new OfbizCatalogClient(snapshotUri, "sync-token",
                    HttpClient.newHttpClient(), Clock.systemUTC());
            liveCatalog = new ProductCatalogApplication(OfbizCatalogSnapshot.load(Clock.systemUTC()), client)
                    .createServer(0);
            liveCatalog.start();
            URI catalogUri = URI.create("http://localhost:" + liveCatalog.getAddress().getPort());
            HttpRequest request = HttpRequest.newBuilder(catalogUri.resolve("/api/catalog/products")).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(503, response.statusCode());
            assertTrue(response.body().contains("ofbiz_catalog_unavailable"));
            assertFalse(response.body().contains("WG-1111"));
        } finally {
            if (liveCatalog != null) {
                liveCatalog.stop(0);
            }
            ofbiz.stop(0);
        }
    }

    private static String encodedRow(String... fields) {
        return java.util.Arrays.stream(fields)
                .map(value -> Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(value.getBytes(StandardCharsets.UTF_8)))
                .collect(java.util.stream.Collectors.joining("|")) + '\n';
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("X-Correlation-ID", "catalog-test")
                .GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
