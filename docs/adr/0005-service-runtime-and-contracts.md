# ADR 0005: service runtime and contracts

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Solution architect and engineering owners (governance register)

## Context

The organization needs a supportable service baseline without rebuilding OFBiz as a proprietary shared framework. Existing Java capability can reduce migration risk, while contracts must remain technology neutral.

## Decision

Use Java 21 LTS and Spring Boot 3.x as the default new-service runtime. Use REST/JSON with OpenAPI 3.1 for synchronous contracts and versioned schemas/AsyncAPI for integration events. Organize service source around application/domain ports and inbound/outbound adapters. Deviations require an ownership, support, build, security, and operations justification.

## Consequences

- The golden path supplies narrow templates, telemetry, security and testing defaults rather than shared domain behavior.
- Contract compatibility is a CI gate and generated clients are disposable build products.
- gRPC is introduced only for measured needs and with gateway/operations support.
- Services cannot expose OFBiz maps, `GenericValue`, entity keys, or status internals.
