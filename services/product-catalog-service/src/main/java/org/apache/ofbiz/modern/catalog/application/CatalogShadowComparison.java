package org.apache.ofbiz.modern.catalog.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CatalogShadowComparison {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogShadowComparison.class);
    private static final String METRIC_NAME = "catalog.shadow.comparisons";
    private static final String RESULT_TAG = "result";

    private final LegacyProductCatalog legacyCatalog;
    private final boolean enabled;
    private final Counter matches;
    private final Counter mismatches;
    private final Counter errors;

    public CatalogShadowComparison(
            ObjectProvider<LegacyProductCatalog> legacyCatalog,
            @Value("${catalog.legacy.shadow-enabled:false}") boolean enabled,
            MeterRegistry meterRegistry) {
        this.legacyCatalog = legacyCatalog.getIfAvailable();
        this.enabled = enabled;
        this.matches = meterRegistry.counter(METRIC_NAME, RESULT_TAG, "match");
        this.mismatches = meterRegistry.counter(METRIC_NAME, RESULT_TAG, "mismatch");
        this.errors = meterRegistry.counter(METRIC_NAME, RESULT_TAG, "error");
    }

    @Async("catalogShadowExecutor")
    public void compare(ProductSearch search, ProductPage modernPage) {
        if (!enabled || legacyCatalog == null) {
            return;
        }
        try {
            ProductPage legacyPage = legacyCatalog.search(search);
            if (sameResult(modernPage, legacyPage)) {
                matches.increment();
            } else {
                mismatches.increment();
                LOG.warn("Catalog shadow mismatch for page {} and size {}; product data omitted",
                        search.page(), search.size());
            }
        } catch (RuntimeException exception) {
            errors.increment();
            LOG.warn("Catalog shadow comparison failed; user response was not affected", exception);
        }
    }

    static boolean sameResult(ProductPage modernPage, ProductPage legacyPage) {
        return modernPage.page() == legacyPage.page()
                && modernPage.size() == legacyPage.size()
                && modernPage.total() == legacyPage.total()
                && modernPage.items().equals(legacyPage.items());
    }
}
