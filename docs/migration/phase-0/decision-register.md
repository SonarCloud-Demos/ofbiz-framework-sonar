# Phase 0 decision register

ADRs 0001–0009 were **Accepted** on 2026-09-22. Named decision authorities and approval evidence are maintained in the organizational governance register.

| ADR | Decision | Proposed default | Authority |
| --- | --- | --- | --- |
| [0001](../../adr/0001-azure-compute-platform.md) | Azure compute platform | Container Apps; AKS only at a documented gate | Architect + platform owner |
| [0002](../../adr/0002-terraform-infrastructure-as-code.md) | Infrastructure as code | Terraform with protected remote state | Platform owner + security |
| [0003](../../adr/0003-identity-and-access.md) | Identity and access | Entra ID/External ID, managed identities and temporary legacy bridge | Security owner |
| [0004](../../adr/0004-edge-and-strangler-routing.md) | Edge and strangler routing | Front Door Premium + APIM | Architect + security + operations |
| [0005](../../adr/0005-service-runtime-and-contracts.md) | Service runtime and contracts | Java 21/Spring Boot, REST/OpenAPI, versioned events | Architect + engineering owners |
| [0006](../../adr/0006-data-and-messaging.md) | Data and messaging | Service-owned PostgreSQL + Service Bus + outbox | Data owner + architect |
| [0007](../../adr/0007-ui-composition-and-markers.md) | UI composition and markers | One modern shell, route-owned features, trusted markers | Product + UX + security |
| [0008](../../adr/0008-monorepo-and-local-development.md) | Repository and developer runtime | Monorepo, affected builds, Compose/Testcontainers | Engineering + platform |
| [0009](../../adr/0009-observability-and-reliability.md) | Observability and reliability | OpenTelemetry + Azure Monitor stack and per-service SLOs | Operations + security |

## Required follow-up decisions

Create additional ADRs when evidence is available for:

- frontend rendering/hosting choice (SPA, SSR/BFF, or hybrid);
- tenant model and isolation;
- schema-per-service versus server-per-service thresholds;
- CDC product and permitted use cases;
- regional topology, availability zones, RPO/RTO and disaster recovery;
- API versioning/deprecation policy and event schema registry;
- secrets rotation, private egress, SIEM and audit retention;
- reporting/analytics platform.
