package org.apache.ofbiz.modern.catalog.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSummary;
import org.junit.jupiter.api.Test;

class CatalogShadowComparisonTest {
    private static final ProductSummary PRODUCT = new ProductSummary(
            "GZ-1000", "FINISHED_GOOD", "Gizmo", null, "Gizmo", "Seed");

    @Test
    void ignoresProjectionTimestampWhenComparingResults() {
        ProductPage modern = new ProductPage(List.of(PRODUCT), 0, 20, 1, Instant.parse("2026-09-23T10:00:00Z"));
        ProductPage legacy = new ProductPage(List.of(PRODUCT), 0, 20, 1, Instant.parse("2026-09-23T10:01:00Z"));

        assertTrue(CatalogShadowComparison.sameResult(modern, legacy));
    }

    @Test
    void detectsFieldAndCountDifferences() {
        ProductPage modern = new ProductPage(List.of(PRODUCT), 0, 20, 1, Instant.EPOCH);
        ProductPage legacy = new ProductPage(List.of(), 0, 20, 0, Instant.EPOCH);

        assertFalse(CatalogShadowComparison.sameResult(modern, legacy));
    }
}
