package org.apache.ofbiz.modern.accounting.application;

import java.util.Optional;
import org.apache.ofbiz.modern.accounting.application.port.out.LoadInvoiceDetail;
import org.apache.ofbiz.modern.accounting.domain.InvoiceDetail;

public class InvoiceDetailQueryService {

    private final LoadInvoiceDetail loadInvoiceDetail;

    public InvoiceDetailQueryService(LoadInvoiceDetail loadInvoiceDetail) {
        this.loadInvoiceDetail = loadInvoiceDetail;
    }

    public Optional<InvoiceDetail> get(String invoiceId) {
        return loadInvoiceDetail.load(invoiceId);
    }
}
