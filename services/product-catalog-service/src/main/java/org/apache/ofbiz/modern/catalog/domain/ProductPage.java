package org.apache.ofbiz.modern.catalog.domain;

import java.time.Instant;
import java.util.List;

public record ProductPage(
        List<ProductSummary> items,
        int page,
        int size,
        long total,
        Instant projectionTimestamp) {
}
