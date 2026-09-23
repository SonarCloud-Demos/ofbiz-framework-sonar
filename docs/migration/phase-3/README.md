# Phase 3: engineering golden path

## Status

**Repository baseline implemented; exit validation incomplete.** The local golden path is represented by a Java 21/Spring Boot reference service, static accessible web shell, local gateway, PostgreSQL projection, contracts, telemetry configuration, CI template, and stable root commands. It does not claim that the Phase 3 exit criteria have passed.

Last reviewed: 2026-09-23

## Delivered repository baseline

- Product Catalog reference service with inbound/outbound ports, OpenAPI, AsyncAPI, Flyway migration, outbox/idempotency tables, readiness/liveness, security modes, and OTLP metrics.
- Same-origin web shell with CSP, responsive accessible catalog journey, trusted Modern marker, and machine-readable `data-runtime="modern"` evidence.
- Deny-by-default local nginx gateway for the modern UI and catalog GET route.
- Docker Compose topology and `./modern build|test|start|smoke|stop` commands. Images must be approved internal digest-pinned coordinates; no public fallback is embedded.
- Deterministic PostgreSQL seed projection and end-to-end API/marker/CSP smoke test.
- Pull-request service/contract pipeline plus protected supply-chain template for SBOM generation, high/critical vulnerability blocking, and keyless signing.
- Ownership metadata, dashboard/alert templates, and an incident/rollback runbook.

## Deliberate boundaries

- OFBiz remains on its existing root build and is not started by the modern profile yet. Combining it with the new stack requires the private-registry asset work documented in `docs/migration/dependency-configuration.md`.
- Messaging is represented by versioned AsyncAPI, transactional outbox, and idempotency schema. An Azure Service Bus emulator or controlled namespace is not configured because no approved image/namespace is available.
- Entra mode is fail-closed configuration for deployment; the local-only gateway uses the explicit local security mode. Phase 4 owns the full identity/session bridge.
- The route catalog describes these routes as `modern-local-only`; it does not enable Front Door or APIM.
- Phase 2 live Azure evidence remains deferred, so no ephemeral Azure smoke journey was run.

## Commands

Run `./modern test` for service tests and script validation after configuring the Artifactory URLs and credential-file paths documented in `docs/migration/dependency-configuration.md`. To start the stack, also export approved digest-pinned `POSTGRES_IMAGE`, `GRADLE_IMAGE`, `JAVA_RUNTIME_IMAGE`, and `NGINX_IMAGE` values, `CORPORATE_CA_FILE`, and a development-only `CATALOG_DATABASE_PASSWORD`; then run `./modern start` followed by `./modern smoke`. `./modern stop` preserves database data.

The root Gradle lifecycle includes the modern service through a composite build. `./gradlew test` runs legacy and modern tests, `./gradlew build` builds both, and `./gradlew sonar` first generates both JaCoCo XML reports. These root commands require the same Artifactory environment and credential-file variables as `./modern test`.

The modern service writes its JaCoCo XML report to `services/product-catalog-service/build/reports/jacoco/test/jacocoTestReport.xml`, and the root SonarQube analysis consumes it. Coverage thresholds are enforced by the SonarQube quality gate rather than Gradle.

## Remaining exit gates

- Prove the documented build/start flow from a fresh workstation, including legacy OFBiz and approved registry access.
- Select and validate an approved Service Bus emulator or controlled development namespace and exercise outbox publication, duplication, reordering, retry, and dead-letter behavior.
- Run contract and smoke journeys in an ephemeral Azure environment after Phase 2 cloud validation becomes available.
- Complete security, accessibility, and developer-usability reviews.
- Ratify ADR 0010 and configure approved internal contract/SBOM/scanner/signing tools.
