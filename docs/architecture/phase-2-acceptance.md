# Phase 2 acceptance matrix

## Repository implementation gate

| Capability | Required repository evidence | Status |
| --- | --- | --- |
| Decision record | `phase-2-decisions.md` records approved topology, controls, and waiver | Complete |
| Terraform formatting | All Terraform files pass `terraform fmt -check -recursive` | Complete for contract scaffold |
| Terraform validation | Every root configuration passes `terraform validate` without Azure credentials | Complete for contract scaffold |
| Terraform tests | Contract tests cover positive and rejected environment contracts; mocked Azure tests follow resource implementation | Complete for current scope |
| Root build | Root `./gradlew build` invokes the repository Terraform verification task | Complete |
| Module boundaries | Bootstrap, shared platform, and environment composition are separate | Complete |
| Environment contract | Test, stage, and production roots contain no tenant, subscription, credential, state, or secret value | Complete |
| Network controls | Isolated subnets, private DNS, private endpoints, and internal Container Apps are modeled | Complete for current modules |
| Identity controls | Workload identities, scoped ACR pull roles, Entra-only PostgreSQL, and OIDC providers are modeled | Complete for current modules |
| Policy controls | Static tests require region, TLS, tags, private networking, identity, diagnostics, and protected state | Complete |
| Data controls | Tests require private Storage/PostgreSQL/Service Bus, Entra database auth, backup, and retention controls | Complete for current modules |
| Cost controls | Tests require budgets, actual 50/80/100 thresholds, forecasted 100%, recipients, and cost tags | Complete |
| Observability | Log Analytics, Application Insights, action groups, platform alerts, and diagnostics for all supported resources are modeled | Complete |
| Delivery safety | CI performs validation only; the apply workflow is unconditionally disabled | Complete |
| Documentation | Bootstrap, validation, extension points, and deferred operational gates are documented | Complete for current scope |
| Dependency policy | Every supported dependency passes policy; unsupported ecosystems have an explicit, documented review path | AzureRM exception approved because dependency-check does not support Terraform providers; version and checksums are locked |

Repository-level completion requires every non-waived row to be complete. A dependency cannot be bypassed with an unpinned provider or an undocumented exception.

## Deferred operational gate

Before the first cloud deployment or Phase 4 production rollout, all of the following require evidence from a real Azure environment:

- reviewed provider-backed plan and controlled apply;
- disposable-environment destroy rehearsal;
- OIDC federation, protected environments, and least-privilege RBAC;
- private endpoints, DNS, origin isolation, WAF, and edge-bypass tests;
- image attestation, immutable-digest promotion, canary, failback, and rollback;
- managed-identity access to configuration, secrets, messaging, storage, and monitoring;
- backup, point-in-time restore, state recovery, and failure exercises;
- live logs, metrics, traces, dashboards, alerts, audit records, and retention;
- approved budgets, alert recipients, cost estimate, capacity assumptions, RPO, RTO, and SLOs;
- named operational owners, on-call routes, break-glass procedure, and runbooks.

Passing the repository gate does not imply that any deferred item passed.

## Completion decision

The repository implementation gate was completed on 2026-09-25. Phase 2 is complete only within the approved unapplied-infrastructure waiver. Every deferred operational item above remains mandatory before the first Azure deployment or Phase 4 production rollout.
