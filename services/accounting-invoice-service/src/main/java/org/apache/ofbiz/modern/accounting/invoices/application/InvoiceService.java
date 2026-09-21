package org.apache.ofbiz.modern.accounting.invoices.application;

import java.util.Optional;
import org.apache.ofbiz.modern.accounting.invoices.application.port.in.InvoiceUseCases;
import org.apache.ofbiz.modern.accounting.invoices.application.port.out.InvoiceRepository;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Detail;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.HeaderCommand;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Page;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Search;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService implements InvoiceUseCases {
    private final InvoiceRepository repository;

    public InvoiceService(InvoiceRepository repository) { this.repository = repository; }

    public Page search(Search search) { return repository.search(search); }
    public Optional<Detail> find(String invoiceId) { return repository.findById(invoiceId); }
    public Detail create(HeaderCommand command) { validate(command, true); return repository.create(command); }
    public Optional<Detail> update(String id, HeaderCommand command) { validate(command, false); return repository.update(id, command); }
    public DeleteResult delete(String id) {
        if (!repository.exists(id)) return DeleteResult.NOT_FOUND;
        return repository.deleteIfIndependent(id) ? DeleteResult.DELETED : DeleteResult.CONFLICT;
    }

    private static void validate(HeaderCommand command, boolean requireId) {
        if (command == null || (requireId && blank(command.invoiceId())) || blank(command.invoiceTypeId())
                || blank(command.partyIdFrom()) || blank(command.partyId()) || blank(command.statusId())
                || blank(command.currencyUomId())) {
            throw new IllegalArgumentException("invoiceId (for create), invoiceTypeId, parties, statusId, and currencyUomId are required");
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    public enum DeleteResult { DELETED, NOT_FOUND, CONFLICT }
}
