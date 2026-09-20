package org.apache.ofbiz.modern.accounting.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GetInvoiceDetail {

    Optional<InvoiceDetailView> get(String invoiceId);

    record InvoiceDetailView(
            SearchInvoices.InvoiceSummaryView header,
            List<LineItemView> lineItems,
            List<PaymentApplicationView> paymentApplications,
            List<StatusHistoryView> statusHistory,
            String source,
            String totalsNote) {

        public InvoiceDetailView {
            lineItems = List.copyOf(lineItems);
            paymentApplications = List.copyOf(paymentApplications);
            statusHistory = List.copyOf(statusHistory);
        }
    }

    record LineItemView(
            String invoiceItemSeqId,
            String invoiceItemTypeId,
            String invoiceItemTypeDescription,
            String productId,
            String description,
            BigDecimal quantity,
            BigDecimal amount,
            BigDecimal lineTotal) {
    }

    record PaymentApplicationView(
            String paymentApplicationId,
            String paymentId,
            String paymentTypeId,
            String paymentTypeDescription,
            String statusId,
            String statusDescription,
            LocalDateTime effectiveDate,
            BigDecimal amountApplied) {
    }

    record StatusHistoryView(
            String statusId,
            String statusDescription,
            LocalDateTime statusDate,
            String changedByUserLoginId) {
    }
}
