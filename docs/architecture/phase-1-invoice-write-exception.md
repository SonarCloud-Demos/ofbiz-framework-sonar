# Phase 1 invoice-header write exception

Status: Accepted for the Minimal Viable Modern Phase 1 demo only

## Context

The migration strategy establishes a default rule that a new service must not write directly to
the OFBiz database. The same strategy explicitly requires the Phase 1 Accounting service to
demonstrate limited create, update, and delete operations for invoice headers while OFBiz remains
the source of truth.

A dedicated Accounting database, backfill, event synchronization, reconciliation process, and
ownership cutover do not exist in Phase 1. Pretending that the modern service owns invoice data
would therefore be less accurate and harder to roll back than a visibly constrained exception.

## Decision

Phase 1 permits `modern-accounting-invoice-service` to write only columns on the legacy `invoice`
header table. This is a temporary demonstration exception to the normal data-ownership rule.

The exception does not permit writes to invoice items, payment applications, status history,
posting records, tax or promotion data, general-ledger data, PDFs, or any other OFBiz entity. OFBiz
continues to own all invoice lifecycle behavior and accounting side effects.

## Controls

- The only write routes are `POST /api/accounting/invoices`,
  `PUT /api/accounting/invoices/{invoiceId}`, and
  `DELETE /api/accounting/invoices/{invoiceId}`.
- Create requires an explicit invoice ID; the service does not imitate OFBiz sequencing.
- Header validation and parameterized SQL are applied before persistence.
- Delete locks the header and returns conflict when invoice items or payment applications exist.
- The Phase 1 smoke test uses an `MSVCTEST...` header and removes it on success or interruption.
- SQL-derived totals are labeled provisional and do not trigger accounting behavior.
- The gateway can roll the strangled search route back to OFBiz independently of the service.

## Rollback

Remove or disable the three write routes and route `/accounting/control/findInvoices` back to the
legacy upstream. Delete any remaining `MSVCTEST...` header after confirming that it has no dependent
rows. No schema migration is required because Phase 1 adds no database objects.

## Exit criteria

This exception must be removed before production write cutover. A successor design must provide:

1. a service-owned Accounting datastore;
2. documented invoice ownership boundaries;
3. backfill and reconciliation against OFBiz;
4. event or outbox synchronization during coexistence;
5. parity for required OFBiz business rules and side effects;
6. tested cutover and rollback procedures.

Until those criteria are met, the modern write endpoints are a local Phase 1 demonstration and not
a production ownership transfer.
