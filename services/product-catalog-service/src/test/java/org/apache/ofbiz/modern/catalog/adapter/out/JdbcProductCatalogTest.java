package org.apache.ofbiz.modern.catalog.adapter.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;

import org.apache.ofbiz.modern.catalog.domain.ProductSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class JdbcProductCatalogTest {
    @Mock
    private JdbcClient jdbcClient;

    @Mock
    private JdbcClient.StatementSpec statement;

    @Mock
    private JdbcClient.MappedQuerySpec<ProductSummary> query;

    @Captor
    private ArgumentCaptor<RowMapper<ProductSummary>> rowMapperCaptor;

    @InjectMocks
    private JdbcProductCatalog productCatalog;

    @Test
    void bindsSearchParametersAndMapsProducts() throws Exception {
        ProductSummary expected = new ProductSummary("GZ-1000", "Gizmo", "Seed product");
        when(jdbcClient.sql(any(String.class))).thenReturn(statement);
        when(statement.param("pattern", "%Gizmo%")).thenReturn(statement);
        when(statement.param("limit", 10)).thenReturn(statement);
        when(statement.query(any(RowMapper.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of(expected));

        assertEquals(List.of(expected), productCatalog.search(" Gizmo ", 10));

        verify(statement).query(rowMapperCaptor.capture());
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("product_id")).thenReturn("GZ-1000");
        when(resultSet.getString("product_name")).thenReturn("Gizmo");
        when(resultSet.getString("description")).thenReturn("Seed product");
        assertEquals(expected, rowMapperCaptor.getValue().mapRow(resultSet, 0));
    }
}
