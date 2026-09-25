# OFBiz microservices migration plan

## 1. Purpose and constraints

This plan moves the current OFBiz modular monolith to a microservices-based architecture on Azure without a big-bang rewrite. It uses the strangler pattern: one business slice at a time is implemented beside OFBiz, traffic and data ownership move only after verification, and the corresponding legacy path is removed only after a stable observation period.

The migration must preserve these constraints:

- All legacy code, modern services, modern UI, infrastructure code, contracts, tests, and developer tooling remain in one repository.
- `./gradlew build` remains the single authoritative command that builds and verifies all legacy and modern code throughout the migration.
- The unified build generates missing dependency lockfiles, consumes locked dependency versions, and fails CI when required lockfiles are absent, stale, or changed without review.
- A developer can build and start a representative integrated system locally without an Azure subscription.
- Legacy and modern pages coexist behind one public origin during migration.
- UI migration proceeds as part of each vertical business slice, not as a separate final rewrite.
- Every modern page displays an unambiguous marker, implemented so application content cannot spoof it.
- Security, observability, rollback, and data reconciliation are delivery requirements for every slice.
- The system remains releasable at the end of every phase.

This plan builds on the current-state description in `docs/migration/OFBIZ_current_architecture.md`. The current system is a single OFBiz JVM with shared service/entity registries, one transactional data model, embedded Tomcat, implicit ECA/scheduled flows, and dense cross-domain dependencies. Those characteristics make metadata, data ownership, and operational behavior as important to migration discovery as Java call graphs.

## 2. Migration principles

1. **Migrate vertical slices.** Each increment includes the browser route, UI, BFF/API, domain logic, data changes, events, operations, and legacy retirement for one usable capability.
2. **Strangle through stable entry points.** A common edge and route catalog decide whether a request goes to modern or legacy code. Clients do not choose the backend.
3. **One writer per business record.** Avoid permanent dual writes. Change ownership using outbox/events, reconciliation, and explicit cutover states.
4. **Database per service.** A service exclusively owns its schema or database and exposes data through its API or events. No new service reads the OFBiz schema directly in steady state.
5. **Events publish facts; APIs perform commands.** Use asynchronous domain events for propagation and synchronous APIs only when the caller needs an immediate business result.
6. **Do not distribute the monolith.** Bound services around business capabilities, not existing Java packages, tables, or individual entity types.
7. **Keep the platform managed and portable.** Prefer managed Azure services in production and standard protocols or emulators locally.
8. **Automate every transition.** Infrastructure, database migrations, route changes, policy, smoke tests, and rollback procedures are versioned and repeatable.
9. **Measure before removal.** Legacy behavior, volume, latency, errors, and reconciliation differences establish the acceptance baseline for a migrated slice.
10. **Secure by default.** Public exposure, identity, authorization, secrets, encryption, dependency provenance, and audit evidence are designed into the platform foundation.
11. **Keep one build authority.** The root Gradle lifecycle owns the complete repository build and analysis inputs. Language-specific tools run through pinned Gradle tasks; convenience scripts delegate to Gradle rather than defining a second build graph.
12. **Lock every dependency ecosystem.** Commit ecosystem-native lockfiles and make their generation and verification part of the root Gradle lifecycle. Normal builds are reproducible; dependency changes are explicit reviewable changes.

## 3. Target state

### 3.1 Logical architecture

```text
Users and API clients
          |
          v
Azure Front Door Premium + WAF
          |
          +---------------- public static assets ----------------+
          |                                                      |
          v                                                      v
Modern web shell / BFF                                  Azure Storage/CDN
          |
          v
Azure API Management
          |
          +--------------------+--------------------------+
          |                    |                          |
          v                    v                          v
Modern domain services   Legacy compatibility       Legacy OFBiz
on Container Apps        adapters/workers           on Container Apps
          |                    |                          |
          +----------+---------+--------------------------+
                     |
        +------------+---------------------+
        |            |          |          |
        v            v          v          v
 PostgreSQL       Service Bus  Blob     Redis
 per service      topics/queues Storage   cache

Shared controls: Entra ID, Managed Identity, Key Vault,
App Configuration, Azure Monitor, Application Insights,
Log Analytics, Defender, Policy, Private DNS and networking
```

The diagram shows a transitional target: the legacy OFBiz runtime remains behind the controlled edge until the final migrated slice has completed its retirement window. It is not exposed directly to the internet.

### 3.2 Azure platform decisions

