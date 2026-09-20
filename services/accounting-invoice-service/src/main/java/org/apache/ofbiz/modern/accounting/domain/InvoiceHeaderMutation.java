package org.apache.ofbiz.modern.accounting.domain;

import java.time.Clock;
import java.time.LocalDateTime;

public record InvoiceHeaderMutation(
        String invoiceId,
        String invoiceTypeId,
        String partyIdFrom,
        String partyId,
        String roleTypeId,
        String statusId,
        String billingAccountId,
        String contactMechId,
        LocalDateTime invoiceDate,
        LocalDateTime dueDate,
        LocalDateTime paidDate,
        String invoiceMessage,
        String referenceNumber,
        String description,
        String currencyUomId,
        String recurrenceInfoId) {

    public InvoiceHeaderMutation {
        invoiceId = required(invoiceId, "invoiceId");
        invoiceTypeId = required(invoiceTypeId, "invoiceTypeId");
        partyIdFrom = required(partyIdFrom, "partyIdFrom");
        partyId = required(partyId, "partyId");
        statusId = defaultIfBlank(statusId, "INVOICE_IN_PROCESS");
        invoiceDate = invoiceDate == null ? LocalDateTime.now(Clock.systemUTC()) : invoiceDate;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
