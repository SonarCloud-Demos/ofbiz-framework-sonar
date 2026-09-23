# Product Catalog reference service

This read-projection service implements the Phase 5 product-search pilot on the Phase 3 ports-and-adapters baseline. It owns its PostgreSQL schema, exposes the canonical six-field paged search contract, includes staging/reconciliation support plus outbox and idempotent-consumer tables, and emits health/OTLP metrics.

Local mode intentionally permits requests only inside the developer stack. Set `CATALOG_SECURITY_MODE=entra` and provide standard Spring resource-server issuer configuration in deployed environments. The local gateway is not a production security boundary.

`CATALOG_PILOT_READ_ENABLED` is fail-closed and defaults to `false`. Enabling it does not by itself authorize traffic; follow `docs/migration/phase-5/README.md` for reconciliation, cohort, evidence, and rollback gates.

The temporary legacy adapter is independently fail-closed. `CATALOG_LEGACY_ADAPTER_ENABLED=true` requires an HTTPS base URL outside local mode and a bearer token supplied through `CATALOG_LEGACY_TOKEN_FILE`. Shadow comparison and synchronization have separate flags. Synchronization accepts only a complete, non-empty, non-stale snapshot and promotes it transactionally after reconciliation.

Use `./modern test`, `./modern start`, and `./modern smoke` from the repository root.
