package org.apache.ofbiz.modern.catalog.domain;

public record ProductSummary(
        String productId,
        String productTypeId,
        String internalName,
        String brandName,
        String productName,
        String description) {
}
