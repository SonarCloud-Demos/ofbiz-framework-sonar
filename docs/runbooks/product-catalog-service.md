# Product Catalog service runbook

Owner: Product Catalog team. Escalation: platform on-call for runtime/network failures; data owner for reconciliation failures.

## Triage

1. Correlate the alert window with request rate, error ratio, P95 latency, database connections, and outbox age.
2. Use the request correlation ID to inspect traces. Never log authorization headers or product data beyond approved identifiers.
3. Check readiness and the PostgreSQL dependency independently.
4. If a deployment caused the failure, roll back the service image only; database changes must remain backward compatible.
5. If the read projection is suspect, disable the local/edge route and return traffic to its prior owner. Do not repair data manually without a reconciliation record.

## Recovery and verification

Restore service, run the catalog API and marker smoke journey, confirm the error budget has stopped burning, and verify that outbox age returns below five minutes. Record the incident, image version, migration version, reconciliation result, and rollback decision.
