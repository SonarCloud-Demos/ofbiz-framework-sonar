# Phase 0 architecture decisions

These decisions are accepted for the platform foundation and first pilot. Each is revisited after the first read cutover and first write-ownership transfer.

## ADR-001: Azure Container Apps as default compute

**Decision:** Deploy independently scalable services, workers, jobs, the modern BFF, and transitional OFBiz containers to Azure Container Apps. Use AKS only if a measured requirement cannot be met.

**Rationale:** It provides revision traffic splitting, managed scaling, jobs, private environments, and lower platform overhead while retaining OCI portability.

## ADR-002: Supported implementation stacks

**Decision:** Use Java with a current supported LTS runtime and Spring Boot for the first domain service/BFF, and TypeScript with React for the shell/features. Gradle remains the root build authority and pins non-JVM tooling. A second backend stack requires an ADR and operations ownership.

**Rationale:** The repository and team-facing build are Java/Gradle-centric; one backend stack minimizes early operational variance. TypeScript is appropriate for the required modern UI.

## ADR-003: Contract-first APIs and events

**Decision:** Use OpenAPI for HTTP contracts, AsyncAPI/JSON Schema for events, and a common event envelope carrying ID, type/version, time, correlation, causation, and actor/tenant metadata when applicable. Commands use APIs; propagation uses facts/events.

**Rationale:** Versioned machine-readable contracts support code generation, compatibility checks, and independent deployment without sharing implementation models.

## ADR-004: Database ownership

**Decision:** Use PostgreSQL with exclusive schema/database credentials per service. No modern service reads OFBiz or another service database in steady state. Use outbox/inbox and sagas rather than distributed transactions.

**Rationale:** This creates enforceable ownership and avoids a distributed monolith while retaining relational guarantees inside a bounded context.

## ADR-005: Local platform fidelity

**Decision:** Use Docker Compose for the integrated runtime, Testcontainers for isolated integration tests, real PostgreSQL, local/emulated narrow adapters for Azure services, and one local TLS origin. No committed authentication bypass exists.

**Rationale:** Developers can work without Azure while testing the same protocols and routing semantics.

## ADR-006: Identity and browser security

**Decision:** Use Entra ID Authorization Code + PKCE with a BFF that keeps tokens out of browser storage. A time-bounded identity bridge creates only the minimum OFBiz session. Services validate tokens and authorize independently.

**Rationale:** It supports MFA/Conditional Access and avoids leaking bearer tokens into browser code or across the legacy boundary.

## ADR-007: UI composition and marker

**Decision:** Build one modular shell with feature modules, not independently deployed micro-frontends initially. The trusted shell renders the **Modern experience** marker from the controlled route catalog. Legacy pages use full-page same-origin transitions, never iframes.

**Rationale:** It enables route-by-route strangling with consistent security/accessibility and lower composition risk.

## ADR-008: Monorepo build and dependency locking

**Decision:** `./gradlew build` builds and verifies all old/new code and IaC validation. Ecosystem-native lockfiles are generated/verified by Gradle and committed. The single SonarQube project analyzes the entire repository.

**Rationale:** One build graph prevents local/CI drift and preserves whole-codebase analysis throughout migration.
