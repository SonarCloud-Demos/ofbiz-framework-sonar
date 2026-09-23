package org.apache.ofbiz.modern.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProductSearchTest {
    @Test
    void mapsContractSortFieldsToAllowlistedColumns() {
        assertEquals("product_name", ProductSearch.SortField.fromContract("productName").columnName());
        assertEquals(ProductSearch.Direction.DESC, ProductSearch.Direction.fromContract("desc"));
    }

    @Test
    void rejectsUnknownSortFieldsAndDirections() {
        assertThrows(IllegalArgumentException.class, () -> ProductSearch.SortField.fromContract("createdStamp"));
        assertThrows(IllegalArgumentException.class, () -> ProductSearch.Direction.fromContract("sideways"));
    }
}
