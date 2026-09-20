package org.apache.ofbiz.modern.accounting.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Optional;
import org.apache.ofbiz.modern.accounting.application.port.in.ManageInvoiceHeaders;
import org.apache.ofbiz.modern.accounting.application.port.in.ManageInvoiceHeaders.MutationResult;
import org.apache.ofbiz.modern.accounting.application.port.in.GetInvoiceDetail;
import org.apache.ofbiz.modern.accounting.application.port.in.SearchInvoices;
import org.junit.jupiter.api.Test;

class InvoiceResourceTest {

    @Test
    void returnsNotFoundWhenTheInvoiceDoesNotExist() {
        SearchInvoices searchInvoices = query -> new SearchInvoices.InvoicePageView(
                java.util.List.of(), 0, query.limit(), query.offset(), "test", "test");
        GetInvoiceDetail getInvoiceDetail = invoiceId -> Optional.empty();
        InvoiceResource resource = new InvoiceResource(searchInvoices, getInvoiceDetail, commands(MutationResult.DELETED));

        assertThatThrownBy(() -> resource.get("missing"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void mapsCreateDuplicateAndDeleteDependencyToConflict() {
        InvoiceResource duplicateResource = resource(commands(MutationResult.DUPLICATE));
        InvoiceResource dependentResource = resource(commands(MutationResult.HAS_DEPENDENCIES));

        assertThatThrownBy(() -> duplicateResource.create(request("9000")))
                .isInstanceOfSatisfying(WebApplicationException.class,
                        exception -> assertThat(exception.getResponse().getStatus()).isEqualTo(409));
        assertThatThrownBy(() -> dependentResource.delete("9000"))
                .isInstanceOfSatisfying(WebApplicationException.class,
                        exception -> assertThat(exception.getResponse().getStatus()).isEqualTo(409));
    }

    @Test
    void createsAtCanonicalLocationAndDeletesWithNoContent() {
        InvoiceResource resource = resource(commands(MutationResult.CREATED));

        assertThat(resource.create(request("9000")).getLocation().toString())
                .isEqualTo("/api/accounting/invoices/9000");
        assertThat(resource.delete("9000").getStatus()).isEqualTo(204);
    }

    private static InvoiceResource resource(ManageInvoiceHeaders commands) {
        SearchInvoices search = query -> new SearchInvoices.InvoicePageView(
                java.util.List.of(), 0, query.limit(), query.offset(), "test", "test");
        return new InvoiceResource(search, invoiceId -> Optional.empty(), commands);
    }

    private static ManageInvoiceHeaders commands(MutationResult result) {
        return new ManageInvoiceHeaders() {
            public MutationResult create(org.apache.ofbiz.modern.accounting.domain.InvoiceHeaderMutation invoice) { return result; }
            public MutationResult update(org.apache.ofbiz.modern.accounting.domain.InvoiceHeaderMutation invoice) { return result; }
            public MutationResult delete(String invoiceId) { return result; }
        };
    }

    private static InvoiceResource.InvoiceHeaderRequest request(String invoiceId) {
        return new InvoiceResource.InvoiceHeaderRequest(invoiceId, "SALES_INVOICE", "FROM", "TO",
                null, null, null, null, null, null, null, null, null, null, "USD", null);
    }
}
