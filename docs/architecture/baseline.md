# Migration baseline and measurement plan

## Captured static baseline

The reproducible repository baseline is recorded in `inventory/summary.json`. It currently contains 33 components, 29 webapps, 3,255 routes, 3,517 services, 1,118 entities/views, 356 ECAs, 22 seeded jobs, and 223 integration candidates. Static Java analysis reports 71 top-level package nodes and 399 dependencies.

These are scope indicators, not production usage metrics.

## Production baseline schema

Collect the following for at least 30 representative days and the longest relevant monthly/quarterly business event before setting pilot SLOs:

| Dimension | Measures | Source | Required breakdown |
| --- | --- | --- | --- |
| Traffic | Requests, authenticated users, concurrency, payload sizes | Edge/Tomcat access telemetry | Route, context, hour, tenant/store if applicable |
| Reliability | Success/error rate, business rejection rate, retries | Request/service telemetry | Route/service/error class |
| Performance | p50/p95/p99 and max duration | Distributed tracing/APM | Route, service, DB operation, external call |
| Data | Row counts, growth, update rate, largest aggregates | Database metrics/reconciliation queries | Entity/data area |
| Jobs | Starts, duration, lag, failures, retries, duplicate effects | JobSandbox/service telemetry | Job ID/service |
| Integrations | Calls/messages, latency, errors, timeouts, retry/DLQ | Adapter telemetry | Partner/mechanism/direction |
| Resources | CPU, heap, GC, threads, connection pools, cache hit rate | JVM/database monitoring | Instance and workload |
| Recovery | Incident count, MTTR, restore duration, data loss | Incident/backup exercises | Severity and context |
| Security | Auth failures, denied actions, privileged operations, findings | Identity/WAF/audit/SIEM | Route/context/risk |
| Delivery | Lead time, deployment frequency, failed changes | CI/CD and incident records | Deployable/context |

## Baseline ownership and acceptance

The platform team owns telemetry collection; product teams approve business correctness and peak windows; data owners approve reconciliation queries; security approves audit measures; operations approves incident/recovery figures. Raw production data is not committed to this repository. Approved aggregates, collection window, query/dashboard links, and sign-off date are recorded in the operational service catalog.

No pilot traffic rollout is allowed with `TBD` values for its availability, latency, error, security, reconciliation, rollback, or support acceptance thresholds.
