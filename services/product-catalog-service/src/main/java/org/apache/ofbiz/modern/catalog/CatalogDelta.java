package org.apache.ofbiz.modern.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

record CatalogDelta(Type type, CatalogItem item) {
    enum Type { UPSERT, DELETE }

    static List<CatalogDelta> between(List<CatalogItem> current, List<CatalogItem> source) {
        Map<String, CatalogItem> currentById = byId(current);
        Map<String, CatalogItem> sourceById = byId(source);
        List<CatalogDelta> changes = new ArrayList<>();
        sourceById.values().stream()
                .filter(item -> !item.equals(currentById.get(item.id())))
                .map(item -> new CatalogDelta(Type.UPSERT, item))
                .forEach(changes::add);
        currentById.values().stream()
                .filter(item -> !sourceById.containsKey(item.id()))
                .map(item -> new CatalogDelta(Type.DELETE, item))
                .forEach(changes::add);
        changes.sort(Comparator.comparing(delta -> delta.item().id()));
        return List.copyOf(changes);
    }

    private static Map<String, CatalogItem> byId(List<CatalogItem> items) {
        return items.stream().collect(Collectors.toMap(CatalogItem::id, Function.identity()));
    }
}
