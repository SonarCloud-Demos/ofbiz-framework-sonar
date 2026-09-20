package org.apache.ofbiz.modern.accounting.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDetail(
        InvoiceSummary header,
        List<LineItem> lineItems,
        List<PaymentApplication> paymentApplications,
        List<StatusHistory> statusHistory) {

    public InvoiceDetail {
        lineItems = List.copyOf(lineItems);
        paymentApplications = List.copyOf(paymentApplications);
        statusHistory = List.copyOf(statusHistory);
    }

    public record LineItem(
            String invoiceItemSeqId,
            String invoiceItemTypeId,
            String invoiceItemTypeDescription,
            String productId,
            String description,
            BigDecimal quantity,
            BigDecimal amount,
            BigDecimal lineTotal) {
    }

    public record PaymentApplication(
            String paymentApplicationId,
            String paymentId,
            String paymentTypeId,
            String paymentTypeDescription,
            String statusId,
            String statusDescription,
            LocalDateTime effectiveDate,
            BigDecimal amountApplied) {
    }

    public record StatusHistory(
            String statusId,
            String statusDescription,
            LocalDateTime statusDate,
            String changedByUserLoginId) {
    }
}
