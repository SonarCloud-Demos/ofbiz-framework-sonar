package org.apache.ofbiz.modern.accounting.invoices.adapter.in.web;

import java.net.URI;
import java.util.Map;
import org.apache.ofbiz.modern.accounting.invoices.application.port.in.InvoiceUseCases;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.HeaderCommand;
import org.apache.ofbiz.modern.accounting.invoices.domain.InvoiceModels.Search;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounting/invoices")
public class InvoiceController {
    private final InvoiceUseCases service;
    public InvoiceController(InvoiceUseCases service) { this.service = service; }

    @GetMapping("/health") public Map<String, String> health() {
        return Map.of("status", "UP", "service", "modern-accounting-invoice-service");
    }

    @GetMapping public Object search(
            @RequestParam(required=false) String invoiceId, @RequestParam(required=false) String invoiceTypeId,
            @RequestParam(required=false) String statusId, @RequestParam(required=false) String partyIdFrom,
            @RequestParam(required=false) String partyId, @RequestParam(defaultValue="20") int limit,
            @RequestParam(defaultValue="0") int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return service.search(new Search(invoiceId, invoiceTypeId, statusId, partyIdFrom, partyId, safeLimit, Math.max(0, offset)));
    }

    @GetMapping("/{invoiceId}") public ResponseEntity<?> detail(@PathVariable String invoiceId) {
        return service.find(invoiceId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping public ResponseEntity<?> create(@RequestBody HeaderCommand command) {
        var created = service.create(command);
        return ResponseEntity.created(URI.create("/api/accounting/invoices/" + created.header().invoiceId())).body(created);
    }

    @PutMapping("/{invoiceId}") public ResponseEntity<?> update(@PathVariable String invoiceId, @RequestBody HeaderCommand command) {
        return service.update(invoiceId, command).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{invoiceId}") public ResponseEntity<?> delete(@PathVariable String invoiceId) {
        return switch (service.delete(invoiceId)) {
            case DELETED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Invoice has line items or payment applications and cannot be deleted by the Phase 1 service"));
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
