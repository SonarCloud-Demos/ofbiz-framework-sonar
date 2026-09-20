package org.apache.ofbiz.modern.accounting.application.port.out;

import java.util.Optional;
import org.apache.ofbiz.modern.accounting.domain.InvoiceDetail;

public interface LoadInvoiceDetail {

    Optional<InvoiceDetail> load(String invoiceId);
}
