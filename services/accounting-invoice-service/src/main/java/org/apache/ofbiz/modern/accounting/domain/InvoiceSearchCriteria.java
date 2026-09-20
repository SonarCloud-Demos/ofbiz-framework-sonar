package org.apache.ofbiz.modern.accounting.domain;

public record InvoiceSearchCriteria(
        String invoiceId,
        String invoiceTypeId,
        String statusId,
        String partyIdFrom,
        String partyId,
        int limit,
        int offset) {
}
