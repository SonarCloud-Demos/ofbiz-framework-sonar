package org.apache.ofbiz.modern.catalog.application;

import java.time.Instant;
import java.util.List;

import org.apache.ofbiz.modern.catalog.domain.ProductSummary;

public interface ProductProjectionWriter {
    void replaceSnapshot(List<ProductSummary> products, long expectedTotal, Instant extractedAt);
}
