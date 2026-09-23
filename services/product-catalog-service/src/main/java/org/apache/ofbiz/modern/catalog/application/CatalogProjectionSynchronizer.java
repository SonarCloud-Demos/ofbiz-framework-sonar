package org.apache.ofbiz.modern.catalog.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSearch;
import org.apache.ofbiz.modern.catalog.domain.ProductSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CatalogProjectionSynchronizer {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogProjectionSynchronizer.class);
    private static final int SYNC_PAGE_SIZE = 100;

    private final LegacyProductCatalog legacyCatalog;
    private final ProductProjectionWriter projectionWriter;
    private final boolean enabled;
    private final Counter successes;
    private final Counter failures;

    public CatalogProjectionSynchronizer(
            ObjectProvider<LegacyProductCatalog> legacyCatalog,
            ProductProjectionWriter projectionWriter,
            @Value("${catalog.legacy.sync-enabled:false}") boolean enabled,
            MeterRegistry meterRegistry) {
        this.legacyCatalog = legacyCatalog.getIfAvailable();
        this.projectionWriter = projectionWriter;
        this.enabled = enabled;
        this.successes = meterRegistry.counter("catalog.projection.sync", "result", "success");
        this.failures = meterRegistry.counter("catalog.projection.sync", "result", "failure");
    }

    @Scheduled(fixedDelayString = "${catalog.legacy.sync-interval:PT1M}")
    public void synchronize() {
        if (!enabled || legacyCatalog == null) {
            return;
        }
        try {
            ProductPage firstPage = legacyCatalog.search(syncSearch(0));
            List<ProductSummary> products = new ArrayList<>(firstPage.items());
            for (int page = 1; products.size() < firstPage.total(); page++) {
                ProductPage nextPage = legacyCatalog.search(syncSearch(page));
                products.addAll(nextPage.items());
                if (nextPage.items().isEmpty()) {
                    throw new IllegalStateException("Legacy catalog ended before its declared total");
                }
            }
            projectionWriter.replaceSnapshot(products, firstPage.total(), Instant.now());
            successes.increment();
            LOG.info("Catalog projection synchronization completed with {} products", products.size());
        } catch (RuntimeException exception) {
            failures.increment();
            LOG.error("Catalog projection synchronization failed; the previous projection remains authoritative", exception);
        }
    }

    private ProductSearch syncSearch(int page) {
        return new ProductSearch(
                "", "", ProductSearch.SortField.PRODUCT_ID, ProductSearch.Direction.ASC, page, SYNC_PAGE_SIZE);
    }
}
