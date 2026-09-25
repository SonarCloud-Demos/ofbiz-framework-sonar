package org.apache.ofbiz.modern.catalog;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

final class PostgresCatalogStore implements CatalogStore {
    private static final String SELECT_PRODUCTS = "SELECT product_id, product_name, category_id, category_name, "
            + "status, displayed_price_reference FROM catalog_product ORDER BY product_id";
    private static final String UPSERT_PRODUCT = "INSERT INTO catalog_product(product_id, product_name, category_id, "
            + "category_name, status, displayed_price_reference) VALUES (?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT (product_id) DO UPDATE SET product_name=EXCLUDED.product_name, "
            + "category_id=EXCLUDED.category_id, category_name=EXCLUDED.category_name, status=EXCLUDED.status, "
            + "displayed_price_reference=EXCLUDED.displayed_price_reference";
    private static final String INSERT_CHANGE = "INSERT INTO catalog_change(event_type, product_id, product_name, "
            + "category_id, category_name, status, displayed_price_reference) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final Clock clock;

    PostgresCatalogStore(String jdbcUrl, String username, String password, Clock clock) throws IOException {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.clock = clock;
        initialize();
    }

    @Override
    public OfbizCatalogSnapshot loadOr(OfbizCatalogSnapshot fallback) throws IOException {
        try (Connection connection = connection();
                PreparedStatement statement = connection.prepareStatement(SELECT_PRODUCTS);
                ResultSet rows = statement.executeQuery()) {
            List<CatalogItem> items = readItems(rows);
            return items.isEmpty() ? fallback : OfbizCatalogSnapshot.of(items, lastRefresh(connection));
        } catch (SQLException databaseFailure) {
            throw failure("Unable to load the catalog shadow", databaseFailure);
        }
    }