| Concern | Target choice | Reason |
| --- | --- | --- |
| Global entry point | Azure Front Door Premium with WAF | One TLS endpoint, WAF policy, routing, rate limiting, health-based failover, and gradual traffic controls. |
| API boundary | Azure API Management | Central API policy, token validation, quotas, versioning, request correlation, and legacy/modern routing. |
| Compute | Azure Container Apps | Managed container orchestration, independent revision deployment and scaling, jobs for workers/migrations, and lower operational load than self-managed Kubernetes. Revisit AKS only if a validated workload requires Kubernetes-specific controls. |
| Identity | Microsoft Entra ID using OAuth 2.0/OIDC | Central workforce identity, MFA/Conditional Access, service principals, app roles, and standards-based tokens. |
| Workload identity | Managed identities | Removes stored Azure credentials and supports least-privilege access to platform resources. |
| Operational data | Azure Database for PostgreSQL Flexible Server | Managed relational storage. Each service receives a separately owned database or schema and credential boundary; production sizing may place high-criticality domains on separate servers. |
| Messaging | Azure Service Bus Premium | Durable commands/events, topics, subscriptions, dead-letter queues, duplicate detection, and private networking. |
| Cache | Azure Managed Redis | Distributed cache only; never the system of record. |
| Documents/media | Azure Blob Storage | Durable content with private endpoints, lifecycle policies, and scoped access. |
| Secrets/keys | Azure Key Vault | Central secret, key, and certificate management with managed-identity access and rotation. |
| Runtime configuration | Azure App Configuration | Versioned non-secret configuration and feature flags. |
| Images | Azure Container Registry | Private, scanned, immutable application images and promotion by digest. |
| Observability | OpenTelemetry to Application Insights/Azure Monitor and Log Analytics | Correlated traces, structured logs, metrics, dashboards, alerts, and service-level objectives. |
| IaC | Terraform with AzureRM/AzAPI providers | Repeatable environments, plan review, policy checks, and drift detection. |
| CI/CD identity | GitHub Actions OIDC federation to Azure | Short-lived credentials; no long-lived Azure deployment secrets. |

Production resources use private endpoints where available. Container Apps use an internal environment; only Front Door-approved ingress and APIM are public-facing. Network Security Groups, Private DNS, Azure Policy, resource locks for stateful production resources, diagnostic settings, budgets, and Defender plans are created by Terraform.

### 3.3 Service boundaries

The initial target context map is a hypothesis to validate during discovery. A context becomes a service only when it has cohesive business rules, clear ownership, and an independent change/scaling reason.

| Bounded context | Candidate responsibility | Likely sequencing |
| --- | --- | --- |
| Identity access adapter | Entra identity mapping, OFBiz transition, roles/permissions projection; not a password store | Foundation |
| Product catalog | Products, categories, attributes, catalog browsing | First pilot; begin read-only |
| Pricing and promotions | Price calculation, price lists, promotion eligibility | After catalog contract stabilizes |
| Party/customer | People, organizations, contacts, relationships, customer profile | Early/middle |
| Inventory/facility | Stock positions, reservations, movements, facilities | Middle; consistency-sensitive |
| Order management | Cart/order lifecycle, order state, fulfillment coordination | Middle/late |
| Shipping/fulfillment | Pick, pack, shipment, carrier integration | After inventory/order events stabilize |
| Work management | Work efforts, schedules, calendar | Independent middle slice |
| Content/media | Content metadata and blob-backed media | Early/middle |
| Manufacturing | BOM, routing, MRP, production runs | Late because of product/inventory coupling |
| Accounting | Invoices, payments, ledger, tax, financial controls | Last major domain because of cross-domain and audit requirements |
| Notification | Email and other outbound notifications driven by events | Early shared capability, not a domain database integration shortcut |

Do not create a generic shared business-data service. A small platform library may standardize telemetry, error envelopes, security middleware, and event metadata, but it must not contain domain models or coordinate releases across all services.

### 3.4 Interaction model

- External APIs are versioned and described with OpenAPI. Consumer-driven contract tests protect consumers during incremental releases.
- Browser code calls only its BFF or published gateway APIs. It never calls internal services directly.
- Commands that require an immediate answer use authenticated synchronous APIs with deadlines, bounded retries, idempotency keys, and circuit breakers.
- Cross-context propagation uses versioned Service Bus events with an event ID, correlation/causation IDs, tenant/actor metadata as appropriate, schema version, and occurrence time.
- Services publish events with a transactional outbox. Consumers are idempotent and maintain inbox/deduplication state.
- Long-running cross-service processes use explicit sagas/process managers with compensating actions. They do not attempt distributed database transactions.
- Dead-letter queues have ownership, alerts, replay tooling, and a runbook. Message retention is not a substitute for an audit record.
- API and event compatibility follows expand/migrate/contract. Breaking changes require a new version and a measured consumer migration.

### 3.5 Data target and ownership transition

Each service owns its data and migration scripts. Other services receive only API representations or domain events. Reporting uses a separately built read model or analytics platform fed by events; it does not create cross-service transactional joins.

Every record set moving out of OFBiz follows these states:

1. **Legacy-owned:** OFBiz is the sole writer. The modern side may read through an anti-corruption API or receive a replicated shadow copy.
2. **Shadowed:** a change-data-capture or outbox bridge feeds the modern store. Automated reconciliation compares counts, keys, totals, and domain invariants. Modern results are not authoritative.
3. **Modern-read:** selected reads use the modern service. OFBiz remains the writer and remains the immediate rollback path.
4. **Modern-owned:** the modern service becomes the sole writer. Events or an explicit compatibility projection supply only the legacy reads that remain necessary.
5. **Legacy-detached:** no live legacy consumer uses the old tables for that capability. Compatibility projection stops and obsolete schema/code is archived or removed.

Cutover uses an idempotent, rehearsed runbook: pause affected writes, drain events, take the final delta, reconcile, switch the route/ownership flag, run smoke tests, and resume. A rollback is allowed only while ownership and reverse synchronization make it safe. After irreversible business writes occur, recovery uses forward correction rather than silently restoring an old database.

