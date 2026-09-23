# Product Catalog reference service

This thin read-projection service demonstrates the Phase 3 ports-and-adapters baseline. It owns its PostgreSQL schema, exposes the versioned catalog search contract, includes outbox and idempotent-consumer tables, and emits health/OTLP metrics.

Local mode intentionally permits requests only inside the developer stack. Set `CATALOG_SECURITY_MODE=entra` and provide standard Spring resource-server issuer configuration in deployed environments. The local gateway is not a production security boundary.

Use `./modern test`, `./modern start`, and `./modern smoke` from the repository root.
