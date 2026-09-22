# Phase 0 measurement baseline

## Purpose

Record the evidence against which migration safety and value will be judged. Do not invent targets from source code. Product and operations owners must supply production measurements, measurement windows, and approved targets.

## Critical journey baseline

Complete one row per critical user or partner journey. Phase 1 will decompose each journey into routes, services, entities, ECA rules, jobs, and integrations.

| Journey | Business owner | User/cohort | Volume | Success rate | p50/p95/p99 duration | Error budget | Financial/control invariant | Recovery procedure |
| --- | --- | --- | ---: | ---: | --- | --- | --- | --- |
| Login and authorization | Unassigned | TBD | TBD | TBD | TBD | TBD | No unauthorized access | TBD |
| Product search/detail | Unassigned | TBD | TBD | TBD | TBD | TBD | Published data consistency | TBD |
| Order creation | Unassigned | TBD | TBD | TBD | TBD | TBD | No duplicate/lost orders | TBD |
| Inventory reservation | Unassigned | TBD | TBD | TBD | TBD | TBD | Stock never overcommitted beyond policy | TBD |
| Shipment processing | Unassigned | TBD | TBD | TBD | TBD | TBD | Shipment status/audit preserved | TBD |
| Invoice creation | Unassigned | TBD | TBD | TBD | TBD | TBD | Totals, tax and references reconcile | TBD |
| Payment processing | Unassigned | TBD | TBD | TBD | TBD | TBD | Idempotent capture/refund | TBD |
| Ledger posting/period close | Unassigned | TBD | TBD | TBD | TBD | TBD | Balanced, immutable postings | TBD |

Remove journeys that are genuinely unused and add organization-specific ones such as manufacturing, HR, content, or integrations.

## Platform and operational baseline

| Measure | Current value | Target | Evidence source | Owner |
| --- | --- | --- | --- | --- |
| Availability by critical journey | TBD | TBD | Production monitoring | Operations |
| Request error rate | TBD | TBD | Access/application logs | Operations |
| API latency percentiles | TBD | TBD | APM/traces | Operations |
| Browser Core Web Vitals | TBD | TBD | Real-user monitoring | UX/platform |
| Scheduled-job delay/failure | TBD | TBD | OFBiz job data/logs | Operations |
| Database size and growth | TBD | TBD | Database metrics | Data |
| Peak DB connections/CPU/IO | TBD | TBD | Database metrics | Data/platform |
| Backup success and restore time | TBD | TBD | Restore exercise | Operations/data |
| RPO/RTO by data class | TBD | TBD | Business impact analysis | Product/data |
| Mean time to detect/recover | TBD | TBD | Incident records | Operations |
| Deployment frequency/lead time | TBD | TBD | CI/CD history | Engineering |
| Change failure/rollback rate | TBD | TBD | Deploy/incident history | Engineering |
| Monthly infrastructure cost | TBD | TBD | Billing export | Sponsor/platform |
| Security finding age by severity | TBD | TBD | Security tooling | Security |

## Migration-wave measures

Every wave reports:

- percentage of in-scope reads, writes, jobs, routes, UI journeys, and owned tables served by modern components;
- reconciliation mismatch count and oldest unresolved mismatch;
- synchronization lag and dead-letter count;
- Modern/Mixed/Legacy route counts and expired Mixed routes;
- modern-versus-legacy functional comparison results;
- canary SLO comparison and rollback time;
- cost per representative transaction and forecast variance.

## Evidence rules

- Every value includes a source, query/dashboard link, measurement window, environment, and owner.
- Averages alone are insufficient for latency and availability; record percentiles and error distribution.
- Security, privacy, accessibility, and financial invariants are release gates even when aggregate availability is healthy.
- Baselines are versioned. A changed business target requires approval and must not rewrite historical evidence.