Direct dual writes from request code to OFBiz and a modern database are prohibited because partial failure cannot be made reliably atomic. Temporary replication must have a named owner, an end date, lag/error monitoring, and a removal criterion.

### 3.6 UI target and concurrent migration

The target UI is a modern web shell with a consistent design system, accessibility baseline, navigation, authentication, authorization-aware menus, telemetry, error handling, and route registry. The default implementation should be one web application organized into independently owned feature modules. Deploying many browser micro-frontends is optional and requires evidence that independent UI deployment outweighs dependency and security complexity.

During migration:

- Front Door presents one origin. The modern shell and legacy OFBiz routes sit behind it, preventing cross-origin token and cookie workarounds.
- A route catalog under source control labels every route as `legacy`, `modern`, or `transition`, identifies its owning team and backend, and controls routing and UI marker behavior.
- A vertical slice is not complete until its modern page and BFF/API are production-capable. Backend-only migrations may support compatibility, but they do not count as user-facing strangler progress.
- The shell can navigate to a legacy route as a full-page transition while preserving a consistent authenticated entry point. Do not embed OFBiz pages in iframes; iframe isolation, clickjacking protection, accessibility, and session behavior make it unsafe and brittle.
- Modern pages use the new design system and APIs. Legacy pages are not rewritten merely to resemble the target shell unless required for a safe transition.
- Route rollout uses server-controlled feature flags and cohorts. Authorization is still enforced by the BFF and services; a feature flag is never an authorization control.

#### Secure modern-page marker

Every modern page must display a visible marker such as **Modern experience** in a fixed shell-owned position. It should also expose a stable test hook such as `data-runtime="modern"` on the shell root. The marker supports users, testers, support staff, and migration telemetry.

The marker is trustworthy because:

- It is rendered only by the trusted modern shell from the server/build-controlled route catalog.
- It is not derived from a query parameter, URL fragment, API response field, arbitrary HTML, or a header supplied by a downstream service.
- Legacy responses cannot set the marker. The edge strips any reserved architecture-marker headers from upstream responses.
- A strict Content Security Policy, no unsafe inline script, output encoding, dependency controls, and Trusted Types where supported reduce injection risk.
- The marker reveals no internal hostname, version, deployment slot, user privilege, or sensitive topology detail.
- End-to-end tests assert that every `modern` route shows exactly one marker and every `legacy` route shows none. The route catalog test fails closed for unknown routes.
- Telemetry records the route classification and release version from trusted shell metadata, enabling adoption and error-rate comparison without trusting DOM text.

This marker is informational, not a security boundary. Authentication and authorization remain mandatory regardless of the label.

### 3.7 Identity and security target

- Front Door WAF blocks common attacks, enforces TLS, and rate-limits abusive clients. Direct origins reject public bypass.
- Entra ID handles user authentication with Authorization Code + PKCE. The BFF pattern keeps access/refresh tokens out of browser storage and uses `Secure`, `HttpOnly`, appropriately scoped `SameSite` cookies plus CSRF protection.
- APIM and each service validate token issuer, audience, signature, expiry, and required scopes/roles. Network location alone never grants trust.
- Services use managed identities for Service Bus, Key Vault, App Configuration, Blob Storage, monitoring, and supported database authentication. Permissions are scoped per service.
- A temporary identity bridge maps an Entra subject to an OFBiz user and creates only the minimum legacy session/claims needed. Modern bearer tokens and legacy session cookies are not forwarded indiscriminately between backends.
- Authorization policy starts by mapping existing OFBiz permissions, then moves to domain-owned app roles and resource checks. Deny-by-default and separation-of-duty tests are required, especially for accounting and administration.
- Secrets never enter Git, Terraform variables files, images, logs, or browser bundles. Key Vault references and rotation procedures are tested.
- Images run non-root, use read-only filesystems where feasible, publish SBOM/provenance, and are scanned before promotion. Production deploys immutable digests.
- Audit events are append-only, access-controlled, correlated to actor and request, and retained according to business/legal policy. Sensitive fields are minimized and redacted from ordinary logs.

### 3.8 Monorepo target layout

```text
applications/                  existing OFBiz applications during migration
framework/                     existing OFBiz framework during migration
themes/                        existing OFBiz themes during migration
services/
  <bounded-context>/
    src/                       service and worker implementation
    db/                        owned schema migrations
    api/                       OpenAPI/event schemas
    tests/                     unit, integration and contract tests
    Dockerfile
web/
  shell/                       navigation, marker, auth and route composition
  features/<bounded-context>/  modern vertical UI slices
  design-system/
contracts/
  events/                      versioned shared event schemas
  external-api/                externally supported API specifications
platform/
  libraries/                   thin security/telemetry/error libraries
  legacy-adapters/             anti-corruption and identity/data bridges
infra/
  modules/                     reusable Terraform modules
  environments/dev/
  environments/test/
  environments/stage/
  environments/prod/
  bootstrap/                   remote-state and federation bootstrap
local-dev/
  compose.yaml                 OFBiz, modern services and local dependencies
  config/                      non-secret local defaults
  seed/                        deterministic test fixtures
  smoke-test.sh
migration/
  reconciliation/              comparison jobs and reports
  cutovers/                    versioned cutover and rollback runbooks
docs/
  architecture/                ADRs, context map, service catalog and SLOs
modern                         cross-platform developer entry script
```

