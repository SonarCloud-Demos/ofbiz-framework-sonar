package org.apache.ofbiz.modern.catalog.config;

import org.apache.ofbiz.modern.catalog.adapter.in.CatalogController;
import org.apache.ofbiz.modern.catalog.application.ProductCatalog;
import org.apache.ofbiz.modern.catalog.application.CatalogShadowComparison;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(value = CatalogController.class, properties = "catalog.security.mode=entra")
@Import(SecurityConfiguration.class)
class EntraSecurityConfigurationTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProductCatalog productCatalog;

    @MockitoBean
    private CatalogShadowComparison shadowComparison;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void rejectsUnauthenticatedCatalogRequests() {
        webTestClient.get()
                .uri("/api/catalog/v1/products")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
