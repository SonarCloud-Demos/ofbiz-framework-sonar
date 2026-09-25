# Phase 2 Azure infrastructure decisions

## Status and scope

These decisions were accepted on 2026-09-25 for the Phase 2 repository implementation. Phase 2 targets Azure accurately as unapplied infrastructure as code. No Azure subscription, tenant, deployed test environment, stage environment, production environment, or production-like environment is available.

The repository implementation gate was completed on 2026-09-25. Phase 2 is complete under the unapplied-infrastructure waiver defined below. This status does not convert deferred operational evidence into passed evidence or authorize an Azure deployment.

The repository may satisfy the implementation acceptance criteria in `phase-2-acceptance.md`. It must not describe operational criteria as tested or passed. Before the first cloud deployment or Phase 4 production rollout, the deferred provider-backed plan, deployment, security, recovery, routing, identity, cost, capacity, and observability checks become mandatory.

## Platform decisions

### Compute

Azure Container Apps hosts services, workers, jobs, the BFF, and transitional OFBiz workloads. AKS is excluded from Phase 2. A later adoption requires a workload-specific ADR and an operational ownership model.

### Edge and API management

Azure Front Door Premium with WAF is the sole intended public application endpoint. UI, BFF, and legacy paths route from Front Door to private origins. External and browser-facing API paths route through API Management. Internal service-to-service calls remain private and do not traverse API Management. One public hostname preserves authentication, cookies, CSP, and legacy-to-modern navigation semantics.

### Data isolation

Each service owns a PostgreSQL database and exclusive identity. Multiple service databases may share an environment-level PostgreSQL Flexible Server initially. A dedicated server is required when availability, recovery, compliance, performance, maintenance, or noisy-neighbor requirements differ. Cross-service SQL, shared tables, and foreign-data access are prohibited. OFBiz remains on its separate legacy store.

### Local messaging and development

The official Azure Service Bus emulator is the local integration transport. Unit tests may use an in-memory fake. Real Service Bus is required for eventual hybrid and deployed validation. Messaging remains behind a narrow application port so local code does not acquire emulator-specific behavior.

The default developer workflow requires no Azure account or credentials. Compose supplies PostgreSQL, the Service Bus emulator, Azurite, a Redis-compatible service, and an OpenTelemetry collector as they become needed. Local identity uses a development OIDC provider, not an authentication bypass. Generated local credentials are ignored and are never production-compatible. Terraform uses mocked providers for local contract tests and does not deploy a local imitation of Azure.

### Region, naming, and tags

The reference region is `westeurope`, supplied as an environment input rather than embedded in reusable modules. Resource names combine a resource-type abbreviation, `ofbiz-modern`, service or capability, environment, and `weu`; a deterministic suffix is added only when global uniqueness requires it.

Every resource that supports tags receives `application`, `environment`, `owner`, `cost-center`, `managed-by`, `data-classification`, `criticality`, `repository`, `service`, and `migration-phase`. Owner and cost-center values are required inputs and are never invented by a module.

### Network

Each environment has an isolated VNet with input-supplied address space and separate subnets for Container Apps infrastructure, API Management, private endpoints, and management or build integration where required. Stateful platform services use private connectivity where supported. Container Apps uses an internal environment. Private DNS is linked per environment. Phase 2 creates no hub, peering, VPN, or ExpressRoute assumption, but module interfaces preserve a future hub-and-spoke integration point.

### Capability baseline

The reference architecture models Front Door Premium, a private-network-capable API Management tier, ACR Premium, Service Bus Premium, PostgreSQL Flexible Server, Key Vault Standard, App Configuration Standard, Standard GPv2 Blob Storage, Azure Managed Redis, a Container Apps workload-profile environment, Log Analytics, and workspace-based Application Insights. Capacity and instance size remain environment inputs. Tests assert required capabilities rather than production scale.

### Identity and configuration

Every workload receives its own user-assigned managed identity and least-privilege data-plane roles. Terraform planning and applying use separate GitHub OIDC identities, with a distinct apply identity per environment and repository/environment-bound subjects. Bootstrap identity is separate from workload deployment identities. Entra administrator, auditor, and approver groups are inputs. No client secret is committed or used for normal deployment.

Non-secret application settings live in App Configuration. Secrets and certificates live in Key Vault and are read using workload identity. Terraform creates stores, RBAC, and references but does not manage ordinary secret values. Configuration is namespaced by service and environment; feature flags are server-controlled and auditable.

### Cost, protection, and recovery

Every environment requires a monthly budget and alert recipients. Alerts are modeled at 50%, 80%, 100%, and forecasted 100%. Example amounts are placeholders, not forecasts.

PostgreSQL backup, retention, redundancy, and point-in-time recovery settings are inputs. Key Vault uses soft delete and purge protection. Blob Storage uses versioning and soft delete. Production stateful resources use deletion locks and lifecycle protection; non-production locks remain configurable. Restore procedures are documented but unproven.

The reference architecture is single-region with zone redundancy where the chosen tier supports it. Phase 2 does not model a deployed secondary region. RPO and RTO are required environment inputs, and future replication settings remain extension points.

### Observability

Each environment has a central Log Analytics workspace and workspace-based Application Insights. Applications emit OpenTelemetry data and preserve W3C trace context through Front Door, API Management, BFF, services, and messaging. Supported resources have diagnostic settings. Dashboard and alert contracts cover availability, latency, errors, saturation, queue age, dead letters, database health, reconciliation, and cost. Notification destinations, retention, classification, and approved SLOs remain environment inputs.

### Policy and delivery

Policy as code restricts regions and capability-compatible resource types, requires tags, diagnostics, private networking, managed identity, RBAC, TLS, and stateful-resource protection, and rejects anonymous storage, permissive inbound rules, and broad Key Vault access. An exception must have an owner, rationale, and expiry.

Pull requests run formatting, validation, mocked Terraform tests, policy and security checks, and root Gradle verification. Images receive an SBOM, vulnerability scan, provenance, and immutable digest. Apply, promotion, and drift workflows remain disabled until real Azure identity and state exist. Promotion uses the same digest across environments.

### Terraform state

Bootstrap state is separate from workload state. Each environment and major platform layer has separate state. Azure Storage backend configuration is partial and supplied externally. State storage is designed for private access, encryption, versioning, RBAC, and deletion protection. Workload modules never create or mutate their own backend. State, credentials, tenant IDs, and subscription IDs are not committed.

## Infrastructure waiver

The following evidence is waived only for repository-level Phase 2 completion: provider-backed plans, applies, destroys, private-origin probes, Entra federation, RBAC behavior, managed identity, WAF behavior, backup and restore, canary and rollback, live diagnostics, alerts, budgets, capacity, costs, and SLOs. These items are deferred, not passed. The waiver does not authorize cloud deployment or production traffic.