The exact service implementation stack should be decided by an ADR after a pilot. Standardizing on one or two supported stacks is preferable to unrestricted polyglot development. Every buildable module is registered explicitly with the root Gradle build; the current OFBiz-only component discovery is not sufficient for modern services or UI modules. Repository ownership rules, Gradle incremental builds, the build cache, and optional path-filtered fast feedback keep the monorepo manageable, but the authoritative CI gate still executes the complete root build.

### 3.9 Local development target

`./gradlew build` is the canonical build entry point on developer machines and CI. It compiles, tests, packages, and validates OFBiz, every modern service/BFF, the modern UI and design system, generated contracts, and Terraform. It is hermetic: it does not require an Azure subscription, apply infrastructure, start long-running services, or require Docker merely to compile and test code that does not intrinsically need it.

The repository also provides a versioned `./modern` command (with a Windows equivalent or portable implementation) for integrated local-runtime operations:

```text
./modern bootstrap       verify/install documented prerequisites
./modern build           delegate unchanged to ./gradlew build
./modern up              start the integrated local stack
./modern smoke           verify login, legacy route, modern route and dependencies
./modern test            run affected unit/integration/contract tests
./modern down            stop the stack without deleting data
./modern reset           explicitly remove local state and reseed after confirmation
```

Docker Compose runs OFBiz, the shell/BFF, migrated services, PostgreSQL, a Service Bus-compatible local transport or adapter, Redis-compatible cache, blob emulator, and an OpenTelemetry collector. Local TLS and a hosts entry provide one origin matching production routing semantics. Testcontainers isolate service integration tests and apply real database migrations. External Azure dependencies sit behind narrow ports so an emulator can be replaced by a real Azure resource in optional hybrid testing.

Local mode must reproduce authentication and authorization behavior. It may use a development OIDC provider and seeded users, but bypass-auth flags are forbidden in committed application paths. Local secrets are generated, ignored, and never production-compatible.

Gradle pins and invokes Node.js, the selected JavaScript package manager, API/schema generators, Terraform validation tooling, and other required build tools. A clean build must not depend on undocumented globally installed versions. `./modern up` consumes artifacts or images produced from this same Gradle build graph so local execution cannot drift from CI.

The first build materializes any missing required lockfile through an idempotent Gradle lifecycle task. Subsequent builds resolve in strict/frozen mode and do not silently select newer versions. Developers use an explicit Gradle update task when changing dependency declarations; that task regenerates affected lockfiles for review. CI runs generation plus verification and fails if the working tree would change, ensuring generated locks are committed rather than existing only in an artifact.

## 4. Delivery and infrastructure model

### 4.1 Terraform structure and controls

- Bootstrap creates the Terraform state storage account, versioned private container, locking mechanism supported by the selected backend, encryption, RBAC, and GitHub OIDC federation. Bootstrap is intentionally separate from workload state.
- Reusable modules provision naming/tags, network/private DNS, Container Apps environments/apps/jobs, Front Door/WAF, APIM, ACR, PostgreSQL, Service Bus, Key Vault, App Configuration, Redis, Blob Storage, monitoring, alerts, budgets, and CI identities.
- Each environment has a root module and a separate state. Production is isolated in its own subscription and uses approval-protected deployment environments.
- Provider and module versions are pinned. Each Terraform root commits its `.terraform.lock.hcl`, generated for every supported CI/deployment platform. Initialization during a normal build uses read-only lock mode. `fmt`, `validate`, linting, security/policy checks, and a saved plan run on pull requests.
- Apply occurs only from a protected CI environment using OIDC. The reviewed plan artifact is the artifact applied; production requires approval and change evidence.
- Database/schema migrations run as versioned, backward-compatible deployment jobs. Terraform does not execute application DDL.
- Drift detection runs on a schedule. Import or reconciliation is required for intentional portal changes; routine manual mutation is prohibited.
- Destructive plans, public network exposure, broad role assignments, unencrypted state/data, missing diagnostic settings, and unbounded high-cost SKUs fail policy checks.

### 4.2 Unified build and SonarQube analysis

The root Gradle lifecycle is the repository's executable build contract:

- `./gradlew build` builds and verifies the entire checked-out codebase, including legacy OFBiz, all modern JVM or non-JVM services, BFFs, TypeScript UI packages, generated API/event contracts, database migrations, and Terraform formatting/validation.
- Modern JVM modules are Gradle subprojects or included builds connected to the root `build` lifecycle. Non-JVM modules expose Gradle lifecycle tasks that invoke pinned package-manager/compiler/test commands and declare correct inputs, outputs, and dependencies.
- Root `build` depends on unit tests, static checks, contract/schema compatibility, architecture rules, UI accessibility checks that can run without a deployed environment, and IaC validation. Cloud deployment and destructive or environment-dependent integration tests remain separate tasks.
- `./modern build` is only a convenience alias for `./gradlew build`; it must not add, omit, reorder, or independently implement build steps.
- Path-aware jobs may provide earlier feedback, but the required merge gate runs the complete `./gradlew build`. Caching and incremental execution optimize that command without weakening its scope.

Dependency locking is part of that contract:

