# ADR 0001: Azure compute platform

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Solution architect and platform owner (governance register)

## Context

The target needs independently deployable containers, managed scaling, private networking, revisions, observability, and a low-friction local container workflow. Operating Kubernetes adds material platform and security responsibility.

## Decision

Use Azure Container Apps as the default service runtime. Use Azure Container Registry for images. Select AKS only when a documented requirement cannot be met by Container Apps, such as required custom operators, privileged/daemon workloads, specialized scheduling/networking, or an approved portability mandate.

## Consequences

- The platform team operates fewer cluster primitives and can focus on service and policy standards.
- Services must remain portable OCI containers and cannot assume direct Kubernetes API access.
- An AKS exception requires cost, staffing, security, recovery, and migration analysis.
