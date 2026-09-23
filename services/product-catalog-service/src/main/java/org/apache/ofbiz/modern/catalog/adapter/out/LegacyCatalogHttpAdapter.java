package org.apache.ofbiz.modern.catalog.adapter.out;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.ofbiz.modern.catalog.application.LegacyProductCatalog;
import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSearch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@ConditionalOnProperty(name = "catalog.legacy.enabled", havingValue = "true")
public class LegacyCatalogHttpAdapter implements LegacyProductCatalog {
    private final WebClient webClient;
    private final Duration timeout;

    public LegacyCatalogHttpAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${catalog.legacy.base-url}") URI baseUrl,
            @Value("${catalog.legacy.token-file}") Path tokenFile,
            @Value("${catalog.legacy.timeout:PT5S}") Duration timeout,
            @Value("${catalog.security.mode}") String securityMode) throws IOException {
        validateBaseUrl(baseUrl, securityMode);
        String token = Files.readString(tokenFile, StandardCharsets.UTF_8).strip();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Legacy catalog token file is empty");
        }
        this.webClient = webClientBuilder
                .baseUrl(baseUrl.toString())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        this.timeout = timeout;
    }

    @Override
    public ProductPage search(ProductSearch search) {
        LegacyCatalogResponse response = webClient.get()
                .uri(uri -> uri.path("/products")
                        .queryParam("productId", search.productId())
                        .queryParam("internalName", search.internalName())
                        .queryParam("sort", search.sort().contractName())
                        .queryParam("direction", search.direction().name().toLowerCase(java.util.Locale.ROOT))
                        .queryParam("page", search.page())
                        .queryParam("size", search.size())
                .build())
                .retrieve()
                .bodyToMono(LegacyCatalogResponse.class)
                .block(timeout);
        if (response == null || response.data() == null) {
            throw new IllegalStateException("Legacy catalog returned an empty response");
        }
        return response.data();
    }

    private void validateBaseUrl(URI baseUrl, String securityMode) {
        boolean localHttp = "local".equals(securityMode) && "http".equalsIgnoreCase(baseUrl.getScheme());
        if (!"https".equalsIgnoreCase(baseUrl.getScheme()) && !localHttp) {
            throw new IllegalArgumentException("Legacy catalog URL must use HTTPS outside local mode");
        }
        if (baseUrl.getUserInfo() != null || baseUrl.getHost() == null) {
            throw new IllegalArgumentException("Legacy catalog URL must not contain credentials and must have a host");
        }
    }

    private record LegacyCatalogResponse(ProductPage data) {
    }
}
