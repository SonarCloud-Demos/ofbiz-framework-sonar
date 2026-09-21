package org.apache.ofbiz.modern.accounting.invoices.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InvoiceModels {
    public static final String SOURCE = "legacy-ofbiz-postgres";
    public static final String TOTALS_NOTE = "Totals are SQL-derived and must be reconciled with OFBiz InvoiceWorker, accounting service ECAs, and GL posting rules before write cutover.";

    private InvoiceModels() { }

    public record Search(String invoiceId, String invoiceTypeId, String statusId,
                         String partyIdFrom, String partyId, int limit, int offset) { }

    public record Summary(String invoiceId, String invoiceTypeId, String invoiceTypeDescription,
                          String statusId, String statusDescription, String partyIdFrom, String partyId,
                          Instant invoiceDate, Instant dueDate, String currencyUomId, long itemCount,
                          BigDecimal invoiceTotal, BigDecimal appliedPaymentTotal, BigDecimal outstandingTotal) { }

    public record Page(List<Summary> invoices, int limit, int offset, long total,
                       String source, String totalsNote) { }

    public record Line(String invoiceItemSeqId, String invoiceItemTypeId, String productId,
                       String description, BigDecimal quantity, BigDecimal amount, BigDecimal lineTotal) { }

    public record PaymentApplication(String paymentApplicationId, String paymentId,
                                     String invoiceItemSeqId, BigDecimal amountApplied) { }

    public record StatusHistory(String statusId, String description, Instant statusDate,
                                String changeByUserLoginId) { }

    public record Detail(Summary header, List<Line> lineItems,
                         List<PaymentApplication> paymentApplications,
                         List<StatusHistory> statusHistory, String source, String totalsNote) { }

    public record HeaderCommand(String invoiceId, String invoiceTypeId, String partyIdFrom,
                                String partyId, String statusId, Instant invoiceDate, Instant dueDate,
                                String description, String currencyUomId) { }
}
