package org.apache.ofbiz.modern.accounting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSearchCriteria;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummary;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummaryPage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

class JdbcInvoiceSummaryRepositoryTest {

    @Test
    void bindsOnlyPopulatedFiltersAndKeepsPaginationMetadata() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class)))
                .thenReturn(0L);
        when(jdbcTemplate.query(
                anyString(),
                any(SqlParameterSource.class),
                org.mockito.ArgumentMatchers.<RowMapper<InvoiceSummary>>any()))
                .thenReturn(List.of());
        JdbcInvoiceSummaryRepository repository = new JdbcInvoiceSummaryRepository(jdbcTemplate);
        InvoiceSearchCriteria criteria = new InvoiceSearchCriteria(
                "8009", " ", null, null, null, 10, 20);

        InvoiceSummaryPage result = repository.load(criteria);

        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> parameters = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbcTemplate).queryForObject(countSql.capture(), parameters.capture(), eq(Long.class));
        assertThat(countSql.getValue()).contains("i.invoice_id = :invoiceId");
        assertThat(countSql.getValue()).doesNotContain("i.invoice_type_id = :invoiceTypeId");
        assertThat(parameters.getValue().getValue("invoiceId")).isEqualTo("8009");
        assertThat(result.invoices()).isEmpty();
        assertThat(result.limit()).isEqualTo(10);
        assertThat(result.offset()).isEqualTo(20);
    }
}
