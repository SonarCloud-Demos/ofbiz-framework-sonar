# ADR 0003: identity and access

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Security owner (governance register)

## Context

Legacy OFBiz sessions and permissions must coexist securely with modern browser routes and services. Storing reusable cloud credentials or trusting network location is unacceptable.

## Decision

Use Microsoft Entra ID for workforce identity and External ID where external users require it. Browser clients use OIDC authorization code with PKCE; adopt a BFF with secure cookies when the UI threat model favors keeping tokens out of the browser. Services and CI use managed identities or OIDC workload federation. Implement a temporary, fail-closed legacy identity/permission bridge with explicit mappings and audit.

## Consequences

- Authentication is centralized, while every service remains responsible for action/resource authorization.
- Cookie flows require CSRF protection, secure session lifecycle, and privilege-change handling.
- Legacy permissions must be inventoried and mapped; unmapped privileges are denied.
- The identity bridge has an owner, threat model, telemetry, retirement criterion, and removal test.
