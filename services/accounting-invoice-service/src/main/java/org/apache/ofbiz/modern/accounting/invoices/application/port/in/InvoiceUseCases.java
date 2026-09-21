package org.apache.ofbiz.modern.accounting.invoices.application.port.in;

import java.util.Optional;
import org.apache.ofbiz.modern.accounting.invoices.application.InvoiceService.DeleteResult;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Detail;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.HeaderCommand;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Page;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Search;

public interface InvoiceUseCases {
    Page search(Search search);
    Optional<Detail> find(String invoiceId);
    Detail create(HeaderCommand command);
    Optional<Detail> update(String invoiceId, HeaderCommand command);
    DeleteResult delete(String invoiceId);
}
