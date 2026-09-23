# ADR 0008: monorepo and local development

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Engineering and platform owners (governance register)

## Context

The user requires one repository and a complete developer-machine test workflow. Independent services still need independent ownership, build, deployment, and rollback.

## Decision

Keep legacy code, services, web features, contracts, Terraform, local runtime, tests, and architecture catalogs in one repository. Provide stable root commands for full and affected builds. Use Docker Compose for the representative local system, Testcontainers for isolated integration tests, deterministic seeds, and host hot reload for a selected service/UI.

## Consequences

- Repository proximity does not permit runtime, data, or source-boundary violations.
- CI uses change impact for speed and scheduled full builds for integrity.
- Local tests do not require production secrets or a personal Azure subscription; unsupported emulator semantics use a clearly controlled development resource.
- Service artifacts are independently versioned/promoted even when one commit changes several components.
