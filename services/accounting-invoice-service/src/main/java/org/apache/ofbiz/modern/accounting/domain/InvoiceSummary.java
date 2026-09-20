package org.apache.ofbiz.modern.accounting.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceSummary(
        String invoiceId,
        String invoiceTypeId,
        String invoiceTypeDescription,
        String statusId,
        String statusDescription,
        String partyIdFrom,
        String partyNameFrom,
        String partyId,
        String partyName,
        LocalDateTime invoiceDate,
        LocalDateTime dueDate,
        String currencyUomId,
        int itemCount,
        BigDecimal invoiceTotal,
        BigDecimal appliedPaymentTotal,
        BigDecimal outstandingTotal) {
}
