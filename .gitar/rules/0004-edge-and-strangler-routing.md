# ADR 0004: edge and strangler routing

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Solution architect, security and operations owners (governance register)

## Context

Legacy and modern routes require one trusted public origin, gradual cohort rollout, immediate rollback, WAF protection, API policy, and auditable implementation ownership.

## Decision

Use Azure Front Door Premium for global ingress, managed TLS and WAF. Use Azure API Management for API authentication/policy and strangler routing. Keep origins private. Version the route catalog in the repository and validate both APIM and the local gateway against it.

## Consequences

- Clients target stable gateway contracts rather than service or OFBiz origins.
- Reads may be safely shadowed; state-changing requests are never blindly mirrored.
- Route rollout supports staff, tenant, named cohort, percentage, default-modern, and modern-only stages.
- Every fallback route has an owner and removal date.