- A root `generateLockfiles` lifecycle task invokes ecosystem-native generators for all registered modules. It creates missing locks idempotently but does not upgrade versions already locked.
- A root `verifyLockfiles` task checks that every dependency manifest has its required lockfile, the lock represents the manifest, normal resolution uses strict/frozen mode, and generation leaves the Git worktree unchanged. Root `build` depends on both tasks in the required order.
- Gradle dependency locking is enabled for all resolvable configurations in the root project, modern JVM subprojects, and included builds. The resulting `gradle.lockfile` files are committed.
- JavaScript/TypeScript workspaces commit the selected package manager's single canonical lockfile, such as `package-lock.json` or `pnpm-lock.yaml`, and build with immutable/frozen install flags. Multiple competing package-manager locks are forbidden.
- Every Terraform root commits `.terraform.lock.hcl`; the Gradle task generates provider checksums for supported developer and CI/deployment platforms and validates with backend access disabled.
- Any future ecosystem must choose and commit its native lock format before its module joins the build. Container base images and CI actions are pinned by immutable digest or commit in addition to dependency lockfiles.
- An explicit `updateLockfiles` task is the only supported path for intentional lock refreshes. Dependency and lockfile changes appear in the same pull request and pass vulnerability, license, compatibility, and full-build checks.
- Lockfiles contain only package coordinates, versions, sources, and integrity hashes; credentials, private registry tokens, and environment-specific secrets are prohibited.

The existing single-project SonarQube flow remains authoritative. The root `sonar` task analyzes the whole monorepo in one project and must depend on, or be invoked with, all compilation, test, and coverage-report tasks needed for accurate analysis. As directories are introduced, `sonar.sources` is expanded from the current OFBiz-centric list to include at least `services`, `web`, `platform`, `contracts`, `infra`, `local-dev`, `migration`, `docs`, root build logic, and the `modern` launcher. Test-source classification, generated-code exclusions, Java binaries, JaCoCo XML, JavaScript/TypeScript LCOV, and other supported report paths are configured explicitly.

The required CI analysis command remains compatible with today's workflow:

```text
./gradlew build jacocoTestReport sonar
```

Additional language-specific coverage aggregation may be attached to `build` or `sonar`, but it must not require a separate manually maintained CI build sequence. A CI test verifies that every registered deployable source directory is covered by both the root build graph and the Sonar source configuration, preventing newly added modules from being silently omitted.

### 4.3 Build, release, and promotion

1. Path-aware CI may calculate affected components for fast feedback, while the authoritative merge gate builds and analyzes the complete repository through the root Gradle tasks.
2. Each deployable produces an image, SBOM, provenance/attestation, test evidence, vulnerability result, and immutable digest.
3. The same digest is promoted through test, stage, and production; it is not rebuilt per environment.
4. Container Apps revisions provide canary/blue-green rollout. Front Door/APIM routing and feature flags control user cohorts for a migrated slice.
5. Automated post-deploy tests cover authentication, authorization, data reconciliation, event health, legacy fallbacks, modern markers, and core business flows.
6. Rollback first restores traffic to the previous compatible revision. Database changes use expand/contract so the prior revision remains runnable.

### 4.4 Service-level operations

Each production service has an owner, on-call route, dashboard, runbook, dependency list, data classification, recovery objectives, SLOs, alerts, capacity assumptions, cost tags, and tested backup/restore process. Initial common service-level indicators are availability, successful business-operation rate, latency, saturation, queue age, dead-letter count, reconciliation errors, and data freshness.

## 5. Migration phases

Phases overlap where their entry criteria are satisfied. UI and service work for a business slice always travel together. Each phase ends with a deployable, supportable system and an explicit go/no-go review.

### Phase 0 — Baseline, discovery, and governance

**Goal:** make hidden coupling and required behavior visible before selecting extraction boundaries.

Work:

- Catalogue OFBiz web routes, controllers/events, services, entity reads/writes, ECAs, scheduled jobs, RMI/REST endpoints, reports, file stores, email flows, and external integrations.
- Build a context map and dependency heat map from runtime telemetry, metadata, source navigation, database access, and stakeholder workflows.
- Record critical user journeys, volume/latency/error baselines, month/quarter-end behavior, regulatory controls, RPO/RTO, and operational pain points.
- Identify authoritative records, transaction boundaries, invariants, and current ownership for each candidate domain.
- Define architecture decision records for compute, implementation stack, API/event conventions, database strategy, local emulation, identity, and UI composition.
- Create service, API, event, route, and data-ownership catalogs with named owners.
- Define migration scorecards and prioritize candidate slices by business value, coupling, risk, write complexity, and reversibility.
- Establish coding, threat-modeling, accessibility, test, observability, and production-readiness standards.

Exit criteria:

- Every critical route and scheduled/integration flow has an owner and dependency record.
- Candidate boundaries and the first pilot slice have evidence-based ADRs.
- Current performance, correctness, security, and operational baselines are measurable.
- The first slice has explicit invariants, rollback boundaries, and acceptance tests.

Project-specific exit decision dated 2026-09-25: the product-catalog read pilot is explicitly validated in `docs/architecture/pilot-scorecard.md`, completing Phase 0. Named-person ownership acceptance beyond the pilot approver, exhaustive runtime validation, and populated production baselines are waived for Phase 0 exit only. They remain mandatory just-in-time inputs to the readiness gate of every production slice that depends on them; the waiver does not relax security, correctness, compliance, data ownership, reconciliation, rollback, or operational-readiness requirements.

