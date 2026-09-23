# OFBiz microservices migration plan

## 1. Purpose

This plan incrementally replaces the current OFBiz modular monolith with a secure, Azure-hosted microservices architecture managed with Terraform. The repository remains a monorepo, and the complete system remains buildable and runnable on a developer machine throughout the migration.

The migration uses the **strangler pattern**. A stable edge routes traffic to either legacy OFBiz or modern implementations. Business capabilities and their user-interface routes move as tested vertical slices. OFBiz remains available for all behavior not yet migrated, and each slice has an explicit rollback path.

The UI is not a final rewrite phase. Every service migration wave includes the corresponding UI journeys. Users and testers can identify migrated pages through a consistent **Modern** marker whose value is based on trusted route/runtime metadata, not user-controlled input.

## 2. Outcomes and success measures

The program is complete when:

- business capabilities run as independently deployable services with exclusive ownership of their data;
- all customer and operator traffic enters through the Azure edge and API gateway;
- all production infrastructure is reproducible from reviewed Terraform;
- OFBiz business applications, shared tables, and migration adapters have been retired;
- the modern UI covers all retained journeys and no longer embeds or redirects to legacy pages;
- every service and UI can be built, tested, and started from the monorepo on a developer machine;
- production deployments are observable, secure by default, independently reversible, and do not require repository-wide releases;
- functional, financial, security, availability, latency, and data-reconciliation objectives are met.

Track at least these measures per migration wave:

| Dimension | Measure |
| --- | --- |
| Functional | Critical journeys passing against legacy baseline and modern implementation |
| Migration | Percentage of requests, UI routes, writes, reads, jobs, and owned tables served by modern components |
| Reliability | Availability, error rate, queue age, retry/dead-letter rate, and recovery time |
| Performance | Browser Core Web Vitals and end-to-end/API latency percentiles |
| Data | Reconciliation mismatch count, replication lag, and unresolved migration exceptions |
| Security | Critical/high findings, privileged-access reviews, and authentication/authorization failures |
| Delivery | Lead time, deployment frequency, change failure rate, and rollback time |
| Cost | Azure cost by service/environment and cost per representative business transaction |

## 3. Guiding principles

1. Migrate vertical business slices, not Java packages.
2. Keep one source repository while preserving independent build and deployment boundaries.
3. Give every service one accountable owner and exclusive write ownership of its data.
4. Prefer contracts over shared implementation: OpenAPI for synchronous calls and versioned schemas for events.
5. Use local ACID transactions inside a service; use sagas and compensating actions across services.
6. Never make a shared database the permanent integration mechanism between modern services.
7. Move the UI journey with its backend capability and expose migration status visibly.
8. Preserve one external origin and one identity/session experience during coexistence.
9. Make the secure path the easiest path through templates, CI policy, managed identities, and platform defaults.
10. Require measurable evidence and a rollback decision for every cutover.
11. Delete transitional code, routes, replicas, flags, and infrastructure after their rollback windows.

## 4. Target state

### 4.1 Logical architecture

```text
Users and partner clients
          |
Azure Front Door Premium + WAF + managed TLS
          |
Azure API Management (single public API origin)
          |
          +---------------- Browser routes ----------------+
          |                                                |
Modern web shell / route-owned UI                 Legacy OFBiz UI (temporary)
          |                                                |
          +--------------- API routes ---------------------+
                           |
              Azure Container Apps environment
  +----------------------------------------------------------------+
  | Identity/authorization | Party | Catalog | Pricing | Inventory |
  | Order | Fulfillment | Billing | Ledger | Manufacturing         |
  | Content | Work | Marketing | Notifications | Reporting         |
  +----------------------------------------------------------------+
        | synchronous REST where an immediate reply is required
        | asynchronous integration events and commands
        v
Azure Service Bus            Per-service PostgreSQL databases
        |                     Blob Storage / cache / search as needed
        +-----------------------------+
                                      v
                     OpenTelemetry -> Azure Monitor,
                     Application Insights and Log Analytics

Legacy OFBiz participates only through APIM and a temporary
anti-corruption layer until each route and data owner is cut over.
```

### 4.2 Azure platform choices

