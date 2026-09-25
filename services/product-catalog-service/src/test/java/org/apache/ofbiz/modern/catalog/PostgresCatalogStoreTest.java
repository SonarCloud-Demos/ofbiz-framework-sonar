package org.apache.ofbiz.modern.catalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostgresCatalogStoreTest {
    private final MemoryDriver driver = new MemoryDriver();

    PostgresCatalogStoreTest() throws SQLException {
        DriverManager.registerDriver(driver);
    }

    @AfterEach
    void unregisterDriver() throws SQLException {
        DriverManager.deregisterDriver(driver);
    }

    @Test
    void persistsCurrentStateOrderedChangesAndCheckpoint() throws Exception {
        Instant firstRefresh = Instant.parse("2026-09-25T08:00:00Z");
        Instant secondRefresh = firstRefresh.plusSeconds(60);
        PostgresCatalogStore store = new PostgresCatalogStore(MemoryDriver.URL, "catalog", "secret",
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        CatalogItem original = item("B", "Before");
        CatalogItem removed = item("C", "Removed");
        store.save(OfbizCatalogSnapshot.of(List.of(original, removed), firstRefresh));
        CatalogItem added = item("A", "Added");
        CatalogItem changed = item("B", "After");
        store.save(OfbizCatalogSnapshot.of(List.of(changed, added), secondRefresh));

        OfbizCatalogSnapshot loaded = store.loadOr(
                OfbizCatalogSnapshot.of(List.of(item("F", "Unused fallback")), Instant.EPOCH));
        assertEquals(List.of(added, changed), loaded.items());
        assertEquals(secondRefresh, loaded.refreshedAt());
        assertEquals(List.of("B:UPSERT", "C:UPSERT", "A:UPSERT", "B:UPSERT", "C:DELETE"), driver.database.changes);
        assertEquals(2, driver.database.commits);
    }

    @Test
    void returnsFallbackUntilAFirstSnapshotIsStored() throws Exception {
        Instant now = Instant.parse("2026-09-25T09:00:00Z");
        PostgresCatalogStore store = new PostgresCatalogStore(MemoryDriver.URL, "catalog", "secret",
                Clock.fixed(now, ZoneOffset.UTC));
        OfbizCatalogSnapshot fallback = OfbizCatalogSnapshot.of(List.of(item("F", "Fallback")), now);

        assertSame(fallback, store.loadOr(fallback));
    }

    @Test
    void wrapsDatabaseFailuresAndRollsBackFailedWrites() throws Exception {
        PostgresCatalogStore store = new PostgresCatalogStore(MemoryDriver.URL, "catalog", "secret",
                Clock.systemUTC());
        driver.database.failUpdates = true;

        var failure = assertThrows(java.io.IOException.class,
                () -> store.save(OfbizCatalogSnapshot.of(List.of(item("A", "Broken")), Instant.now())));

        assertEquals("Unable to persist catalog deltas", failure.getMessage());
        assertEquals(1, driver.database.rollbacks);
    }

    @Test
    void bootstrapsAndConsumesSourceChangesExactlyOnce() throws Exception {
        Instant refreshedAt = Instant.parse("2026-09-25T10:00:00Z");
        PostgresCatalogStore store = new PostgresCatalogStore(MemoryDriver.URL, "catalog", "secret",
                Clock.fixed(refreshedAt, ZoneOffset.UTC));
        assertEquals(-1, store.sourceCursor());
        store.bootstrapSource(OfbizCatalogSnapshot.of(List.of(item("OLD", "Before")), refreshedAt));

        List<OfbizCatalogChange> changes = List.of(
                new OfbizCatalogChange(1, CatalogDelta.Type.UPSERT, item("NEW", "Added")),
                new OfbizCatalogChange(2, CatalogDelta.Type.DELETE, item("OLD", "Before")));
        store.applySourceChanges(changes, refreshedAt.plusSeconds(1));
        store.applySourceChanges(changes, refreshedAt.plusSeconds(2));

        assertEquals(2, store.sourceCursor());
        assertEquals(List.of(item("NEW", "Added")), store.loadOr(
                OfbizCatalogSnapshot.of(List.of(item("F", "Fallback")), Instant.EPOCH)).items());
        assertEquals(3, driver.database.changes.size());
    }

    private static CatalogItem item(String id, String name) {
        return new CatalogItem(id, name, "CATEGORY", "Category", "ACTIVE", "USD 1.00");
    }

    private static final class MemoryDriver implements Driver {
        private static final String URL = "jdbc:catalog-memory:test";
        private final Database database = new Database();

        @Override
        public Connection connect(String url, Properties info) {
            return acceptsURL(url) ? database.connection() : null;
        }

        @Override public boolean acceptsURL(String url) { return URL.equals(url); }
        @Override public int getMajorVersion() { return 1; }
        @Override public int getMinorVersion() { return 0; }
        @Override public boolean jdbcCompliant() { return false; }
        @Override public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new java.sql.DriverPropertyInfo[0]; }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
    }

    private static final class Database {
        private final Map<String, CatalogItem> products = new LinkedHashMap<>();
        private final List<String> changes = new ArrayList<>();
        private Instant refreshedAt;
        private long sourceCursor;
        private boolean sourceInitialized;
        private int commits;
        private int rollbacks;
        private boolean failUpdates;

        Connection connection() {
            return proxy(Connection.class, (method, args) -> switch (method.getName()) {
                case "createStatement" -> statement();
                case "prepareStatement" -> prepared((String) args[0]);
                case "setAutoCommit", "close" -> null;
                case "commit" -> { commits++; yield null; }
                case "rollback" -> { rollbacks++; yield null; }
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            });
        }

        private Statement statement() {
            return proxy(Statement.class, (method, args) -> switch (method.getName()) {
                case "execute" -> true;
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            });
        }

        private PreparedStatement prepared(String sql) {
            Map<Integer, Object> parameters = new LinkedHashMap<>();
            return proxy(PreparedStatement.class, (method, args) -> switch (method.getName()) {
                case "setString", "setObject", "setLong" -> {
                    parameters.put((Integer) args[0], args[1]);
                    yield null;
                }
                case "executeQuery" -> query(sql);
                case "executeUpdate" -> update(sql, parameters);
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            });
        }

        private ResultSet query(String sql) {
            List<Object[]> rows = sql.startsWith("SELECT product_id")
                    ? products.values().stream().sorted(java.util.Comparator.comparing(CatalogItem::id))
                            .map(item -> new Object[] {item.id(), item.name(), item.categoryId(), item.categoryName(),
                                    item.status(), item.displayedPriceReference()}).toList()
                    : sql.startsWith("SELECT source_cursor")
                            ? refreshedAt == null ? List.of()
                                    : List.<Object[]>of(new Object[] {sourceCursor, sourceInitialized})
                            : refreshedAt == null ? List.of()
                                    : List.<Object[]>of(new Object[] {
                                            OffsetDateTime.ofInstant(refreshedAt, ZoneOffset.UTC)});
            int[] cursor = {-1};
            return proxy(ResultSet.class, (method, args) -> switch (method.getName()) {
                case "next" -> ++cursor[0] < rows.size();
                case "getString" -> (String) rows.get(cursor[0])[(Integer) args[0] - 1];
                case "getObject" -> rows.get(cursor[0])[(Integer) args[0] - 1];
                case "getLong" -> (Long) rows.get(cursor[0])[(Integer) args[0] - 1];
                case "getBoolean" -> (Boolean) rows.get(cursor[0])[(Integer) args[0] - 1];
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            });
        }

        private int update(String sql, Map<Integer, Object> parameters) throws SQLException {
            if (failUpdates) {
                throw new SQLException("simulated write failure");
            }
            if (sql.startsWith("INSERT INTO catalog_change")) {
                changes.add(parameters.get(2) + ":" + parameters.get(1));
            } else if (sql.startsWith("DELETE FROM")) {
                products.remove(parameters.get(1));
            } else if (sql.startsWith("INSERT INTO catalog_product")) {
                CatalogItem item = new CatalogItem((String) parameters.get(1), (String) parameters.get(2),
                        (String) parameters.get(3), (String) parameters.get(4), (String) parameters.get(5),
                        (String) parameters.get(6));
                products.put(item.id(), item);
            } else if (sql.startsWith("UPDATE catalog_sync_state")) {
                sourceCursor = 0;
                sourceInitialized = true;
            } else if (sql.startsWith("INSERT INTO catalog_sync_state")) {
                refreshedAt = ((OffsetDateTime) parameters.get(1)).toInstant();
                if (sql.contains("source_initialized")) {
                    sourceCursor = (Long) parameters.get(2);
                    sourceInitialized = true;
                }
            }
            return 1;
        }
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] args) throws Throwable;
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (ignored, method, args) -> invocation.invoke(method, args == null ? new Object[0] : args)));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        throw new IllegalArgumentException("Unsupported primitive " + type);
    }
}