### Phase 1 — Monorepo build and local platform foundation

**Goal:** enable modern code to be developed and tested beside OFBiz without changing production traffic.

Work:

- Add the target monorepo directories, ownership rules, dependency boundaries, explicit Gradle module registration, and complete-build CI, with affected-project jobs only as optional fast feedback.
- Extend the root `build` lifecycle to compile, test, package, and validate OFBiz, the shell/BFF, the reference service, contracts, and Terraform; make `./modern build` delegate to it.
- Add root `generateLockfiles`, `verifyLockfiles`, and `updateLockfiles` tasks; enable strict Gradle locks and frozen JavaScript/Terraform dependency resolution.
- Expand the root SonarQube configuration and coverage aggregation to include every introduced source tree while preserving the existing single-project CI invocation.
- Implement `./modern up|smoke|test|down` and Compose-based local dependencies.
- Containerize a minimal shell/BFF and one reference service with health/readiness endpoints and OpenTelemetry.
- Add contract, integration, end-to-end, accessibility, and infrastructure test harnesses.
- Create schema migration, outbox/inbox, idempotency, structured error, correlation, and security middleware reference implementations.
- Ensure a clean machine can run `./gradlew build`, start OFBiz plus the reference service, visit one legacy and one reference route, and shut down using documented commands.

Exit criteria:

- CI and a clean developer machine both build all legacy and modern code successfully with `./gradlew build`.
- The existing `./gradlew build jacocoTestReport sonar` CI shape analyzes all current source directories and consumes all supported test and coverage reports.
- A guard test fails if a deployable module is absent from either the Gradle build graph or SonarQube scope.
- A clean checkout generates no uncommitted lockfile diff, while deleting any required lockfile causes `./gradlew build` to recreate it locally and fail CI until it is committed.
- Intentional dependency updates regenerate only the affected committed lockfiles through `./gradlew updateLockfiles` and pass dependency policy checks.
- No Azure account is required for the default local workflow.
- Logs and traces correlate a browser request through shell/BFF/reference service.
- Repository guardrails prevent forbidden cross-service database/code dependencies.

### Phase 2 — Azure landing zone and continuous delivery

**Goal:** create secure, repeatable non-production Azure environments before deploying business workloads.

Work:

- Implement Terraform bootstrap, remote state, subscription/environment separation, naming, tags, budgets, and OIDC deployment identities.
- Provision network/private DNS, ACR, Container Apps, Front Door/WAF, APIM, Key Vault, App Configuration, PostgreSQL, Service Bus, Blob Storage, Redis, and monitoring modules.
- Deploy the reference service through test and stage with private service endpoints, managed identities, dashboards, alerts, backup, and restore tests.
- Add policy-as-code, drift checks, image/IaC scanning, immutable digest promotion, canary deployment, and automated rollback.
- Document break-glass access and prove that origins cannot be reached by bypassing the edge.

Exit criteria:

- A reviewed Terraform plan creates an environment from scratch and a destroy rehearsal succeeds in a disposable environment.
- Test and stage are reproducible with no manually created workload resource.
- Security tests validate private origins, identity, RBAC, secret access, WAF, audit logging, and restore procedures.
- Platform SLOs, alerts, costs, and operational ownership are visible.

### Phase 3 — Edge strangler, identity bridge, and UI shell

**Goal:** put legacy and modern traffic behind one controlled entry point without changing business ownership.

Work:

- Route all user traffic through Front Door/WAF and APIM where applicable; place OFBiz behind the edge with direct access blocked.
- Introduce Entra sign-in and the temporary OFBiz identity/session bridge. Map existing permissions and test deny-by-default behavior.
- Deliver the shell, BFF, navigation, design system baseline, CSP, CSRF protection, route catalog, feature flags, accessibility tests, and telemetry.
- Implement the secure **Modern experience** marker and fail-closed route/marker test suite.
- Register existing OFBiz routes as `legacy`; introduce a harmless authenticated modern diagnostics/profile route as `modern`.
- Exercise cohort routing, canary, failback, session expiry, logout, deep links, and mixed legacy/modern navigation.

Exit criteria:

- Users access legacy and modern routes through one origin and one sign-in journey.
- Unknown routes fail closed; edge bypass and marker spoofing tests fail as expected.
- Modern pages always show the marker and legacy pages never show it.
- Traffic can return to legacy without redeploying either application.

### Phase 4 — First vertical slice: product catalog read path

**Goal:** prove the complete strangler method on a useful, reversible, predominantly read-oriented capability.

Work:

- Confirm product catalog browsing is suitable after Phase 0 evidence; select a different low-risk read slice if it is not.
- Define the catalog API, BFF contract, event/schema conventions, SLOs, and modern catalog UI pages.
- Build an anti-corruption adapter around OFBiz behavior; do not expose OFBiz entities as the modern API model.
- Populate the catalog service store from legacy data, stream deltas, and continuously reconcile keys, attributes, categories, prices shown, and freshness.
- Run shadow reads and compare results without affecting users.
- Enable modern catalog pages for internal users, then cohorts, then all users. Display the modern marker and retain instant route rollback.
- Measure usability, accessibility, latency, errors, support contacts, reconciliation, and infrastructure cost.

Exit criteria:

- Catalog read invariants reconcile within an agreed threshold for the full observation window.
- The modern UI meets functional, security, accessibility, performance, and support criteria.
- Production traffic is on the modern read path with tested rollback.
- The pilot retrospective updates the reference architecture and extraction playbook.

### Phase 5 — First ownership transfer: product/catalog writes

**Goal:** prove safe transfer of system-of-record responsibility.

Work:

- Implement modern product administration UI, command APIs, validation, authorization, audit, idempotency, and database migrations.
- Inventory every legacy consumer of product data. Replace direct dependencies with APIs/events or a temporary legacy compatibility projection.
- Introduce transactional outbox events and consumer inboxes; test duplicates, reordering, poison messages, and replay.
- Rehearse cutover from a production-like snapshot, including write pause, final delta, reconciliation, switch, smoke test, and rollback/forward-recovery decision points.
- Move product writes to the modern service for cohorts, then globally. Block old write paths at both UI and server levels.
- Observe through a full business cycle, then stop reverse projection when no legacy consumer remains.

Exit criteria:

- The catalog service is the only product writer and owns its data store.
- No modern service directly queries OFBiz product tables.
- All remaining legacy product consumers use an explicit compatibility contract.
- Legacy product write UI/services are disabled and scheduled for removal.

### Phase 6 — Repeatable domain waves

**Goal:** migrate remaining domains using a factory-like, evidence-driven slice loop rather than parallel rewrites.

For every slice, execute this loop:

1. Discover routes, callers, data, ECAs, jobs, integrations, reports, controls, and invariants.
2. Define the bounded context, API/events, UI routes, ownership state, SLOs, threat model, and rollback point.
3. Build the service, owned store, BFF/API, modern UI, marker registration, telemetry, and operations.
4. Backfill, shadow, reconcile, load-test, security-test, and run failure/recovery exercises.
5. Canary reads; then transfer writes using the ownership state machine.
6. Observe through the relevant business cycle.
7. Remove the legacy route, write path, ECA/job, compatibility code, and tables only when no verified consumer remains.

Suggested waves, subject to Phase 0 evidence:

- **Wave A:** content/media, notification, work management, and party/customer read slices.
- **Wave B:** party/customer ownership, pricing/promotions, and inventory visibility.
- **Wave C:** inventory reservation/movement, order capture and order lifecycle.
- **Wave D:** fulfillment/shipping and manufacturing.
- **Wave E:** invoicing, payments, taxation, ledger, financial reporting, and remaining administration.

Concurrency rules:

- Multiple teams may migrate slices concurrently only when their data ownership and event contracts do not conflict.
- One team owns each route, entity set, API, and event schema at a time.
- A domain UI route cannot be declared modern before its production BFF/service path and fallback are operational.
- Shared platform changes are versioned and backward compatible; they do not require lock-step service releases.
- Limit work in progress so operations can support every active dual-run and reconciliation path.

Exit criteria for each wave:

- Modern routes, API volume, and owned-record percentages meet the wave target.
- No unresolved reconciliation, security, availability, or audit finding remains.
- Legacy compute/database usage measurably falls, and retired paths are removed rather than merely hidden.
- On-call teams have runbooks and have completed a failure/restore exercise.

### Phase 7 — Reporting, integration, and shared-runtime removal

**Goal:** eliminate residual dependencies that keep migrated domains coupled to OFBiz.

Work:

- Replace cross-domain SQL reports with event-fed operational read models and an analytics platform appropriate to reporting latency.
- Move scheduled jobs to domain-owned Container Apps Jobs/workers with idempotency, leader control where needed, and observable execution.
- Replace remaining file, email, RMI, direct-database, and batch integrations with owned adapters and supported APIs/events.
- Remove global shared libraries containing business rules; move rules to their owning domains.
- Migrate static/media storage to Blob Storage and test retention, legal hold, and disaster recovery.
- Prove no modern service requires the OFBiz classpath, Entity Engine, Service Engine, widget metadata, or legacy database at runtime.

Exit criteria:

- All integrations and reports have named modern owners and supported contracts.
- No live modern workload reads the OFBiz database or invokes an OFBiz in-process service.
- Remaining OFBiz routes and jobs are explicitly listed with approved retirement dates.

### Phase 8 — OFBiz retirement and optimization

**Goal:** remove the legacy runtime safely and simplify the transitional platform.

Work:

- Complete the final accounting/administrative cutovers and required financial close observation periods.
- Freeze legacy changes, export required audit/history data, prove retention and restore access, and take final backups.
- Route all supported paths to modern workloads; monitor for attempted legacy access and unknown consumers.
- Run a time-boxed dark period with OFBiz stopped but recoverable, then obtain business, security, finance, and operations sign-off.
- Remove OFBiz routing, identity bridge, replication, compatibility projections, legacy database access, images, secrets, infrastructure, and temporary feature flags.
- Simplify Terraform and local Compose; archive rather than silently discard legally required artifacts.
- Rebaseline SLOs, capacity, cost, recovery, security posture, and team ownership for the all-modern system.

Exit criteria:

- OFBiz is absent from production request paths and scheduled processing.
- No production credential, network rule, service, report, or operator procedure depends on OFBiz.
- Final backups and audit exports satisfy retention and tested retrieval requirements.
- The decommission reduces cost and attack surface without an SLO regression.

