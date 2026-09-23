package org.apache.ofbiz.modern.catalog.adapter.out;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.ofbiz.modern.catalog.application.ProductCatalog;
import org.apache.ofbiz.modern.catalog.application.ProductProjectionWriter;
import org.apache.ofbiz.modern.catalog.domain.ProductPage;
import org.apache.ofbiz.modern.catalog.domain.ProductSearch;
import org.apache.ofbiz.modern.catalog.domain.ProductSummary;
import org.apache.ofbiz.modern.catalog.domain.StaleProjectionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcProductCatalog implements ProductCatalog, ProductProjectionWriter {
    private static final String BATCH_ID_PARAMETER = "batchId";
    private final JdbcClient jdbcClient;
    private final TransactionTemplate transactionTemplate;
    private final Duration maximumProjectionAge;

    public JdbcProductCatalog(
            JdbcClient jdbcClient,
            TransactionTemplate transactionTemplate,
            @Value("${catalog.projection.maximum-age:PT0S}") Duration maximumProjectionAge) {
        this.jdbcClient = jdbcClient;
        this.transactionTemplate = transactionTemplate;
        this.maximumProjectionAge = maximumProjectionAge;
    }

    @Override
    public ProductPage search(ProductSearch search) {
        String internalNamePattern = "%" + escapeLike(search.internalName()) + "%";
        long offset;
        try {
            offset = Math.multiplyExact((long) search.page(), search.size());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Requested page is too large", exception);
        }
        String filters = """
                WHERE (:productId = '' OR product_id = :productId)
                  AND (:internalName = '' OR internal_name ILIKE :internalName ESCAPE '\\')
                """;
        String orderBy = " ORDER BY " + search.sort().columnName() + " " + search.direction().name()
                + (search.sort() == ProductSearch.SortField.PRODUCT_ID ? "" : ", product_id " + search.direction().name());
        List<ProductSummary> items = bindFilters(jdbcClient.sql("""
                SELECT product_id, product_type_id, internal_name, brand_name, product_name, description
                FROM catalog_product
                """ + filters + orderBy + " LIMIT :size OFFSET :offset"), search, internalNamePattern)
                .param("size", search.size())
                .param("offset", offset)
                .query((resultSet, rowNumber) -> new ProductSummary(
                        resultSet.getString("product_id"),
                        resultSet.getString("product_type_id"),
                        resultSet.getString("internal_name"),
                        resultSet.getString("brand_name"),
                        resultSet.getString("product_name"),
                        resultSet.getString("description")))
                .list();
        long total = bindFilters(jdbcClient.sql("SELECT COUNT(*) FROM catalog_product " + filters), search, internalNamePattern)
                .query(Long.class)
                .single();
        Timestamp latest = jdbcClient.sql("""
                SELECT COALESCE(MAX(updated_at), TIMESTAMPTZ '1970-01-01 00:00:00+00')
                FROM catalog_product
                """)
                .query(Timestamp.class)
                .single();
        if (!maximumProjectionAge.isZero()
                && latest.toInstant().isBefore(Instant.now().minus(maximumProjectionAge))) {
            throw new StaleProjectionException();
        }
        return new ProductPage(items, search.page(), search.size(), total, latest.toInstant());
    }

    private JdbcClient.StatementSpec bindFilters(
            JdbcClient.StatementSpec statement,
            ProductSearch search,
            String internalNamePattern) {
        return statement
                .param("productId", search.productId())
                .param("internalName", internalNamePattern);
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public void replaceSnapshot(List<ProductSummary> products, long expectedTotal, Instant extractedAt) {
        if (expectedTotal <= 0 || products.size() != expectedTotal) {
            throw new IllegalArgumentException("Refusing incomplete or empty catalog snapshot");
        }
        Timestamp latestProjection = jdbcClient.sql("""
                SELECT COALESCE(MAX(updated_at), TIMESTAMPTZ '1970-01-01 00:00:00+00')
                FROM catalog_product
                """).query(Timestamp.class).single();
        if (extractedAt.isBefore(latestProjection.toInstant())) {
            throw new IllegalArgumentException("Refusing catalog snapshot older than the current projection");
        }
        UUID batchId = UUID.randomUUID();
        long sourceVersion = extractedAt.toEpochMilli();
        transactionTemplate.executeWithoutResult(status -> {
            for (ProductSummary product : products) {
                stageProduct(batchId, sourceVersion, extractedAt, product);
            }
            promoteBatch(batchId);
            long mismatches = reconciliationMismatchCount(batchId);
            if (mismatches != 0) {
                throw new IllegalStateException("Catalog reconciliation failed with " + mismatches + " mismatches");
            }
            jdbcClient.sql("DELETE FROM catalog_product_stage WHERE batch_id = :batchId")
                    .param(BATCH_ID_PARAMETER, batchId)
                    .update();
        });
    }

    private void stageProduct(UUID batchId, long sourceVersion, Instant extractedAt, ProductSummary product) {
        jdbcClient.sql("""
                INSERT INTO catalog_product_stage (
                    batch_id, product_id, product_type_id, internal_name, brand_name,
                    product_name, description, source_version, extracted_at
                ) VALUES (
                    :batchId, :productId, :productTypeId, :internalName, :brandName,
                    :productName, :description, :sourceVersion, :extractedAt
                )
                """)
                .param(BATCH_ID_PARAMETER, batchId)
                .param("productId", product.productId())
                .param("productTypeId", product.productTypeId())
                .param("internalName", product.internalName(), Types.VARCHAR)
                .param("brandName", product.brandName(), Types.VARCHAR)
                .param("productName", product.productName(), Types.VARCHAR)
                .param("description", product.description(), Types.VARCHAR)
                .param("sourceVersion", sourceVersion)
                .param("extractedAt", Timestamp.from(extractedAt))
                .update();
    }

    private void promoteBatch(UUID batchId) {
        jdbcClient.sql("""
                INSERT INTO catalog_product (
                    product_id, product_type_id, internal_name, brand_name, product_name,
                    description, source_version, updated_at
                )
                SELECT product_id, product_type_id, internal_name, brand_name, product_name,
                    description, source_version, extracted_at
                FROM catalog_product_stage
                WHERE batch_id = :batchId
                ON CONFLICT (product_id) DO UPDATE SET
                    product_type_id = EXCLUDED.product_type_id,
                    internal_name = EXCLUDED.internal_name,
                    brand_name = EXCLUDED.brand_name,
                    product_name = EXCLUDED.product_name,
                    description = EXCLUDED.description,
                    source_version = EXCLUDED.source_version,
                    updated_at = EXCLUDED.updated_at
                WHERE catalog_product.source_version <= EXCLUDED.source_version
                """).param(BATCH_ID_PARAMETER, batchId).update();
        jdbcClient.sql("""
                DELETE FROM catalog_product projection
                WHERE NOT EXISTS (
                    SELECT 1 FROM catalog_product_stage source
                    WHERE source.batch_id = :batchId AND source.product_id = projection.product_id
                )
                """).param(BATCH_ID_PARAMETER, batchId).update();
    }

    private long reconciliationMismatchCount(UUID batchId) {
        return jdbcClient.sql("""
                SELECT COUNT(*)
                FROM catalog_product_stage source
                FULL OUTER JOIN catalog_product projection USING (product_id)
                WHERE (source.batch_id = :batchId OR source.product_id IS NULL)
                  AND (
                    source.product_id IS NULL OR projection.product_id IS NULL
                    OR ROW(source.product_type_id, source.internal_name, source.brand_name,
                           source.product_name, source.description)
                       IS DISTINCT FROM
                       ROW(projection.product_type_id, projection.internal_name, projection.brand_name,
                           projection.product_name, projection.description)
                  )
                """).param(BATCH_ID_PARAMETER, batchId).query(Long.class).single();
    }
}
