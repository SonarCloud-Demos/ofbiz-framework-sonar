package org.apache.ofbiz.modern.accounting.invoices.application.port.out;

import java.util.Optional;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Detail;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.HeaderCommand;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Page;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Search;

public interface InvoiceRepository {
    Page search(Search search);
    Optional<Detail> findById(String invoiceId);
    Detail create(HeaderCommand command);
    Optional<Detail> update(String invoiceId, HeaderCommand command);
    boolean deleteIfIndependent(String invoiceId);
    boolean exists(String invoiceId);
}
