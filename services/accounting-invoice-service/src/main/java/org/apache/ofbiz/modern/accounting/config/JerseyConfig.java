package org.apache.ofbiz.modern.accounting.config;

import org.apache.ofbiz.modern.accounting.adapter.in.web.HealthResource;
import org.apache.ofbiz.modern.accounting.adapter.in.web.IllegalArgumentExceptionMapper;
import org.apache.ofbiz.modern.accounting.adapter.in.web.InvoiceResource;
import org.apache.ofbiz.modern.accounting.adapter.in.web.InvoicePageResource;
import org.apache.ofbiz.modern.accounting.adapter.out.persistence.JdbcInvoiceSummaryRepository;
import org.apache.ofbiz.modern.accounting.adapter.out.persistence.JdbcInvoiceDetailRepository;
import org.apache.ofbiz.modern.accounting.adapter.out.persistence.JdbcInvoiceHeaderRepository;
import org.apache.ofbiz.modern.accounting.application.InvoiceHeaderCommandService;
import org.apache.ofbiz.modern.accounting.application.InvoiceDetailQueryService;
import org.apache.ofbiz.modern.accounting.application.InvoiceQueryService;
import org.apache.ofbiz.modern.accounting.application.ServiceHealthService;
import org.apache.ofbiz.modern.accounting.application.port.in.GetServiceHealth;
import org.apache.ofbiz.modern.accounting.application.port.in.GetInvoiceDetail;
import org.apache.ofbiz.modern.accounting.application.port.in.SearchInvoices;
import org.apache.ofbiz.modern.accounting.application.port.in.ManageInvoiceHeaders;
import org.apache.ofbiz.modern.accounting.application.port.out.LoadInvoiceSummaries;
import org.apache.ofbiz.modern.accounting.application.port.out.LoadInvoiceDetail;
import org.apache.ofbiz.modern.accounting.application.port.out.StoreInvoiceHeaders;
import org.apache.ofbiz.modern.accounting.domain.InvoiceDetail;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSearchCriteria;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummary;
import org.apache.ofbiz.modern.accounting.domain.InvoiceSummaryPage;
import org.apache.ofbiz.modern.accounting.domain.ServiceHealth;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class JerseyConfig extends ResourceConfig {

    private static final String SOURCE = "legacy-ofbiz-postgres";
    private static final String TOTALS_NOTE = "Totals are SQL-derived and must be reconciled with OFBiz InvoiceWorker before write cutover.";

    public JerseyConfig() {
        register(HealthResource.class);
        register(IllegalArgumentExceptionMapper.class);
        register(InvoiceResource.class);
        register(InvoicePageResource.class);
    }

    @Bean
    GetServiceHealth getServiceHealth() {
        ServiceHealthService service = new ServiceHealthService();
        return () -> toHealthView(service.getHealth());
    }

    private static GetServiceHealth.HealthView toHealthView(ServiceHealth health) {
        return new GetServiceHealth.HealthView(health.status(), health.service());
    }

    @Bean
    LoadInvoiceSummaries loadInvoiceSummaries(NamedParameterJdbcTemplate jdbcTemplate) {
        return new JdbcInvoiceSummaryRepository(jdbcTemplate);
    }

    @Bean
    SearchInvoices searchInvoices(LoadInvoiceSummaries loadInvoiceSummaries) {
        InvoiceQueryService service = new InvoiceQueryService(loadInvoiceSummaries);
        return query -> toInvoicePageView(service.search(toCriteria(query)));
    }

    @Bean
    LoadInvoiceDetail loadInvoiceDetail(
            NamedParameterJdbcTemplate jdbcTemplate,
            LoadInvoiceSummaries loadInvoiceSummaries) {
        return new JdbcInvoiceDetailRepository(jdbcTemplate, loadInvoiceSummaries);
    }

    @Bean
    GetInvoiceDetail getInvoiceDetail(LoadInvoiceDetail loadInvoiceDetail) {
        InvoiceDetailQueryService service = new InvoiceDetailQueryService(loadInvoiceDetail);
        return invoiceId -> service.get(invoiceId).map(JerseyConfig::toInvoiceDetailView);
    }

    @Bean
    StoreInvoiceHeaders storeInvoiceHeaders(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        return new JdbcInvoiceHeaderRepository(jdbcTemplate, new TransactionTemplate(transactionManager));
    }

    @Bean
    ManageInvoiceHeaders manageInvoiceHeaders(StoreInvoiceHeaders storeInvoiceHeaders) {
        return new InvoiceHeaderCommandService(storeInvoiceHeaders);
    }

    private static InvoiceSearchCriteria toCriteria(SearchInvoices.InvoiceSearchQuery query) {
        return new InvoiceSearchCriteria(
                query.invoiceId(),
                query.invoiceTypeId(),
                query.statusId(),
                query.partyIdFrom(),
                query.partyId(),
                query.limit(),
                query.offset());
    }

    private static SearchInvoices.InvoicePageView toInvoicePageView(InvoiceSummaryPage page) {
        return new SearchInvoices.InvoicePageView(
                page.invoices().stream().map(JerseyConfig::toInvoiceSummaryView).toList(),
                page.total(),
                page.limit(),
                page.offset(),
                SOURCE,
                TOTALS_NOTE);
    }

    private static SearchInvoices.InvoiceSummaryView toInvoiceSummaryView(InvoiceSummary invoice) {
        return new SearchInvoices.InvoiceSummaryView(
                invoice.invoiceId(),
                invoice.invoiceTypeId(),
                invoice.invoiceTypeDescription(),
                invoice.statusId(),
                invoice.statusDescription(),
                invoice.partyIdFrom(),
                invoice.partyNameFrom(),
                invoice.partyId(),
                invoice.partyName(),
                invoice.invoiceDate(),
                invoice.dueDate(),
                invoice.currencyUomId(),
                invoice.itemCount(),
                invoice.invoiceTotal(),
                invoice.appliedPaymentTotal(),
                invoice.outstandingTotal());
    }

    private static GetInvoiceDetail.InvoiceDetailView toInvoiceDetailView(InvoiceDetail detail) {
        return new GetInvoiceDetail.InvoiceDetailView(
                toInvoiceSummaryView(detail.header()),
                detail.lineItems().stream().map(JerseyConfig::toLineItemView).toList(),
                detail.paymentApplications().stream().map(JerseyConfig::toPaymentApplicationView).toList(),
                detail.statusHistory().stream().map(JerseyConfig::toStatusHistoryView).toList(),
                SOURCE,
                TOTALS_NOTE);
    }

    private static GetInvoiceDetail.LineItemView toLineItemView(InvoiceDetail.LineItem item) {
        return new GetInvoiceDetail.LineItemView(
                item.invoiceItemSeqId(),
                item.invoiceItemTypeId(),
                item.invoiceItemTypeDescription(),
                item.productId(),
                item.description(),
                item.quantity(),
                item.amount(),
                item.lineTotal());
    }

    private static GetInvoiceDetail.PaymentApplicationView toPaymentApplicationView(
            InvoiceDetail.PaymentApplication paymentApplication) {
        return new GetInvoiceDetail.PaymentApplicationView(
                paymentApplication.paymentApplicationId(),
                paymentApplication.paymentId(),
                paymentApplication.paymentTypeId(),
                paymentApplication.paymentTypeDescription(),
                paymentApplication.statusId(),
                paymentApplication.statusDescription(),
                paymentApplication.effectiveDate(),
                paymentApplication.amountApplied());
    }

    private static GetInvoiceDetail.StatusHistoryView toStatusHistoryView(
            InvoiceDetail.StatusHistory history) {
        return new GetInvoiceDetail.StatusHistoryView(
                history.statusId(),
                history.statusDescription(),
                history.statusDate(),
                history.changedByUserLoginId());
    }
}