These are defaults to ratify with Architecture Decision Records (ADRs), not unchangeable product selections.

| Concern | Target default |
| --- | --- |
| Global edge, TLS, WAF | Azure Front Door Premium |
| API gateway and strangler routing | Azure API Management (APIM) |
| Service runtime | Azure Container Apps |
| Image registry | Azure Container Registry |
| Web delivery | Static Web Apps or static assets behind Front Door; choose through an ADR based on SSR/BFF needs |
| Operational data | Azure Database for PostgreSQL Flexible Server, with an independent database lifecycle per service |
| Messaging | Azure Service Bus Premium queues/topics |
| Secrets, keys, certificates | Azure Key Vault |
| Workload identity | Managed identities; OIDC workload federation for CI/CD |
| Configuration and migration flags | Azure App Configuration with auditable feature flags |
| Objects and documents | Azure Blob Storage |
| Cache | The Microsoft-recommended managed Redis-compatible service at implementation time, only where measured |
| Observability | OpenTelemetry, Application Insights, Log Analytics, Azure Monitor alerts/workbooks |
| Network isolation | VNet integration, private endpoints, private DNS, least-privilege network rules |
| Governance | Azure Policy, Defender for Cloud, budgets, mandatory tags and diagnostic settings |

Use Container Apps unless a documented requirement—such as specialized Kubernetes operators, privileged workloads, custom networking, or mandated portability—requires AKS. Do not operate Kubernetes merely to label the result “microservices.”

### 4.3 Candidate bounded contexts

The table is a discovery hypothesis. Context boundaries must be validated with domain experts, runtime traces, service/ECA calls, data ownership, and business transaction mapping. An OFBiz component does not automatically become one service.

| Context | Candidate ownership |
| --- | --- |
| Identity and Access | Federation links, application roles and permissions; Entra remains identity provider |
| Party | People, organizations, relationships, contact mechanisms and consent references |
| Product Catalog | Products, variants, categories, features and associations |
| Pricing and Promotions | Price lists, eligibility, rules and promotions |
| Inventory | Stock, reservations, facilities and availability |
| Order | Quotes/carts where retained, sales and purchase orders, order lifecycle |
| Fulfillment | Pick, pack, shipment and carrier interactions |
| Billing and Payments | Invoices, payment intents, captures, refunds and payment applications |
| Accounting and Ledger | Accounts, journals, periods, posting and financial controls |
| Manufacturing | BOM, routing, work orders and MRP |
| Work Management / HR | Work efforts, calendars, tasks and employment capabilities |
| Content | Content metadata, resources and publication |
| Marketing | Campaigns, segments and tracking policy |
| Notifications | Template-based email/SMS/webhook delivery and delivery state |
| Reporting and Analytics | Read-only projections and analytical models built from events |

Begin with a small but real vertical slice, such as notifications or a read-only catalog/invoice query. Do not use ledger posting, payment capture, or another irreversible high-risk process as the pilot.

### 4.4 Mandatory service boundaries

Every modern service must:

- expose versioned OpenAPI contracts and versioned event schemas;
- own its database objects; no other service or UI reads them directly;
- place domain/application logic behind inbound ports and persistence/integration behind outbound ports;
- authenticate the caller and authorize the action at the boundary;
- use managed identity for Azure resources and Key Vault references for secrets;
- publish integration events using a transactional outbox;
- make message handlers idempotent and define duplicate/reordering behavior;
- provide readiness, liveness, logs, metrics, traces, dashboards and actionable alerts;
- use explicit timeouts, bounded retries with jitter, circuit breakers and bulkheads where appropriate;
- use backward-compatible database migrations during rolling deployment;
- pass unit, integration, contract, security, migration and smoke tests;
- deploy and roll back without rebuilding or redeploying unrelated services.

Shared libraries may contain narrow cross-cutting utilities or generated contract clients. They must not contain shared domain models, database entities, business rules, or a replacement “enterprise framework” that recouples services.

### 4.5 Target UI architecture

