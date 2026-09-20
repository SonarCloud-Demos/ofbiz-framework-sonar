package org.apache.ofbiz.modern.accounting.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SearchInvoices {

    InvoicePageView search(InvoiceSearchQuery query);

    record InvoiceSearchQuery(
            String invoiceId,
            String invoiceTypeId,
            String statusId,
            String partyIdFrom,
            String partyId,
            int limit,
            int offset) {

        private static final int MAX_LIMIT = 100;

        public InvoiceSearchQuery {
            if (limit < 1 || limit > MAX_LIMIT) {
                throw new IllegalArgumentException("limit must be between 1 and 100");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("offset must not be negative");
            }
        }
    }

    record InvoiceSummaryView(
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

    record InvoicePageView(
            List<InvoiceSummaryView> invoices,
            long total,
            int limit,
            int offset,
            String source,
            String totalsNote) {

        public InvoicePageView {
            invoices = List.copyOf(invoices);
        }
    }
}
