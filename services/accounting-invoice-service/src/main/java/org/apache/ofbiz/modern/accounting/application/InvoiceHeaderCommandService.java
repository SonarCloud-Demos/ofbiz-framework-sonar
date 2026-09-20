package org.apache.ofbiz.modern.accounting.application;

import org.apache.ofbiz.modern.accounting.application.port.in.ManageInvoiceHeaders;
import org.apache.ofbiz.modern.accounting.application.port.out.StoreInvoiceHeaders;
import org.apache.ofbiz.modern.accounting.domain.InvoiceHeaderMutation;

public class InvoiceHeaderCommandService implements ManageInvoiceHeaders {

    private final StoreInvoiceHeaders store;

    public InvoiceHeaderCommandService(StoreInvoiceHeaders store) {
        this.store = store;
    }

    @Override
    public MutationResult create(InvoiceHeaderMutation invoice) {
        return store.insert(invoice);
    }

    @Override
    public MutationResult update(InvoiceHeaderMutation invoice) {
        return store.update(invoice);
    }

    @Override
    public MutationResult delete(String invoiceId) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("invoiceId is required");
        }
        return store.deleteGuarded(invoiceId.trim());
    }
}
