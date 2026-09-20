package org.apache.ofbiz.modern.accounting.application.port.out;

import org.apache.ofbiz.modern.accounting.application.port.in.ManageInvoiceHeaders.MutationResult;
import org.apache.ofbiz.modern.accounting.domain.InvoiceHeaderMutation;

public interface StoreInvoiceHeaders {

    MutationResult insert(InvoiceHeaderMutation invoice);

    MutationResult update(InvoiceHeaderMutation invoice);

    MutationResult deleteGuarded(String invoiceId);
}
