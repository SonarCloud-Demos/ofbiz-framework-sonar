package org.apache.ofbiz.modern.accounting.invoices.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.apache.ofbiz.modern.accounting.invoices.application.InvoiceService.DeleteResult;
import org.apache.ofbiz.modern.accounting.invoices.application.port.in.InvoiceUseCases;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Detail;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.HeaderCommand;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Page;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Search;
import org.junit.jupiter.api.Test;

class InvoiceUiControllerTest {
    @Test
    void rendersCssPercentagesAndNavigation() {
        InvoiceUiController controller = new InvoiceUiController(new EmptyUseCases());

        String html = controller.page(null, null, null, null, null);

        assertTrue(html.contains("width:100%"));
        assertTrue(html.contains("OFBiz"));
        assertTrue(html.contains("Legacy"));
    }

    private static final class EmptyUseCases implements InvoiceUseCases {
        @Override public Page search(Search search) { return new Page(List.of(), 50, 0, 0, "test", "test note"); }
        @Override public Optional<Detail> find(String invoiceId) { return Optional.empty(); }
        @Override public Detail create(HeaderCommand command) { throw new UnsupportedOperationException(); }
        @Override public Optional<Detail> update(String invoiceId, HeaderCommand command) { return Optional.empty(); }
        @Override public DeleteResult delete(String invoiceId) { return DeleteResult.NOT_FOUND; }
    }
}
