package org.apache.ofbiz.modern.catalog;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

interface CatalogStore {
    OfbizCatalogSnapshot loadOr(OfbizCatalogSnapshot fallback) throws IOException;

    void save(OfbizCatalogSnapshot snapshot) throws IOException;

    default long sourceCursor() throws IOException {
        return -1;
    }

    default void bootstrapSource(OfbizCatalogSnapshot snapshot) throws IOException {
        save(snapshot);
    }

    default void applySourceChanges(List<OfbizCatalogChange> changes, Instant refreshedAt) throws IOException {
        throw new IOException("Replayable source changes require the database catalog store");
    }
}
