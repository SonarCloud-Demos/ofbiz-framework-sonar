package org.apache.ofbiz.modern.healthcheck;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpHealthProbe {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private HttpHealthProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one health URL");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(arguments[0]))
                .timeout(TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || !response.body().contains("\"status\":\"UP\"")) {
            throw new IllegalStateException("Service health check failed with HTTP " + response.statusCode());
        }
    }
}
