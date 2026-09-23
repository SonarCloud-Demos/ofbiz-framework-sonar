# ADR 0010: Phase 3 runtime security update

- Status: Accepted
- Date: 2026-09-23
- Supersedes: Spring Boot 3.x selection in ADR 0005
- Decision authority: Security and engineering owners (temporary repository decision pending formal review)

## Context

The Phase 3 dependency review found that the available Spring Boot 3.x web baseline was not admissible. Spring Boot 3.5.6 actuator was affected by CVE-2026-22731 and CVE-2026-22733, while the dependency catalog did not recognize `spring-boot-starter-web` at either the patched 3.5.12 or current 4.1.1 line. Direct servlet-container alternatives were also rejected by license policy and security findings.

## Decision

Use Java 21 with Spring Boot 4.1.1 and the policy-approved WebFlux starter for the golden path. Blocking JDBC projection reads are isolated on Reactor's bounded-elastic scheduler. Retain REST/JSON, OpenAPI 3.1, AsyncAPI, ports/adapters, and all other decisions from ADR 0005.

## Consequences

- New services use the patched Spring 7 generation and must not assume servlet APIs.
- Teams must keep blocking adapters off event-loop threads.
- Formal architecture owners must ratify or replace this temporary superseding decision before Phase 3 can pass its exit review.
