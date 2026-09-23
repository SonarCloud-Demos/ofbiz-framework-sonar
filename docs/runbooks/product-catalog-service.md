# Product Catalog service runbook

Owner: Product Catalog team. Escalation: platform on-call for runtime/network failures; data owner for reconciliation failures.

## Triage

1. Correlate the alert window with request rate, error ratio, P95 latency, database connections, and outbox age.
2. Use the request correlation ID to inspect traces. Never log authorization headers or product data beyond approved identifiers.
3. Check readiness and the PostgreSQL dependency independently.
4. Check `catalog.projection.sync` and `catalog.shadow.comparisons`. Any sync failure, freshness breach, or shadow mismatch blocks cohort expansion.
5. If a deployment caused the failure, roll back the service image only; database changes must remain backward compatible.
6. If the read projection is suspect, set `catalog_pilot_enabled=false` at the edge and disable `CATALOG_PILOT_READ_ENABLED`. Return traffic to `/catalog/control/FindProduct`; do not repair data manually without a reconciliation record.

## Recovery and verification

Restore service, run the catalog API and marker smoke journey, confirm the error budget has stopped burning, and verify a successful synchronization, zero shadow mismatches, acceptable projection age, and outbox age below five minutes. Record the incident, image version, migration version, batch/reconciliation result, cohort decision, and rollback outcome.
