package org.apache.ofbiz.modern.accounting.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.ofbiz.modern.accounting.application.port.out.LoadInvoiceSummaries;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSearchCriteria;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummaryPage;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class JdbcInvoiceDetailRepositoryTest {

    @Test
    void skipsChildQueriesWhenTheInvoiceDoesNotExist() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        LoadInvoiceSummaries loadInvoiceSummaries = mock(LoadInvoiceSummaries.class);
        InvoiceSearchCriteria expectedCriteria = new InvoiceSearchCriteria(
                "missing", null, null, null, null, 1, 0);
        when(loadInvoiceSummaries.load(expectedCriteria))
                .thenReturn(new InvoiceSummaryPage(List.of(), 0, 1, 0));
        JdbcInvoiceDetailRepository repository = new JdbcInvoiceDetailRepository(
                jdbcTemplate, loadInvoiceSummaries);

        assertThat(repository.load("missing")).isEmpty();
        verifyNoInteractions(jdbcTemplate);
    }
}