Adopt a **modern web shell with route ownership**. The shell owns navigation, accessibility, localization, authentication state, error boundaries, telemetry, feature flags, and the modern-page marker. Feature areas own page modules aligned with bounded contexts. Prefer one deployable frontend initially; split into independently deployed micro-frontends only when independent teams and release cadence justify the operational and UX cost.

During coexistence, Front Door/APIM presents one public origin and routes:

- migrated browser routes to the modern shell;
- legacy browser routes to OFBiz;
- migrated API routes to modern services; and
- remaining API routes to OFBiz through an anti-corruption layer.

Do not iframe OFBiz as the default integration strategy. Same-origin route transitions or controlled full-page navigation preserve a clearer security boundary. If an iframe is temporarily unavoidable, require an explicit threat model, restrictive `frame-ancestors`, sandbox permissions, origin checks, and a removal date.

### 4.6 Required modern-page markers

Every page must make its implementation status clear to users and testers:

- Modern pages show a consistent badge such as **Modern**, plus an accessible text label, in a stable location in the shell.
- Legacy pages do not imitate the marker. Optionally add a distinct **Legacy** marker through the shared OFBiz theme during migration.
- The badge is available in normal environments, not only development. Production styling may be subtle but remains accessible.
- The page exposes a machine-readable marker, for example `data-runtime="modern"`, for automated tests.
- A details affordance may show UI build version, owning context, API implementation (`modern`, `legacy-adapter`, or `mixed`), and correlation ID. It must never expose secrets or internal hostnames.
- A **Mixed** state is allowed only for a time-boxed route whose UI is modern but still calls a legacy adapter. It must have an owner and expiry criterion.

The marker's source of truth is trusted deployment/route configuration supplied by the shell or a same-origin runtime manifest. It must not be derived from query parameters, arbitrary response content, or local storage. APIM and services add an internal implementation label to telemetry; the browser receives only an allowlisted representation. Content Security Policy and output encoding prevent badge/details injection.

End-to-end tests assert that each route's visible and machine-readable marker matches the gateway route catalog and that modern pages do not call unapproved legacy endpoints.

## 5. Monorepo and local developer experience

### 5.1 Target repository layout

```text
applications/                     # legacy OFBiz while migration is active
framework/                        # legacy OFBiz framework
themes/                           # legacy/shared OFBiz themes
services/
  <bounded-context-service>/
    src/
    api/openapi.yaml
    events/asyncapi.yaml
    db/migrations/
    Dockerfile
    service.yaml                  # owner, ports, dependencies, SLO and data classification
    README.md
web/
  shell/
  features/<bounded-context>/
  shared/                         # design system and generated clients only
contracts/
  api/
  events/
infra/
  bootstrap/                      # state storage and CI identity
  modules/
  environments/{dev,test,staging,prod}/
  policies/
local-dev/
  docker-compose.yml
  gateway/
  seed/
  scripts/
architecture/
  context-map.yaml
  data-ownership.yaml
  route-ownership.yaml
docs/adr/
```

The root build exposes stable commands that hide tool differences, for example `./gradlew buildAll`, `./gradlew testAll`, and a documented `local-dev` launcher. CI uses change detection to run affected builds while a scheduled full build proves repository-wide reproducibility.

### 5.2 Local runtime

Developers must be able to run a representative system without an Azure subscription:

- OFBiz and PostgreSQL for legacy behavior;
- each selected modern service and its database;
- a local Service Bus-compatible emulator when its semantics are sufficient, otherwise a clearly documented development namespace;
- a local edge/gateway that implements the same route catalog and modern/legacy decisions;
- the web shell and migrated feature modules;
- local OpenTelemetry collection and optional lightweight trace/log viewing;
- deterministic seed data, including linked legacy and modern identifiers.

Use Docker Compose for infrastructure and packaged services, while supporting an inner loop where one service or the UI runs from the host with hot reload. Provide one command to build/start the default stack, health-based readiness checks, profiles for individual slices, and one command to stop it without deleting developer data unless explicitly requested.

No local workflow depends on production secrets. Use development-only credentials, TLS certificates and identity configuration. Testcontainers should provide isolated integration dependencies in automated tests. Contract tests and gateway route tests must run without Azure.

## 6. Strangler mechanics

### 6.1 Edge routing

