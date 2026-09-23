package org.apache.ofbiz.modern.catalog.adapter.in;

import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.ofbiz.modern.catalog.application.ProductCatalog;
import org.apache.ofbiz.modern.catalog.domain.ProductSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.apache.ofbiz.modern.catalog.config.SecurityConfiguration;

@WebFluxTest(CatalogController.class)
@Import(SecurityConfiguration.class)
class CatalogControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProductCatalog productCatalog;

    @Test
    void returnsBoundedCatalogResults() {
        when(productCatalog.search("giz", 100)).thenReturn(List.of(new ProductSummary("GZ-1000", "Gizmo", "Seed")));

        webTestClient.get()
                .uri("/api/catalog/v1/products?query=giz&limit=999")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectBody()
                .jsonPath("$.products[0].id").isEqualTo("GZ-1000");
    }
}
