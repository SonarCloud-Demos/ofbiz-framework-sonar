# Modern Accounting Invoice Service

This Spring Boot 3 service is the Phase 1 strangler slice for Accounting > Invoices. It is
kept separate from the OFBiz runtime and uses the `org.apache.ofbiz.modern.accounting`
namespace defined by the SonarQube intended architecture.

The current foundation exposes:

- `GET /api/accounting/invoices/health`
- `GET /api/accounting/invoices` with `invoiceId`, `invoiceTypeId`, `statusId`,
  `partyIdFrom`, `partyId`, `limit`, and `offset` filters
- `GET /api/accounting/invoices/{invoiceId}` with header, line items, payment applications,
  and status history
- `POST /api/accounting/invoices` for an explicitly identified invoice header
- `PUT /api/accounting/invoices/{invoiceId}` for full header replacement
- `DELETE /api/accounting/invoices/{invoiceId}` when no items or payment applications exist
- `GET /modern/accounting/invoices` with the modern Helveticus-inspired search/list page
- Spring Boot Actuator health and info endpoints
- a PostgreSQL datasource configuration for the legacy OFBiz demo database

The list projection includes type/status labels, parties, item count, SQL-derived invoice total,
applied payments, outstanding amount, and pagination metadata. SQL totals are explicitly
provisional: they must be reconciled with `InvoiceWorker`, tax handling, currency conversion,
rounding, and accounting ECAs before any broader write cutover. Phase 1 writes are deliberately
limited to header columns. Create requires an explicit `invoiceId` because OFBiz sequencing is
outside this slice, and delete is rejected while invoice items or payment applications exist.
Item mutation, payment application, posting, taxes, promotions, GL effects, PDFs, and OFBiz ECAs
remain exclusively in the legacy application. OFBiz remains the source of truth.

Invalid filters or header payloads return `400 Bad Request` with an `INVALID_REQUEST` JSON error.
Duplicate IDs and guarded-delete dependency conflicts return `409 Conflict`; missing headers return
`404 Not Found`.

Direct header writes to the OFBiz database are the narrowly bounded Phase 1 demonstration exception
documented in
[`docs/architecture/phase-1-invoice-write-exception.md`](../../docs/architecture/phase-1-invoice-write-exception.md).
They do not represent production data-ownership cutover.

## Build

The build declares no public artifact repository. Supply the organization-approved Gradle init
script described in `docs/migration/npm-config.md`; it must inject the Artifactory plugin and
dependency repositories. This makes missing Artifactory configuration fail closed.

```sh
./gradlew --init-script "$PWD/gradle/modern-artifactory.init.gradle" \
    -p services/accounting-invoice-service test
```

The init script uses `ARTIFACTORY_ACCESS_TOKEN` or `ARTIFACTORY_PRIVATE_READER_TOKEN` from the
environment. Never put Artifactory credentials in this directory or in Gradle command-line
arguments.
