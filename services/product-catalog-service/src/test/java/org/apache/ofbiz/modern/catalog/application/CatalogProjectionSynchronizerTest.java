package org.apache.ofbiz.modern.catalog.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSearch;
import org.apache.ofbiz.modern.catalog.domain.ProductSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.ObjectProvider;

class CatalogProjectionSynchronizerTest {
    @Test
    void replacesProjectionOnlyAfterReadingCompleteSnapshot() {
        LegacyProductCatalog legacyCatalog = mock(LegacyProductCatalog.class);
        ProductProjectionWriter writer = mock(ProductProjectionWriter.class);
        ObjectProvider<LegacyProductCatalog> provider = provider(legacyCatalog);
        ProductSummary product = new ProductSummary(
                "GZ-1000", "FINISHED_GOOD", "Gizmo", null, "Gizmo", "Seed");
        ProductPage page = new ProductPage(List.of(product), 0, 100, 1, Instant.EPOCH);
        when(legacyCatalog.search(ArgumentMatchers.any(ProductSearch.class))).thenReturn(page);
        CatalogProjectionSynchronizer synchronizer = new CatalogProjectionSynchronizer(
                provider, writer, true, new SimpleMeterRegistry());

        synchronizer.synchronize();

        verify(writer).replaceSnapshot(
                ArgumentMatchers.eq(List.of(product)), ArgumentMatchers.eq(1L), ArgumentMatchers.any(Instant.class));
    }

    @Test
    void preservesProjectionWhenLegacySnapshotIsIncomplete() {
        LegacyProductCatalog legacyCatalog = mock(LegacyProductCatalog.class);
        ProductProjectionWriter writer = mock(ProductProjectionWriter.class);
        ObjectProvider<LegacyProductCatalog> provider = provider(legacyCatalog);
        ProductPage incomplete = new ProductPage(List.of(), 0, 100, 1, Instant.EPOCH);
        when(legacyCatalog.search(ArgumentMatchers.any(ProductSearch.class))).thenReturn(incomplete);
        CatalogProjectionSynchronizer synchronizer = new CatalogProjectionSynchronizer(
                provider, writer, true, new SimpleMeterRegistry());

        synchronizer.synchronize();

        verify(writer, never()).replaceSnapshot(
                ArgumentMatchers.anyList(), ArgumentMatchers.anyLong(), ArgumentMatchers.any(Instant.class));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<LegacyProductCatalog> provider(LegacyProductCatalog legacyCatalog) {
        ObjectProvider<LegacyProductCatalog> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(legacyCatalog);
        return provider;
    }
}
