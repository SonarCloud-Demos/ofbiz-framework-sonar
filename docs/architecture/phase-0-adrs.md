# Phase 0 architecture decisions

These decisions are accepted for the platform foundation and first pilot. Each is revisited after the first read cutover and first write-ownership transfer.

## ADR-001: Azure Container Apps as default compute

**Decision:** Deploy independently scalable services, workers, jobs, the modern BFF, and transitional OFBiz containers to Azure Container Apps. Use AKS only if a measured requirement cannot be met.

**Rationale:** It provides revision traffic splitting, managed scaling, jobs, private environments, and lower platform overhead while retaining OCI portability.

**Phase 2 confirmation (2026-09-25):** Azure Container Apps remains the exclusive Phase 2 compute target. AKS is outside Phase 2 and requires a later ADR backed by a measured workload requirement and an operational ownership model.

## ADR-002: Supported implementation stacks

**Decision:** Use Java with a current supported LTS runtime and Spring Boot for the first domain service/BFF, and TypeScript with React for the shell/features. Gradle remains the root build authority and pins non-JVM tooling. A second backend stack requires an ADR and operations ownership.

**Rationale:** The repository and team-facing build are Java/Gradle-centric; one backend stack minimizes early operational variance. TypeScript is appropriate for the required modern UI.

**Phase 1 implementation note (2026-09-25):** The reference service and BFF use the Java 17 JDK HTTP server temporarily. The proposed Spring Boot 3.5.12/Actuator/JDBC/Flyway/Micrometer graph failed dependency policy because it resolved prohibited-license components and high-severity vulnerable components. No exception was granted and the rejected dependencies were removed. Revisit this ADR, validate an acceptable locked graph, and record the decision before a business service adopts Spring Boot or selects a replacement stack.

**Phase 2 confirmation (2026-09-25):** The dependency-light Java 17 reference runtime is the landing-zone workload. Spring Boot remains the preferred business-service candidate but is not approved until a current, locked dependency graph passes repository, license, vulnerability, Java, observability, migration, and coverage policy. No dependency-policy exception is assumed. If it fails, Quarkus and Micronaut are evaluated against the same criteria before Phase 4 implementation.

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