## 6. Slice readiness and completion gates

A slice may enter production canary only when all readiness items are true:

- Business owner, technical owner, on-call owner, and data owner are named.
- Legacy dependencies, ECAs, scheduled jobs, integrations, reports, and permissions are mapped.
- API/event schemas and compatibility rules are reviewed; consumer contracts pass.
- Data classification, threat model, authorization tests, and privacy requirements are complete.
- Backfill and reconciliation are repeatable and produce auditable reports.
- Dashboards, SLOs, alerts, dead-letter handling, backups, and restore runbooks are exercised.
- UI meets design-system, accessibility, responsive, security, and modern-marker requirements.
- Load, resilience, deployment, rollback, and forward-recovery tests pass.
- Terraform and deployment changes have reviewed plans and no manual prerequisites.

A slice is complete only when:

- Its modern service is the declared system of record where ownership was in scope.
- Its modern UI routes carry production traffic and the marker is correct.
- Remaining legacy consumers use explicit temporary contracts with retirement dates.
- Obsolete legacy routes, writes, jobs, ECAs, and data access are removed.
- The observation window covers the slice's relevant business cycle.
- Documentation, runbooks, diagrams, ownership catalogs, and support training are current.

## 7. Success measures

Track migration by capability and risk reduction, not lines of code or number of services:

- Percentage of user journeys and production requests on modern marked routes.
- Percentage of business writes owned by modern services.
- Number of OFBiz routes, services, jobs, ECAs, direct database consumers, and tables still active.
- Reconciliation error count, replication lag, dead-letter age, and replay success.
- Lead time, deployment frequency, change-failure rate, and recovery time per service.
- Availability, latency, business-operation success, security findings, and accessibility defects versus baseline.
- Developer bootstrap time and success rate for `./modern up` and smoke tests.
- Clean and incremental `./gradlew build` success rate/duration, plus the percentage of deployable modules covered by the root build and single SonarQube analysis.
- Azure cost per business transaction and idle/non-production cost.
- Number and age of temporary adapters, reverse projections, feature flags, and exemptions.

Set quarterly outcome targets for these measures. Do not reward service count: unnecessary service boundaries increase failure modes and operating cost.

## 8. Principal risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Hidden ECAs, jobs, or metadata dependencies cause behavior loss | Phase 0 metadata/runtime inventory, golden business-flow tests, shadow execution, and full-cycle observation. |
| Shared transactions are split incorrectly | Identify invariants first; keep strongly consistent rules in one context and use explicit sagas elsewhere. |
| Data diverges during ownership transfer | Single-writer rule, outbox/CDC, idempotent consumers, continuous reconciliation, rehearsed cutovers, and explicit recovery boundaries. |
| The result becomes a distributed monolith | Enforce database ownership, stable contracts, dependency rules, independent release tests, and bounded synchronous call depth. |
| UI becomes inconsistent or insecure | One shell/design system/BFF, same-origin routing, CSP/CSRF controls, accessibility gates, and shell-owned marker. |
| Marker can be forged or exposes internals | Render from trusted route metadata, strip reserved headers, prohibit content-derived labels, and test positive/negative routes. |
| Identity bridge broadens privilege | Short-lived mapping, least privilege, deny-by-default tests, audit, no token/cookie forwarding, and a retirement milestone. |
| Too many simultaneous dual-run paths overwhelm operations | WIP limits, wave gates, named owners, automated reconciliation, and time limits for transitional components. |
| Azure dependence blocks developers | Compose/Testcontainers defaults, narrow platform ports, deterministic seed data, and optional hybrid tests rather than mandatory cloud access. |
| Dependency resolution drifts between machines or over time | Ecosystem-native committed locks, strict/frozen resolution, idempotent generation in the root build, CI worktree verification, immutable image/action pins, and an explicit reviewed update task. |
| Terraform drift or unsafe changes damage production | OIDC-only applies, protected environments, saved plans, policy gates, isolated state, backups, locks, and scheduled drift detection. |
| Costs grow unnoticed | Budgets/alerts/tags, scale-to-zero for suitable non-production workloads, capacity tests, and cost-per-transaction tracking. |
| Accounting migration violates audit/control requirements | Migrate last, involve finance/compliance early, preserve immutable audit evidence, parallel close, and require formal sign-off. |

## 9. Immediate next actions

1. Approve or amend the Azure platform ADRs, especially Container Apps versus AKS, APIM topology, PostgreSQL isolation, and local Service Bus substitute.
2. Execute Phase 0 inventory and produce the context map, route catalog, data-ownership map, critical-flow baseline, and first-slice scorecard.
3. Validate product catalog read as the pilot; choose the best-scoring alternative if discovery disproves the assumption.
4. Create the Phase 1 repository skeleton, register every new module in the root Gradle lifecycle, implement and seed repository-wide lockfile tasks, expand the single-project SonarQube scope, and implement `./modern` as a runtime helper without changing production routing.
5. Define measurable Phase 2 landing-zone acceptance tests and establish Azure subscriptions, Entra groups, budgets, and CI OIDC trust.
6. Build the edge/identity/shell proof of concept, including automated anti-spoof tests for the modern-page marker.
7. Re-estimate later waves after the first read cutover and again after the first write-ownership transfer; those two pilots provide the most reliable migration throughput evidence.