APIM is the decision point for API strangling; Front Door handles global ingress and WAF. Maintain a version-controlled `route-ownership.yaml` containing route, owner, implementation, allowed callers, rollout cohort and retirement condition. Generate or validate APIM configuration and local gateway routes against it.

Routing progresses independently for reads, writes and UI routes:

1. legacy only;
2. shadow/mirror safe read traffic;
3. modern for an internal cohort;
4. modern for a percentage or named tenant cohort;
5. modern by default with an emergency legacy fallback;
6. modern only; remove the legacy route after the rollback window.

Never mirror state-changing requests. Shadow writes execute only in an explicitly non-authoritative validation mode with side effects suppressed.

### 6.2 Anti-corruption layer

Create a temporary `ofbiz-adapter` at the modern boundary. It translates canonical contracts into OFBiz services and translates OFBiz maps, `GenericValue`, status identifiers and errors into stable domain contracts. It propagates identity, authorization context, trace context and idempotency keys, and emits migration telemetry.

The adapter cannot own business rules or data. Every route has an owner and deletion criterion. Modern services must not link OFBiz libraries or query OFBiz tables through the adapter.

### 6.3 Data transition

For each entity/table, maintain one target owner in `architecture/data-ownership.yaml`, with writers, readers, sensitivity, retention, volume and migration strategy. Use this sequence:

1. **Observe:** profile data and current readers/writers; establish reconciliation queries.
2. **Encapsulate:** prevent new direct access and place legacy behavior behind a contract.
3. **Backfill:** copy data into the service-owned store with repeatable, resumable tooling.
4. **Synchronize:** use an outbox or approved CDC mechanism; measure lag and failures.
5. **Shadow:** compare modern calculations and reads without making them authoritative.
6. **Switch writes:** make the modern service authoritative; temporarily project required facts back to legacy.
7. **Switch reads:** point UI and consumers to modern APIs/read models.
8. **Retire:** stop replication, revoke access, archive/delete legacy data under retention policy, and remove code.

Avoid dual writes. Database-plus-broker changes use a transactional outbox. Cross-service processes use explicit sagas with timeouts, compensations and manual-recovery states. Reporting joins are served from event-built projections, not cross-service operational queries.

### 6.4 Identity and session coexistence

Establish Microsoft Entra ID (and External ID where required) as the identity provider early. Use OAuth 2.0/OIDC authorization code flow with PKCE for browser clients and workload identities/client credentials for service calls. Prefer a backend-for-frontend (BFF) with secure, `HttpOnly`, `Secure`, `SameSite` cookies when it materially reduces token exposure.

During coexistence, use a narrowly scoped bridge so OFBiz trusts the same authenticated identity. Map legacy permissions explicitly and fail closed; do not forward reusable bearer tokens to arbitrary legacy pages. Preserve CSRF protection for cookie-authenticated operations, enforce CSP and security headers at the edge/application, rotate sessions on privilege change, and audit impersonation and administrator actions.

## 7. Delivery phases

Each phase has an evidence-based exit review. Waves may overlap where their dependencies permit, but no phase may bypass its safety gates.

### Phase 0 — Mobilize and baseline

Deliverables:

- program charter, named product/platform/security/data owners and funding model;
- critical journey inventory, compliance scope, recovery objectives and production SLO baseline;
- ADRs for Container Apps versus AKS, service runtime, API/event style, data platform, identity, UI composition, edge and repository build strategy;
- architecture decision and threat-model templates;
- initial risk register, cost allocation tags and migration dashboard;
- intended-architecture model for new service and UI boundaries.

Exit criteria:

- owners approve the target principles and measurable outcomes;
- every critical business journey has a business owner and baseline;
- the first discovery/pilot scope is funded and has no unresolved platform blocker.

### Phase 1 — Discover boundaries and characterize behavior

Map Java/Groovy calls plus XML entities, services, controllers, widgets, ECA rules and jobs. Combine static analysis with production traces, database access evidence, event storming and domain workshops.

Deliverables:

