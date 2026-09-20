package org.apache.ofbiz.modern.accounting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSearchCriteria;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummary;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummaryPage;
import org.junit.jupiter.api.Test;

class InvoiceQueryServiceTest {

    @Test
    void returnsTheLegacyProjection() {
        InvoiceSummary invoice = new InvoiceSummary(
                "8009",
                "SALES_INVOICE",
                "Sales Invoice",
                "INVOICE_READY",
                "Ready",
                "Company",
                "Your Company Name Here",
                "DemoCustomer",
                "Demo Customer",
                LocalDateTime.parse("2009-08-03T00:00:00"),
                null,
                "USD",
                2,
                new BigDecimal("100.00"),
                new BigDecimal("99.99"),
                new BigDecimal("0.01"));
        InvoiceQueryService service = new InvoiceQueryService(criteria ->
                new InvoiceSummaryPage(List.of(invoice), 1, criteria.limit(), criteria.offset()));

        InvoiceSummaryPage result = service.search(criteria(10, 0));

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.invoices()).singleElement().extracting(InvoiceSummary::invoiceId)
                .isEqualTo("8009");
    }

    @Test
    void rejectsUnsafePaginationValues() {
        assertThatThrownBy(() -> new org.apache.ofbiz.modern.accounting.application.port.in.SearchInvoices.InvoiceSearchQuery(
                null, null, null, null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 100");
        assertThatThrownBy(() -> new org.apache.ofbiz.modern.accounting.application.port.in.SearchInvoices.InvoiceSearchQuery(
                null, null, null, null, null, 10, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("offset must not be negative");
    }

    private static InvoiceSearchCriteria criteria(int limit, int offset) {
        return new InvoiceSearchCriteria(null, null, null, null, null, limit, offset);
    }
}
