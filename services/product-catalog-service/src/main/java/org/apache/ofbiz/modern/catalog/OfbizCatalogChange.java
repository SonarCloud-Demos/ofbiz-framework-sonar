package org.apache.ofbiz.modern.catalog;

record OfbizCatalogChange(long sequence, CatalogDelta.Type type, CatalogItem item) {
}
