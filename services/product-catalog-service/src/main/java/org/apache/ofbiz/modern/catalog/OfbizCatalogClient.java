package org.apache.ofbiz.modern.catalog;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

final class OfbizCatalogClient {
    private static final String SYNC_TOKEN_HEADER = "X-Modern-Catalog-Token";
    private static final String LOCAL_OFBIZ_HOST = "localhost";
    private final URI snapshotUri;
    private final URI changesUri;
    private final String token;
    private final HttpClient client;
    private final Clock clock;

    OfbizCatalogClient(URI snapshotUri, String token, HttpClient client, Clock clock) {
        this(snapshotUri, null, token, client, clock);
    }

    OfbizCatalogClient(URI snapshotUri, URI changesUri, String token, HttpClient client, Clock clock) {
        this.snapshotUri = snapshotUri;
        this.changesUri = changesUri;
        this.token = token;
        this.client = client;
        this.clock = clock;
    }

    boolean supportsChanges() {
        return changesUri != null;
    }

    List<OfbizCatalogChange> fetchChanges(long after) throws IOException, InterruptedException {
        if (changesUri == null) {
            return List.of();
        }
        URI requestUri = URI.create(changesUri + "?after=" + after);
        HttpRequest request = authenticatedRequest(requestUri);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("OFBiz catalog changes returned status " + response.statusCode());
        }
        List<OfbizCatalogChange> changes = new ArrayList<>();
        long previous = after;
        for (String line : response.body().lines().toList()) {
            String[] fields = line.split("\\|", -1);
            if (fields.length != 8) {
                throw new IOException("Invalid OFBiz catalog change field count");
            }
            long sequence = parseSequence(fields[0], previous);
            CatalogDelta.Type type;
            try {
                type = CatalogDelta.Type.valueOf(fields[1]);
            } catch (IllegalArgumentException invalidType) {
                throw new IOException("Invalid OFBiz catalog change type", invalidType);
            }
            String productId = decoded(fields[2]);
            CatalogItem item = type == CatalogDelta.Type.DELETE
                    ? new CatalogItem(productId, productId, "DELETED", "Deleted", "INACTIVE", "Not configured")
                    : new CatalogItem(productId, decoded(fields[3]), decoded(fields[4]), decoded(fields[5]),
                            decoded(fields[6]), decoded(fields[7]));
            changes.add(new OfbizCatalogChange(sequence, type, item));
            previous = sequence;
        }
        return List.copyOf(changes);
    }

    OfbizCatalogSnapshot fetch() throws IOException, InterruptedException {
        HttpRequest request = authenticatedRequest(snapshotUri);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("OFBiz catalog snapshot returned status " + response.statusCode());
        }
        return OfbizCatalogSnapshot.parseRemote(response.body().lines().toList(), clock);
    }

    private HttpRequest authenticatedRequest(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
                .header(SYNC_TOKEN_HEADER, token)
                .header("Host", LOCAL_OFBIZ_HOST)
                .GET().build();
    }

    private static long parseSequence(String value, long previous) throws IOException {
        try {
            long sequence = Long.parseLong(value);
            if (sequence <= previous) {
                throw new IOException("OFBiz catalog changes are not strictly ordered");
            }
            return sequence;
        } catch (NumberFormatException invalidSequence) {
            throw new IOException("Invalid OFBiz catalog change sequence", invalidSequence);
        }
    }

    private static String decoded(String value) throws IOException {
        try {
            return new String(Base64.getUrlDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException invalidEncoding) {
            throw new IOException("Invalid OFBiz catalog change encoding", invalidEncoding);
        }
    }
}
