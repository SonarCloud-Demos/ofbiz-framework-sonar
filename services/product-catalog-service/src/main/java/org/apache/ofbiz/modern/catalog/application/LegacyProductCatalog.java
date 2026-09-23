package org.apache.ofbiz.modern.catalog.application;

import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSearch;

public interface LegacyProductCatalog {
    ProductPage search(ProductSearch search);
}
