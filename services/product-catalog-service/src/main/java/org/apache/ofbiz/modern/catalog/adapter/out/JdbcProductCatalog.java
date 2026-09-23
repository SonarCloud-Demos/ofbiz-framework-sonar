package org.apache.ofbiz.modern.catalog.adapter.out;

import java.util.List;

import org.apache.ofbiz.modern.catalog.application.ProductCatalog;
import org.apache.ofbiz.modern.catalog.domain.ProductSummary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProductCatalog implements ProductCatalog {
    private final JdbcClient jdbcClient;

    public JdbcProductCatalog(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<ProductSummary> search(String query, int limit) {
        String escaped = query.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String pattern = "%" + escaped + "%";
        return jdbcClient.sql("""
                SELECT product_id, product_name, description
                FROM catalog_product
                WHERE product_name ILIKE :pattern ESCAPE '\\' OR product_id ILIKE :pattern ESCAPE '\\'
                ORDER BY product_name, product_id
                LIMIT :limit
                """)
                .param("pattern", pattern)
                .param("limit", limit)
                .query((resultSet, rowNumber) -> new ProductSummary(
                        resultSet.getString("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("description")))
                .list();
    }
}
