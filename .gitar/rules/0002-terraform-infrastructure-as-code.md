# ADR 0002: Terraform infrastructure as code

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Platform and security owners (governance register)

## Context

Azure infrastructure must be reproducible, reviewable, policy checked, independently promoted, and manageable with adjacent services. Competing tools controlling the same resource would create drift and unclear ownership.

## Decision

Use Terraform as the authoritative IaC tool. Bootstrap remote Azure Storage state separately; use private access, encryption, versioning, state recovery, environment isolation, locking, and OIDC-federated deployment identities. Do not manage the same resource lifecycle with Bicep/ARM and Terraform.

## Consequences

- Provider and Terraform versions and lockfiles are reviewed and pinned.
- Pull requests contain validated plans and policy/security checks; protected jobs apply them.
- Emergency portal changes are break-glass actions and must be reconciled into Terraform.
- Secrets use Key Vault references rather than Terraform values or outputs whenever possible.
