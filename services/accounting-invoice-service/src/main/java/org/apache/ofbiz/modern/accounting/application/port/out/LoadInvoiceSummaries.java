package org.apache.ofbiz.modern.accounting.application.port.out;

import org.apache.ofbiz.modern.accounting.domain.InvoiceSearchCriteria;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummaryPage;

public interface LoadInvoiceSummaries {

    InvoiceSummaryPage load(InvoiceSearchCriteria criteria);
}
