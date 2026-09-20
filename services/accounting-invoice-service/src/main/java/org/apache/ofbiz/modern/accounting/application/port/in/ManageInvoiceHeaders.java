package org.apache.ofbiz.modern.accounting.application.port.in;

import org.apache.ofbiz.modern.accounting.domain.InvoiceHeaderMutation;

public interface ManageInvoiceHeaders {

    MutationResult create(InvoiceHeaderMutation invoice);

    MutationResult update(InvoiceHeaderMutation invoice);

    MutationResult delete(String invoiceId);

    enum MutationResult {
        CREATED,
        UPDATED,
        DELETED,
        NOT_FOUND,
        DUPLICATE,
        HAS_DEPENDENCIES
    }
}
