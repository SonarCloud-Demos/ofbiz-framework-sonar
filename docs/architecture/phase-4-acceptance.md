# Phase 4 acceptance matrix

## Repository and local-runtime implementation

| Capability | Evidence | Status |
| --- | --- | --- |
| Bounded read contract | `contracts/external-api/product-catalog.openapi.yaml` exposes product/category reads without write or pricing authority | Complete locally |
| Anti-corruption boundary | A token-protected OFBiz endpoint maps legacy entities to a base64-delimited snapshot contract; modern code never queries OFBiz tables | Complete locally |
| Shadow model | PostgreSQL transactionally maintains the current projection, refresh checkpoint, and ordered append-only UPSERT/DELETE log | Complete for local development |
| Delta replay seam | OFBiz entity ECAs write a sequenced transactional outbox; the catalog service bootstraps once, then consumes by a transactionally stored cursor | Complete locally |
| Reconciliation seam | Internal report compares a fresh OFBiz source snapshot against the durable shadow by product identity and full mapped value | Complete locally |
| BFF and UI | Authenticated BFF proxies only catalog GETs; accessible shell supports safe text search and rendering | Complete locally |
| Rollback | `MODERN_ROUTES_ENABLED=false` sends catalog API traffic back to `/catalog/control/main` | Complete locally |
| Tests | Service and BFF tests cover filtering, read-only enforcement, reconciliation, correlation, routing, and safe UI rendering | Complete locally |

Stakeholder acceptance of the selected legacy behavior is recorded by the Phase 4 implementation decision on
2026-09-25. Numeric SLOs, reconciliation tolerances, and the production observation window remain intentionally
unset and are rollout gates, not values inferred by the implementation.

## Required before production traffic

- Approve and automate source-outbox retention using the durable consumer cursor as its low-water mark.
- Validate production route volume, latency, error, security, and support baselines.
- Configure approved SLOs, reconciliation tolerances, freshness limits, and observation window.
- Contract and validate production pricing-reference, content, authorization, and store-scoping dependencies.
- Complete all deferred Phase 2 and Phase 3 Azure, Entra, edge, observability, security, backup, and rollback evidence.
- Pass accessibility, security, authorization, load, stale-data, reconciliation, and route-failback rehearsals.

This implementation does not authorize Azure deployment or production traffic and does not complete Phase 4's
production exit criteria.

## Local verification

Run `./modern up`, then use the legacy administration UI at `https://localhost:8443/catalog/control/main`
and the modern read UI at `https://localhost:8444/catalog`. The legacy port is bound to loopback only for
development; production must keep OFBiz behind the controlled edge. A saved product name is visible in the
modern UI on its next catalog request because the local shadow refreshes on read.
