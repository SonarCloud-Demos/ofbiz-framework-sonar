package org.apache.ofbiz.modern.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;

final class CatalogShadowStore implements CatalogStore {
    private final Path snapshotPath;
    private final Clock clock;

    CatalogShadowStore(Path snapshotPath, Clock clock) {
        this.snapshotPath = snapshotPath.toAbsolutePath().normalize();
        this.clock = clock;
    }

    @Override
    public OfbizCatalogSnapshot loadOr(OfbizCatalogSnapshot fallback) throws IOException {
        if (!Files.isRegularFile(snapshotPath)) {
            return fallback;
        }
        return OfbizCatalogSnapshot.parsePersisted(Files.readAllLines(snapshotPath, StandardCharsets.UTF_8), clock);
    }

    @Override
    public void save(OfbizCatalogSnapshot snapshot) throws IOException {
        Path parent = snapshotPath.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "catalog-shadow-", ".tmp");
        boolean moved = false;
        try {
            Files.write(temporary, snapshot.persistedLines(), StandardCharsets.UTF_8);
            moveAtomically(temporary);
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private void moveAtomically(Path temporary) throws IOException {
        try {
            Files.move(temporary, snapshotPath, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, snapshotPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
