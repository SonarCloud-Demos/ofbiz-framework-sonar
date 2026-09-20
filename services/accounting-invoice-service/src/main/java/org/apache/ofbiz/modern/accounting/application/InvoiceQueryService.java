package org.apache.ofbiz.modern.accounting.application;

import org.apache.ofbiz.modern.accounting.application.port.out.LoadInvoiceSummaries;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSearchCriteria;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummaryPage;

public class InvoiceQueryService {

    private final LoadInvoiceSummaries loadInvoiceSummaries;

    public InvoiceQueryService(LoadInvoiceSummaries loadInvoiceSummaries) {
        this.loadInvoiceSummaries = loadInvoiceSummaries;
    }

    public InvoiceSummaryPage search(InvoiceSearchCriteria criteria) {
        return loadInvoiceSummaries.load(criteria);
    }
}
