package org.apache.ofbiz.modern.accounting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ofbiz.modern.accounting.application.port.in.SearchInvoices;
import org.junit.jupiter.api.Test;

class InvoicePageResourceTest {

    @Test
    void rendersBrandingNavigationAndEscapedInvoiceData() {
        SearchInvoices.InvoiceSummaryView invoice = new SearchInvoices.InvoiceSummaryView(
                "8009",
                "SALES_INVOICE",
                "Sales <script>alert(1)</script>",
                "INVOICE_READY",
                "Ready",
                "Company",
                "Your Company",
                "DemoCustomer",
                "Demo Customer",
                LocalDateTime.parse("2009-08-03T00:00:00"),
                null,
                "USD",
                2,
                new BigDecimal("100.00"),
                new BigDecimal("99.99"),
                new BigDecimal("0.01"));
        SearchInvoices searchInvoices = query -> new SearchInvoices.InvoicePageView(
                List.of(invoice),
                1,
                query.limit(),
                query.offset(),
                "legacy-ofbiz-postgres",
                "SQL-derived totals");
        InvoicePageResource resource = new InvoicePageResource(searchInvoices);

        Response response = resource.page("<img src=x>", null, null, null, null, 25, 0);
        String html = (String) response.getEntity();

        assertThat(html)
                .contains("#1BC5BD", "#dcfffd", "#133d3b")
                .contains("/images/ofbiz_logo.png")
                .contains("/accounting/control/findPayments")
                .contains("/accounting/control/FindTaxAuthority")
                .contains("/api/accounting/invoices/8009")
                .contains("Legacy detail", "8009")
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("&lt;img src=x&gt;")
                .contains("name=\"invoiceTypeId\" value=\"\"")
                .contains("name=\"statusId\" value=\"\"")
                .doesNotContain("value=\"&mdash;\"")
                .doesNotContain("<script>alert(1)</script>", "<img src=x>");
        assertThat(response.getHeaderString("Content-Security-Policy"))
                .contains("default-src 'self'", "form-action 'self'");
    }
}