- context map, service-call inventory and UI route/journey inventory;
- machine-readable data and route ownership catalogs;
- sequence diagrams for critical transactions and failure paths;
- characterization tests around current outputs, permissions and side effects;
- data-quality/volume profile and extraction scorecard;
- selected pilot vertical slice, including its UI route and marker state.

Exit criteria:

- pilot-owned data, contracts, consumers, security model and rollback path are understood;
- characterization tests protect its critical behavior;
- the selected slice is valuable, reversible and not a disguised framework-only exercise.

### Phase 2 — Build the Azure and Terraform foundation

Create reusable Terraform modules for resource groups, network/private DNS, Container Apps, ACR, APIM, Front Door, PostgreSQL, Service Bus, Key Vault, App Configuration, observability, policy and budgets.

Requirements:

- pin Terraform and provider versions and commit dependency locks;
- bootstrap remote encrypted state separately, with private access, versioning and recovery procedures;
- isolate state and deployment identities per environment;
- use OIDC federation for CI/CD and least-privilege managed identities;
- run `fmt`, `validate`, lint, security, policy and plan checks on pull requests;
- require reviewed protected applies and retain plans/evidence;
- prohibit secrets in variables, state outputs, repository and logs;
- test modules with disposable environments and verify destroy behavior;
- enforce regions, SKUs, tags, diagnostics, TLS, private endpoints and public-access restrictions through policy.

Exit criteria:

- CI can create, smoke-test and destroy an ephemeral environment;
- staging/production networking, identity, diagnostics, budgets and recovery are tested;
- drift detection and emergency change reconciliation are documented.

### Phase 3 — Establish the engineering golden path

Build a thin reference service, modern web shell and local-dev stack.

Deliverables:

- service template with ports/adapters, OpenAPI, event schemas, migrations, outbox, idempotency and telemetry;
- shell with Entra integration, route-level authorization, design system, CSP, accessibility and Modern/Mixed marker;
- contract generation/validation and compatibility checks;
- local gateway and route catalog shared conceptually with APIM;
- Docker Compose stack and root build/start/test commands;
- CI templates for service/UI/container/Terraform pipelines, SBOMs, signing and vulnerability scanning;
- dashboards, alert templates, runbooks and ownership metadata.

Exit criteria:

- a fresh workstation can build/test/start OFBiz, reference service, shell, gateway, databases and messaging from documented commands;
- an ephemeral Azure deployment passes the same contracts and smoke journey;
- security review and developer usability review pass.

### Phase 4 — Introduce the strangler edge and identity

Put Front Door/APIM in front of legacy OFBiz without changing behavior first. Deploy the anti-corruption layer and shared identity bridge. Introduce the version-controlled route catalog and feature/cohort controls.

Deliverables:

- one public origin, WAF, managed TLS and deny-by-default backend exposure;
- APIM policies for authentication, authorization context, rate limits, correlation and safe error handling;
- modern shell route with navigation to legacy routes;
- optional Legacy marker added through the common OFBiz theme;
- route-to-marker conformance tests and end-to-end session transition tests;
- tested emergency routing and rollback runbook.

Exit criteria:

- existing journeys work through the new edge with no material regression;
- direct public access to origins is blocked;
- modern/legacy route decisions and marker values are observable and auditable;
- identity, CSRF, logout, privilege-change and session-transition tests pass.

### Phase 5 — Pilot one complete vertical slice

Extract the selected low-risk capability with its data, API/events, UI and operations. Use shadow reads and reconciliation before authority changes.

Required sequence:

1. define canonical contracts and owning data;
2. build service and modern feature UI behind disabled flags;
3. backfill and continuously reconcile the modern store;
4. shadow safe reads and compare results;
5. enable staff/test cohorts; show **Mixed** while legacy dependencies remain;
6. switch authoritative reads/writes in controlled cohorts;
7. show **Modern** only when the route satisfies its modernity definition;
8. remove fallback, adapter route and legacy implementation after the rollback window.

Exit criteria:

- service, UI route and data owner are independently deployable and authoritative;
- SLO, security, accessibility, performance and reconciliation targets hold under load;
- rollback and restore have been exercised;
- pilot lessons update templates and following waves.

### Phase 6 — Repeat domain migration waves