    @Override
    public void save(OfbizCatalogSnapshot source) throws IOException {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                List<CatalogItem> current = currentItems(connection);
                for (CatalogDelta delta : CatalogDelta.between(current, source.items())) {
                    appendAndApply(connection, delta);
                }
                updateCheckpoint(connection, source.refreshedAt());
                connection.commit();
            } catch (SQLException databaseFailure) {
                rollback(connection, databaseFailure);
                throw databaseFailure;
            }
        } catch (SQLException databaseFailure) {
            throw failure("Unable to persist catalog deltas", databaseFailure);
        }
    }

    private void initialize() throws IOException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS catalog_product (product_id VARCHAR(255) PRIMARY KEY, "
                    + "product_name TEXT NOT NULL, category_id VARCHAR(255) NOT NULL, category_name TEXT NOT NULL, "
                    + "status VARCHAR(32) NOT NULL, displayed_price_reference TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS catalog_change (sequence_id BIGSERIAL PRIMARY KEY, "
                    + "event_type VARCHAR(16) NOT NULL, product_id VARCHAR(255) NOT NULL, product_name TEXT, "
                    + "category_id VARCHAR(255), category_name TEXT, status VARCHAR(32), "
                    + "displayed_price_reference TEXT, occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS catalog_sync_state (singleton BOOLEAN PRIMARY KEY DEFAULT TRUE, "
                    + "refreshed_at TIMESTAMPTZ NOT NULL, source_cursor BIGINT NOT NULL DEFAULT 0, CHECK (singleton))");
            statement.execute("ALTER TABLE catalog_sync_state ADD COLUMN IF NOT EXISTS "
                    + "source_cursor BIGINT NOT NULL DEFAULT 0");
            statement.execute("ALTER TABLE catalog_sync_state ADD COLUMN IF NOT EXISTS "
                    + "source_initialized BOOLEAN NOT NULL DEFAULT FALSE");
        } catch (SQLException databaseFailure) {
            throw failure("Unable to initialize the catalog database", databaseFailure);
        }
    }

    @Override
    public long sourceCursor() throws IOException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT source_cursor, source_initialized FROM catalog_sync_state WHERE singleton=TRUE");
                ResultSet row = statement.executeQuery()) {
            return row.next() && row.getBoolean(2) ? row.getLong(1) : -1;
        } catch (SQLException databaseFailure) {
            throw failure("Unable to load the catalog source cursor", databaseFailure);
        }
    }

    @Override
    public void bootstrapSource(OfbizCatalogSnapshot snapshot) throws IOException {
        save(snapshot);
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE catalog_sync_state SET source_cursor=0, source_initialized=TRUE WHERE singleton=TRUE")) {
            statement.executeUpdate();
        } catch (SQLException databaseFailure) {
            throw failure("Unable to initialize the catalog source cursor", databaseFailure);
        }
    }

    @Override
    public void applySourceChanges(List<OfbizCatalogChange> changes, Instant refreshedAt) throws IOException {
        if (changes.isEmpty()) {
            return;
        }
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                long cursor = sourceCursor(connection);
                for (OfbizCatalogChange change : changes) {
                    if (change.sequence() > cursor) {
                        appendAndApply(connection, new CatalogDelta(change.type(), change.item()));
                        cursor = change.sequence();
                    }
                }
                updateSourceCheckpoint(connection, refreshedAt, cursor);
                connection.commit();
            } catch (SQLException databaseFailure) {
                rollback(connection, databaseFailure);
                throw databaseFailure;
            }
        } catch (SQLException databaseFailure) {
            throw failure("Unable to apply catalog source changes", databaseFailure);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static List<CatalogItem> currentItems(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_PRODUCTS);
                ResultSet rows = statement.executeQuery()) {
            return readItems(rows);
        }
    }

    private static List<CatalogItem> readItems(ResultSet rows) throws SQLException {
        List<CatalogItem> items = new ArrayList<>();
        while (rows.next()) {
            items.add(new CatalogItem(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4),
                    rows.getString(5), rows.getString(6)));
        }
        return List.copyOf(items);
    }

    private static void appendAndApply(Connection connection, CatalogDelta delta) throws SQLException {
        append(connection, delta);
        if (delta.type() == CatalogDelta.Type.DELETE) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM catalog_product WHERE product_id = ?")) {
                statement.setString(1, delta.item().id());
                statement.executeUpdate();
            }
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_PRODUCT)) {
            setItem(statement, 1, delta.item());
            statement.executeUpdate();
        }
    }

    private static void append(Connection connection, CatalogDelta delta) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_CHANGE)) {
            statement.setString(1, delta.type().name());
            setItem(statement, 2, delta.item());
            statement.executeUpdate();
        }
    }

    private static void setItem(PreparedStatement statement, int offset, CatalogItem item) throws SQLException {
        statement.setString(offset, item.id());
        statement.setString(offset + 1, item.name());
        statement.setString(offset + 2, item.categoryId());
        statement.setString(offset + 3, item.categoryName());
        statement.setString(offset + 4, item.status());
        statement.setString(offset + 5, item.displayedPriceReference());
    }

    private static void updateCheckpoint(Connection connection, Instant refreshedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO catalog_sync_state(singleton, refreshed_at) VALUES (TRUE, ?) "
                        + "ON CONFLICT (singleton) DO UPDATE SET refreshed_at=EXCLUDED.refreshed_at")) {
            statement.setObject(1, OffsetDateTime.ofInstant(refreshedAt, ZoneOffset.UTC));
            statement.executeUpdate();
        }
    }

    private static void updateSourceCheckpoint(Connection connection, Instant refreshedAt, long cursor)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO catalog_sync_state(singleton, refreshed_at, source_cursor, source_initialized) "
                        + "VALUES (TRUE, ?, ?, TRUE) "
                        + "ON CONFLICT (singleton) DO UPDATE SET refreshed_at=EXCLUDED.refreshed_at, "
                        + "source_cursor=EXCLUDED.source_cursor, source_initialized=TRUE")) {
            statement.setObject(1, OffsetDateTime.ofInstant(refreshedAt, ZoneOffset.UTC));
            statement.setLong(2, cursor);
            statement.executeUpdate();
        }
    }

    private static long sourceCursor(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT source_cursor, source_initialized FROM catalog_sync_state WHERE singleton=TRUE");
                ResultSet row = statement.executeQuery()) {
            return row.next() && row.getBoolean(2) ? row.getLong(1) : -1;
        }
    }

    private Instant lastRefresh(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT refreshed_at FROM catalog_sync_state WHERE singleton=TRUE");
                ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getObject(1, OffsetDateTime.class).toInstant() : clock.instant();
        }
    }

    private static void rollback(Connection connection, SQLException original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private static IOException failure(String message, SQLException cause) {
        return new IOException(message, cause);
    }
}
