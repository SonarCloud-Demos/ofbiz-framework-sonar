package org.apache.ofbiz.modern.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CatalogShadowStoreTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsAndRecoversValidatedSnapshot() throws Exception {
        OfbizCatalogSnapshot source = snapshot("LIVE-1", "Persisted product");
        Path snapshotPath = temporaryDirectory.resolve("nested/shadow.tsv");
        CatalogShadowStore store = new CatalogShadowStore(snapshotPath, CLOCK);

        store.save(source);
        OfbizCatalogSnapshot recovered = store.loadOr(snapshot("FALLBACK", "Fallback product"));

        assertEquals(source.items(), recovered.items());
        assertEquals(CLOCK.instant(), recovered.refreshedAt());
        assertFalse(Files.readString(snapshotPath).contains("Persisted product"));
    }

    @Test
    void usesFallbackWhenDurableSnapshotDoesNotExist() throws Exception {
        OfbizCatalogSnapshot fallback = snapshot("FALLBACK", "Fallback product");
        CatalogShadowStore store = new CatalogShadowStore(temporaryDirectory.resolve("missing.tsv"), CLOCK);

        assertEquals(fallback, store.loadOr(fallback));
    }

    @Test
    void reconciliationCountsChangesAdditionsAndRemovals() throws Exception {
        OfbizCatalogSnapshot shadow = OfbizCatalogSnapshot.parseRemote(List.of(
                row("UNCHANGED", "Same"), row("CHANGED", "Before"), row("REMOVED", "Removed")), CLOCK);
        OfbizCatalogSnapshot source = OfbizCatalogSnapshot.parseRemote(List.of(
                row("UNCHANGED", "Same"), row("CHANGED", "After"), row("ADDED", "Added")), CLOCK);

        assertEquals(3, shadow.mismatchesWith(source));
    }

    private static OfbizCatalogSnapshot snapshot(String id, String name) throws Exception {
        return OfbizCatalogSnapshot.parseRemote(List.of(row(id, name)), CLOCK);
    }

    private static String row(String id, String name) {
        return java.util.stream.Stream.of(id, name, "CATEGORY", "Category", "ACTIVE", "USD 1.00")
                .map(value -> Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .collect(java.util.stream.Collectors.joining("|"));
    }
}