Run a rolling portfolio of vertical slices, limiting concurrent work to what platform, data and domain owners can safely support. A likely dependency-informed order is:

1. notifications and read-only projections;
2. party/contact and product catalog read journeys;
3. pricing, availability and inventory reservations;
4. order capture and lifecycle;
5. fulfillment/shipping;
6. invoicing and payments;
7. accounting/ledger after upstream event contracts stabilize;
8. manufacturing, work/HR, content and marketing according to business priority;
9. reporting rebuilt from governed event projections.

For every wave, deliver the UI pages concurrently, including authorization, accessibility, telemetry, markers, responsive behavior and browser tests. Do not declare a backend context migrated while its users still depend on a legacy page that performs direct legacy writes.

Wave entry criteria:

- context, data owner, API/events, consumers and UI routes are approved;
- characterization, contract and reconciliation tests exist;
- threat model, privacy classification, SLO and rollback plan are complete;
- dependent contracts are stable enough for parallel work.

Wave exit criteria:

- modern service owns writes and reads;
- all in-scope UI routes are modern and correctly marked;
- no unapproved direct table access or legacy service call remains;
- alerts/runbooks/on-call ownership and cost dashboards are operational;
- fallback, replication and deletion dates are recorded.

### Phase 7 — Eliminate shared-data and framework coupling

Once enough contexts are authoritative:

- revoke modern identities from legacy schemas and enforce per-service database permissions;
- replace remaining cross-domain queries with APIs or event-built projections;
- convert scheduled OFBiz jobs to owned service workers or workflows;
- remove ECA bridges and temporary event translators;
- test disaster recovery per service and for business-wide workflows;
- shrink the OFBiz deployment by disabling fully migrated components.

Exit criteria:

- ownership catalogs match actual database permissions and runtime access;
- no modern production path imports OFBiz runtime libraries or relies on OFBiz transactions;
- cross-service workflow recovery and audit evidence meet business requirements.

### Phase 8 — Retire OFBiz and migration scaffolding

Deliverables:

- final data archive and retention/legal approval;
- removal of OFBiz routes, identity bridge, adapters, replication jobs and legacy feature flags;
- deletion of obsolete infrastructure through reviewed Terraform plans;
- final penetration, performance, resilience and recovery tests;
- operational handover, cost baseline and architecture documentation;
- removal of Legacy/Mixed marker states while retaining release/build diagnostics for modern pages.

Exit criteria:

- traffic and dependency evidence show no OFBiz consumers for the agreed observation period;
- business, security, operations and data owners approve shutdown;
- restore/access requirements for archived records are tested;
- the monolith can be powered off without breaking retained journeys.

## 8. Per-slice work package

Each backlog slice must contain all of the following; splitting them across unrelated epics invites partial migration:

1. business scope and acceptance examples;
2. current service/entity/ECA/job and UI-route dependencies;
3. target owner, API/event contracts and authorization rules;
4. database schema, backfill, synchronization and reconciliation design;
5. saga/compensation and failure-recovery design where applicable;
6. service implementation and outbound adapters;
7. modern UI route, accessible Modern/Mixed marker and design-system work;
8. local seed data, Compose wiring and developer documentation;
9. Terraform and deployment configuration;
10. unit, integration, contract, E2E, accessibility, security, load and resilience tests;
11. observability, SLO, alert, runbook and cost allocation;
12. cohort rollout, cutover, rollback and deletion plan.

## 9. Security and compliance controls

- Perform threat modeling for the platform and every materially different slice.
- Use zero-trust identity and explicit authorization between services; network location is not trust.
- Keep origins private and permit ingress only through approved edge/gateway paths.
- Enforce least-privilege managed identities and separate human, CI and runtime access.
- Store secrets and certificates in Key Vault; rotate and audit them.
- Apply secure headers, CSP, CSRF protection, dependency and container scanning, SBOM generation and image signing/verification.
- Validate request bodies and file uploads, encode output, parameterize queries and return non-sensitive problem details.
- Classify PII/financial data, encrypt in transit and at rest, minimize event payloads, and define retention/deletion.
- Send immutable security/audit events with correlation identifiers; prevent application operators from silently changing audit history.
- Use private endpoints and egress controls for sensitive production resources.
- Test tenant isolation, privilege escalation, confused-deputy risks, replay/idempotency and legacy identity bridging.
- Never let the visible Modern marker weaken authorization or reveal topology; it is informational, not a trust signal.

