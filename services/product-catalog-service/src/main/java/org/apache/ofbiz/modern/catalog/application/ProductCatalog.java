package org.apache.ofbiz.modern.catalog.application;

import java.util.List;

import org.apache.ofbiz.modern.catalog.domain.ProductSummary;

public interface ProductCatalog {
    List<ProductSummary> search(String query, int limit);
}
