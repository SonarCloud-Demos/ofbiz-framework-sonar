package org.apache.ofbiz.modern.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogDeltaTest {
    @Test
    void producesDeterministicReplayableUpsertsAndDeletes() {
        List<CatalogItem> current = List.of(item("UNCHANGED", "Same"), item("CHANGED", "Before"),
                item("REMOVED", "Removed"));
        List<CatalogItem> source = List.of(item("UNCHANGED", "Same"), item("CHANGED", "After"),
                item("ADDED", "Added"));

        List<CatalogDelta> deltas = CatalogDelta.between(current, source);

        assertEquals(List.of("ADDED:UPSERT", "CHANGED:UPSERT", "REMOVED:DELETE"), deltas.stream()
                .map(delta -> delta.item().id() + ':' + delta.type()).toList());
        assertEquals(List.of(), CatalogDelta.between(source, source));
    }

    private static CatalogItem item(String id, String name) {
        return new CatalogItem(id, name, "CATEGORY", "Category", "ACTIVE", "USD 1.00");
    }
}