## 10. CI/CD and quality gates

Pull requests run affected builds plus contract compatibility, architecture, secrets, dependency, IaC, container and license checks. Main-branch pipelines create signed immutable artifacts once and promote the same digests through environments.

Required gates include:

- formatting, compilation, unit/integration tests and static analysis;
- API/event backward-compatibility checks;
- database migration expand/contract validation;
- Terraform formatting, validation, lint, security/policy scan and reviewed plan;
- container vulnerability scan, SBOM and provenance/signature;
- browser E2E tests covering routing, login/logout, authorization and marker correctness;
- ephemeral-environment smoke and accessibility tests;
- post-deployment health/SLO checks with automated halt or rollback.

Deploy services independently. Use canary or cohort rollout and feature flags, but assign every flag an owner and expiry date. Database rollback normally means forward repair; destructive migrations wait until old versions and rollback windows are gone.

## 11. Governance and intended architecture

The current Sonar intended-architecture model has no constraints. Add a model when the first modern modules are introduced, and evolve it deliberately. Model each service and UI feature as a container with explicit public API/interface groups and private application/domain/adapter groups. Allow cross-service dependencies only through published contract/client packages; forbid imports of another service's internals and all modern-to-OFBiz imports except the anti-corruption adapter.

Also enforce what static architecture cannot:

- route ownership and legacy access through route-catalog CI tests;
- data ownership through database grants and access telemetry;
- runtime API/event dependencies through contract catalogs and observability;
- Terraform dependency/governance through module policy and plans;
- no browser-to-internal-service access through edge and network configuration.

Maintain ADRs, context map, data ownership, route ownership and service catalog as reviewed code. A temporary architectural exception needs an owner, rationale, compensating control and expiry date.

## 12. Major risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Hidden XML/ECA coupling | Include descriptors and jobs in discovery; add characterization and runtime trace evidence. |
| Distributed monolith | Enforce data ownership, public contracts and independent deployment from the first service. |
| Unsafe dual writes | Use outbox/CDC, idempotency and reconciliation; never uncoordinated dual writes. |
| Broken cross-domain transactions | Model sagas, compensations, timeouts and manual recovery before cutover. |
| Inconsistent legacy/modern identity | Establish shared IdP and tested bridge early; fail closed and audit mappings. |
| UI postponed behind backend work | Make UI route and marker mandatory exit criteria for every slice. |
| Fragmented user experience | Use one shell/design system/origin and accessibility/performance budgets. |
| Marker spoofing or information leak | Source it from trusted route metadata; CSP/encoding; expose only allowlisted details. |
| Cloud-only developer loop | Maintain Compose/Testcontainers, deterministic seeds and gateway parity tests. |
| Terraform drift or secret leakage | Protected remote state, OIDC, plan review, drift detection and Key Vault references. |
| Excessive service count | Require cohesive ownership, independent lifecycle and operational justification. |
| Permanent migration scaffolding | Give every adapter, replica, route and flag an owner and deletion criterion. |
| Cost/operational overload | Start with Container Apps, platform templates, budgets and limited concurrent waves. |

## 13. Immediate next actions

1. Approve program owners, outcomes and the Phase 0 ADR list.
2. Create the context, data-ownership and route-ownership catalogs.
3. Baseline critical user journeys, SLOs, database volumes and Azure cost assumptions.
4. Select a reversible pilot that includes one meaningful modern UI route.
5. Define the exact Modern/Mixed/Legacy marker design and machine-readable contract with security and UX review.
6. Bootstrap Terraform state and an ephemeral Azure environment using federated CI identity.
7. Build the service golden path, web shell and local Compose stack.
8. Put the edge in front of unchanged OFBiz and prove rollback before extracting behavior.
9. Create and verify the intended-architecture constraints when the new source modules exist.
10. Execute the pilot, measure it against the exit criteria, and update subsequent waves from evidence.
