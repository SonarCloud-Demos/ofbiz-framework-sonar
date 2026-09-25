package org.apache.ofbiz.modern.catalog;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OfbizCatalogClientTest {
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void readsStrictlyOrderedReplayableChangesAfterCursor() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/changes", exchange -> {
            assertEquals("after=40", exchange.getRequestURI().getQuery());
            assertEquals("sync-token", exchange.getRequestHeaders().getFirst("X-Modern-Catalog-Token"));
            String body = change(41, "UPSERT", "P-1", "Changed")
                    + change(42, "DELETE", "P-2", "");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        URI changesUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/changes");
        OfbizCatalogClient client = new OfbizCatalogClient(changesUri, changesUri, "sync-token",
                HttpClient.newHttpClient(), Clock.systemUTC());

        List<OfbizCatalogChange> changes = client.fetchChanges(40);

        assertEquals(List.of(41L, 42L), changes.stream().map(OfbizCatalogChange::sequence).toList());
        assertEquals(CatalogDelta.Type.UPSERT, changes.get(0).type());
        assertEquals("Changed", changes.get(0).item().name());
        assertEquals(CatalogDelta.Type.DELETE, changes.get(1).type());
    }

    @Test
    void rejectsOutOfOrderAndMalformedChanges() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/changes", exchange -> {
            byte[] bytes = change(4, "UNKNOWN", "P-1", "Bad").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        URI changesUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/changes");
        OfbizCatalogClient client = new OfbizCatalogClient(changesUri, changesUri, "sync-token",
                HttpClient.newHttpClient(), Clock.systemUTC());

        assertThrows(java.io.IOException.class, () -> client.fetchChanges(4));
    }

    private static String change(long sequence, String type, String id, String name) {
        return sequence + "|" + type + "|" + encoded(id) + '|' + encoded(name) + '|' + encoded("CATEGORY")
                + '|' + encoded("Category") + '|' + encoded("ACTIVE") + '|' + encoded("USD 1.00") + '\n';
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
