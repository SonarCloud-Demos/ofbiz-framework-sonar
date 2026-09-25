package org.apache.ofbiz.modern.catalog;

record CatalogItem(String id, String name, String categoryId, String categoryName, String status,
        String displayedPriceReference) {
    CatalogItem {
        required(id, "id");
        required(name, "name");
        required(categoryId, "categoryId");
        required(categoryName, "categoryName");
        required(status, "status");
        required(displayedPriceReference, "displayedPriceReference");
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
