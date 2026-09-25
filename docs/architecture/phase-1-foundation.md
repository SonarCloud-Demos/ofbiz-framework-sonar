# Phase 1 local platform foundation

## Implemented foundation

- `services/reference-service` is an independently runnable Java 17 diagnostic service with liveness/readiness endpoints and correlation propagation.
- `services/experience-bff` serves the trusted modern shell and calls the reference service without exposing internal service topology to browser code.
- `web/shell` owns the static **Modern experience** marker and has anti-spoof tests.
- `contracts/external-api/reference-service.openapi.yaml` defines the reference HTTP contract.
- `local-dev/compose.yaml` runs both modern deployables as non-root, read-only containers.
- The representative stack also runs PostgreSQL 13 and the demo OFBiz runtime; the BFF exposes authenticated, same-origin checks for one modern and one legacy route over locally generated TLS.
- `./modern build|test|up|smoke|down|reset` is the local runtime entry point; authoritative builds still delegate to root Gradle.
- Root verification checks explicit module registration, Sonar source scope, and required Gradle, JavaScript, and Terraform lockfiles.
- Root verification rejects direct imports from modern services into OFBiz implementation packages.
- The reference database contains versioned outbox, inbox, and idempotency tables. They are reference schemas only; no Phase 1 business workflow claims ownership of them.
- Browser-compatible W3C `traceparent` and correlation identifiers propagate through the BFF to the reference service and appear in both services' logs. The local smoke test verifies the returned context.
- Java coverage gates require at least 80%, and shell tests enforce marker anti-spoofing and a dependency-free semantic accessibility baseline.

## Local workflow

```text
./gradlew build
./modern up
./modern smoke
./modern down
```

The default stack binds only to loopback and requires no Azure subscription. `./modern up` generates a local certificate and credentials outside source control; requests use TLS and HTTP Basic authentication with no bypass switch. Entra/OIDC and the OFBiz identity bridge are Phase 3 deliverables.

## Phase 1 completion boundary

The local foundation and repository exit criteria are implemented. The existing CI invocation is `./gradlew build jacocoTestReport sonar`; module/Sonar-scope guards, strict locks, local integration smoke tests, and architecture checks are attached to the root build. A generated-lock cleanup was also exercised to prove that missing locks are recreated rather than silently ignored.

Phase 1 intentionally proves OpenTelemetry-compatible W3C propagation and correlated logs, not an external trace exporter or collector; Azure Monitor/Application Insights wiring belongs to the Phase 2 landing zone. `/api/legacy/health` proves authenticated same-origin OFBiz reachability, not general legacy-page reverse proxying; production routing remains unchanged as required by the phase goal.

Spring Boot 3.5.12, Actuator, JDBC, Flyway, PostgreSQL JDBC, and Micrometer tracing were evaluated and rejected by dependency policy because the resolved graph contained prohibited licenses and high-severity vulnerabilities. The Phase 1 diagnostic runtime therefore uses the Java 17 HTTP server and Docker's PostgreSQL initialization hook without those runtime dependencies. ADR-002 must be revisited before the first business service adopts Spring or another framework.
