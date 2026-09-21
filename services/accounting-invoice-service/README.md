# Modern Accounting Invoice Service

Phase 1 Spring Boot 3 read projection and deliberately limited invoice-header CRUD. It reads the legacy OFBiz PostgreSQL schema during coexistence; this is a temporary migration exception, not the target ownership model.

Totals are SQL-derived. They include item amount × quantity and payment applications, but must be reconciled with OFBiz `InvoiceWorker`, service/entity ECAs, tax, promotion, rounding, and GL posting rules before any write cutover. The service does not mutate invoice items, payments, posting, tax, promotion, PDFs, or accounting side effects. Delete is allowed only when no items or payment applications exist.

Dependency resolution is fail-closed through Artifactory URLs. Credentials belong in a host/CI Gradle init script or BuildKit secret and must never be committed.
