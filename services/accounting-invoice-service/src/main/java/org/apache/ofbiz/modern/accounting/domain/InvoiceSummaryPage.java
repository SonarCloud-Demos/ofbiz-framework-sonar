package org.apache.ofbiz.modern.accounting.domain;

import java.util.List;

public record InvoiceSummaryPage(List<InvoiceSummary> invoices, long total, int limit, int offset) {

    public InvoiceSummaryPage {
        invoices = List.copyOf(invoices);
    }
}
