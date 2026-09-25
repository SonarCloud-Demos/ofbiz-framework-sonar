package org.apache.ofbiz.modern.bff;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceBffApplicationTest {
    private HttpServer reference;
    private HttpServer bff;
    private URI baseUri;

    @BeforeEach
    void startServers() throws Exception {
        reference = HttpServer.create(new InetSocketAddress(0), 0);
        reference.createContext("/api/reference", exchange -> {
            String correlationId = exchange.getRequestHeaders().getFirst("X-Correlation-ID");
            String traceParent = exchange.getRequestHeaders().getFirst("traceparent");
            byte[] body = ("{\"correlationId\":\"" + correlationId + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("traceparent", traceParent);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        reference.start();
        URI referenceUri = URI.create("http://localhost:" + reference.getAddress().getPort());
        bff = new ExperienceBffApplication(referenceUri, referenceUri, HttpClient.newHttpClient(),
                "test-user", "test-password")
                .createServer(0);
        bff.start();
        baseUri = URI.create("http://localhost:" + bff.getAddress().getPort());
    }

    @AfterEach
    void stopServers() {
        bff.stop(0);
        reference.stop(0);
    }

    @Test
    void servesTrustedModernShell() throws Exception {
        HttpResponse<String> response = get("/", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("data-runtime=\"modern\""));
        assertTrue(response.body().contains("Modern experience"));
        assertTrue(response.headers().firstValue("Content-Security-Policy").orElseThrow()
                .contains("default-src 'self'"));
    }

    @Test
    void servesStaticAssetsWithExplicitTypes() throws Exception {
        assertTrue(get("/app.css", null).headers().firstValue("Content-Type").orElseThrow()
                .startsWith("text/css"));
        assertTrue(get("/app.js", null).headers().firstValue("Content-Type").orElseThrow()
                .startsWith("text/javascript"));
    }

    @Test
    void forwardsCorrelationIdToReferenceService() throws Exception {
        HttpResponse<String> response = get("/api/reference", "request-42");

        assertEquals(200, response.statusCode());
        assertEquals("request-42", response.headers().firstValue("X-Correlation-ID").orElseThrow());
        assertTrue(response.body().contains("request-42"));
        assertTrue(response.headers().firstValue("traceparent").orElseThrow()
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-01"));
    }

    @Test
    void replacesInvalidCorrelationId() throws Exception {
        HttpResponse<String> response = get("/api/reference", "invalid value");

        assertEquals(200, response.statusCode());
        assertFalse(response.body().contains("invalid value"));
    }

    @Test
    void returnsNotFoundForUnknownRoutesAndMethods() throws Exception {
        assertEquals(404, get("/unknown", null).statusCode());
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/reference"))
                .header("Authorization", authorization())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        assertEquals(405, HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void reportsUnavailableReferenceService() throws Exception {
        reference.stop(0);

        assertEquals(503, get("/api/reference", "request-99").statusCode());
    }

    @Test
    void reportsLegacyRuntimeReachabilityWithoutExposingItsAddress() throws Exception {
        HttpResponse<String> response = get("/api/legacy/health", "legacy-check");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"runtime\":\"legacy\""));
        assertFalse(response.body().contains("localhost"));
    }

    @Test
    void publishesFailClosedRouteManifestAndAuthenticatedProfile() throws Exception {
        HttpResponse<String> manifest = get("/route-manifest.json", null);
        HttpResponse<String> profile = get("/modern/profile", null);

        assertEquals(200, manifest.statusCode());
        assertTrue(manifest.body().contains("\"default\":\"legacy\""));
        assertTrue(manifest.body().contains("\"path\":\"/modern/profile\",\"runtime\":\"modern\""));
        assertEquals(200, profile.statusCode());
        assertTrue(profile.body().contains("\"subject\":\"test-user\""));
        assertFalse(profile.body().contains("test-password"));
    }

    @Test
    void switchesModernProfileBackToLegacyWithoutRedeployment() throws Exception {
        HttpServer failback = new ExperienceBffApplication(baseUri, baseUri, HttpClient.newHttpClient(),
                "test-user", "test-password", false).createServer(0);
        failback.start();
        try {
            URI failbackUri = URI.create("http://localhost:" + failback.getAddress().getPort());
            HttpRequest request = HttpRequest.newBuilder(failbackUri.resolve("/modern/profile"))
                    .header("Authorization", authorization())
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(307, response.statusCode());
            assertEquals("/webtools/control/main", response.headers().firstValue("Location").orElseThrow());
        } finally {
            failback.stop(0);
        }
    }

    @Test
    void createsAndRevokesLegacySessionWithCsrfProtection() throws Exception {
        HttpResponse<String> created = request("/legacy-session", "POST", null, null);
        String cookie = created.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
        String csrfToken = created.body().replaceFirst(".*\\\"csrfToken\\\":\\\"([^\\\"]+).*", "$1");

        assertEquals(201, created.statusCode());
        assertTrue(created.headers().firstValue("Set-Cookie").orElseThrow().contains("SameSite=Strict"));
        assertEquals(403, request("/legacy-session", "DELETE", cookie, "wrong-token").statusCode());
        assertEquals(204, request("/legacy-session", "DELETE", cookie, csrfToken).statusCode());
        assertEquals(403, request("/legacy-session", "DELETE", cookie, csrfToken).statusCode());
    }

    @Test
    void rejectsIdentityValuesThatCannotBeSafelySerialized() {
        assertThrows(IllegalArgumentException.class, () -> new ExperienceBffApplication(
                baseUri, baseUri, HttpClient.newHttpClient(), "unsafe\"identity", "test-password"));
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.headers().firstValue("WWW-Authenticate").orElseThrow()
                .startsWith("Basic realm="));
    }

    private HttpResponse<String> get(String path, String correlationId) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path)).GET();
        request.header("Authorization", authorization());
        if (correlationId != null) {
            request.header("X-Correlation-ID", correlationId);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> request(String path, String method, String cookie, String csrfToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Authorization", authorization())
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        if (csrfToken != null) {
            request.header("X-CSRF-Token", csrfToken);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String authorization() {
        return "Basic " + Base64.getEncoder().encodeToString(
                "test-user:test-password".getBytes(StandardCharsets.UTF_8));
    }
}
