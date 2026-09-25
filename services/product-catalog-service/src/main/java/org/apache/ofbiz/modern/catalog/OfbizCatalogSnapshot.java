package org.apache.ofbiz.modern.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;

final class OfbizCatalogSnapshot {
    private static final String SNAPSHOT_RESOURCE = "/catalog/ofbiz-shadow.tsv";

    private final List<CatalogItem> items;
    private final Instant refreshedAt;

    private OfbizCatalogSnapshot(List<CatalogItem> items, Instant refreshedAt) {
        this.items = List.copyOf(items);
        this.refreshedAt = refreshedAt;
    }

    static OfbizCatalogSnapshot of(List<CatalogItem> items, Instant refreshedAt) throws IOException {
        if (items.isEmpty()) {
            throw new IOException("Catalog shadow snapshot is empty");
        }
        return new OfbizCatalogSnapshot(items, refreshedAt);
    }

    static OfbizCatalogSnapshot load(Clock clock) throws IOException {
        try (InputStream input = OfbizCatalogSnapshot.class.getResourceAsStream(SNAPSHOT_RESOURCE)) {
            if (input == null) {
                throw new IOException("Catalog shadow snapshot is missing");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                List<CatalogItem> loaded = reader.lines()
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .map(OfbizCatalogSnapshot::parse)
                        .toList();
                if (loaded.isEmpty()) {
                    throw new IOException("Catalog shadow snapshot is empty");
                }
                return new OfbizCatalogSnapshot(loaded, clock.instant());
            }
        }
    }

    static OfbizCatalogSnapshot parseRemote(List<String> lines, Clock clock) throws IOException {
        try {
            List<CatalogItem> loaded = lines.stream().filter(line -> !line.isBlank())
                    .map(OfbizCatalogSnapshot::parseEncoded).toList();
            if (loaded.isEmpty()) {
                throw new IOException("OFBiz catalog snapshot is empty");
            }
            return new OfbizCatalogSnapshot(loaded, clock.instant());
        } catch (IllegalArgumentException invalidSnapshot) {
            throw new IOException("OFBiz catalog snapshot is invalid", invalidSnapshot);
        }
    }

    static OfbizCatalogSnapshot parsePersisted(List<String> lines, Clock clock) throws IOException {
        return parseRemote(lines, clock);
    }

    List<String> persistedLines() {
        return items.stream().map(OfbizCatalogSnapshot::encodedLine).toList();
    }

    int mismatchesWith(OfbizCatalogSnapshot source) {
        Map<String, CatalogItem> shadowById = items.stream()
                .collect(Collectors.toMap(CatalogItem::id, Function.identity()));
        Map<String, CatalogItem> sourceById = source.items.stream()
                .collect(Collectors.toMap(CatalogItem::id, Function.identity()));
        long changedOrMissing = sourceById.entrySet().stream()
                .filter(entry -> !entry.getValue().equals(shadowById.get(entry.getKey())))
                .count();
        long removed = shadowById.keySet().stream().filter(id -> !sourceById.containsKey(id)).count();
        return Math.toIntExact(changedOrMissing + removed);
    }

    private static CatalogItem parse(String line) {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 6) {
            throw new IllegalArgumentException("Catalog snapshot row must contain six fields");
        }
        return new CatalogItem(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]);
    }

    private static CatalogItem parseEncoded(String line) {
        String[] fields = line.split("\\|", -1);
        if (fields.length != 6) {
            throw new IllegalArgumentException("Encoded catalog snapshot row must contain six fields");
        }
        return new CatalogItem(decoded(fields[0]), decoded(fields[1]), decoded(fields[2]), decoded(fields[3]),
                decoded(fields[4]), decoded(fields[5]));
    }

    private static String decoded(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String encodedLine(CatalogItem item) {
        return encoded(item.id()) + '|' + encoded(item.name()) + '|' + encoded(item.categoryId()) + '|'
                + encoded(item.categoryName()) + '|' + encoded(item.status()) + '|'
                + encoded(item.displayedPriceReference());
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    List<CatalogItem> items() {
        return items;
    }

    Instant refreshedAt() {
        return refreshedAt;
    }
}
