# Phase 0 technical baseline

## Evidence date and sources

Baseline date: 2026-09-22

Sources:

- Sonar current Java architecture graph;
- Sonar intended-architecture API read-back;
- `docs/migration/OFBIZ_current_architecture.md`;
- root Gradle, OFBiz component, Entity Engine, Service Engine, web and Docker configuration;
- repository CI workflow inventory.

This is a source-architecture baseline. Production telemetry and business-volume evidence still need to be supplied through the measurement baseline.

## Current architecture

The system is a metadata-driven Java modular monolith. Active framework, application, and theme components are assembled into one Gradle distribution, loaded into one JVM, hosted by embedded Tomcat, and normally share the Entity Engine, Service Engine, security context, transaction manager, scheduled-job infrastructure, caches, and relational database.

Sonar reports:

| Measure | Baseline |
| --- | ---: |
| Top-level Java architecture nodes | 71 |
| Java package dependencies | 399 |

The highest-coupling hubs are:

| Package | Incoming dependents | Outgoing dependencies | Migration implication |
| --- | ---: | ---: | --- |
| `org.apache.ofbiz.base.util` | 67 | 15 | Utility coupling must not become a new shared service framework. |
| `org.apache.ofbiz.entity` | 55 | 13 | Data ownership and removal of direct Entity Engine access are primary extraction gates. |
| `org.apache.ofbiz.service` | 48 | 11 | Local dispatch must become explicit API/event contracts. |
| `org.apache.ofbiz.security` | 24 | 6 | Identity and authorization coexistence must precede broad route migration. |
| `org.apache.ofbiz.webapp` | 17 | 13 | UI strangling must account for sessions, controllers, events, and security filters. |
| `org.apache.ofbiz.common` | 16 | 11 | Shared business utilities require ownership analysis rather than blind reuse. |
| `org.apache.ofbiz.product.product` | 10 | 14 | Product is both a domain hub and a likely upstream dependency for later waves. |
| `org.apache.ofbiz.content` | 10 | 10 | Content crosses several business journeys and needs explicit contract boundaries. |

## Current deployability

- The deployable unit is the full OFBiz distribution.
- The default container image runs one OFBiz process and embedded Tomcat.
- H2 is the local/default database; PostgreSQL is supported as an external deployment.
- The repository includes Docker and a PostgreSQL Compose example, but not the target Azure landing zone or complete modern local stack.
- Horizontal scale replicates the complete runtime and must account for sessions, in-process caches, shared files, and scheduled-job leasing.
- Business components cannot currently deploy, scale, recover, or roll back independently.

## Current delivery and governance observations

- GitHub workflows cover Gradle/build, Docker image, and SonarQube analysis concerns.
- There is no committed Terraform target platform yet.
- There is no committed machine-readable data-ownership or route-ownership catalog yet.
- Existing OFBiz XML entity, service, ECA, controller, screen, and component metadata are architectural dependencies and must be included in Phase 1 discovery.
- The worktree contains user-owned migration documentation changes; this program must preserve unrelated changes and introduce migration artifacts incrementally.

## Target architecture enforcement baseline

SonarQube intended-architecture model: `4987d1a6-81fe-4f94-86a2-75d56491b7a2`.

The model was created and read back through the Sonar API with:

- a Java perspective containing 18 top-level groups and 34 constraints;
- a TypeScript UI perspective containing 18 top-level groups and 63 constraints;
- public contract/entry groups and explicitly private implementation groups;
- the anti-corruption adapter as the only permitted modern-to-legacy Java dependency;
- no missing parent patterns, silently dropped constraints, or altered interface flags in API read-back.

The local `sonar context architecture get-intended` view currently returns an empty effective constraint list. Treat this as an enforcement-verification action: the platform owner must confirm the model is attached to the next SonarQube analysis and triage the resulting architecture state before Phase 0 closes. The API model remains the source evidence that the target was accepted and stored.

## Initial modernization hypotheses

These are inputs to Phase 1, not validated conclusions:

1. Notifications or a read-only projection is a safer first vertical slice than ledger or payments.
2. Party and product/catalog capabilities are upstream dependencies and should stabilize before order and fulfillment cutovers.
3. Accounting/ledger extraction should follow stable invoice, payment, inventory-valuation, and order event contracts.
4. An identity/session bridge and one controlled edge are prerequisites for concurrent UI migration.
5. Data ownership, not package movement, is the dominant migration constraint.
