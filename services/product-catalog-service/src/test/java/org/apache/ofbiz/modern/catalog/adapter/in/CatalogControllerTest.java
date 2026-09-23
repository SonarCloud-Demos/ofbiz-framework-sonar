package org.apache.ofbiz.modern.catalog.adapter.in;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.apache.ofbiz.modern.catalog.application.ProductCatalog;
import org.apache.ofbiz.modern.catalog.application.CatalogShadowComparison;
import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSearch;
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

    @MockitoBean
    private CatalogShadowComparison shadowComparison;

    @Test
    void returnsCanonicalCatalogPage() {
        ProductSearch search = new ProductSearch(
                "GZ-1000", "Giz", ProductSearch.SortField.PRODUCT_ID, ProductSearch.Direction.ASC, 0, 100);
        ProductSummary product = new ProductSummary(
                "GZ-1000", "FINISHED_GOOD", "Gizmo", null, "Gizmo", "Seed");
        when(productCatalog.search(search)).thenReturn(new ProductPage(
                List.of(product), 0, 100, 1, Instant.parse("2026-09-23T12:00:00Z")));

        webTestClient.get()
                .uri("/api/catalog/v1/products?productId=GZ-1000&internalName=Giz&size=100")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectBody()
                .jsonPath("$.items[0].productId").isEqualTo("GZ-1000")
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.projectionTimestamp").isEqualTo("2026-09-23T12:00:00Z");
    }

    @Test
    void rejectsUnsupportedSortField() {
        webTestClient.get()
                .uri("/api/catalog/v1/products?sort=createdStamp")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsOversizedPage() {
        webTestClient.get()
                .uri("/api/catalog/v1/products?size=101")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
