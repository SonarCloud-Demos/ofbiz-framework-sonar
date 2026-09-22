# ADR 0007: UI composition and implementation markers

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Product, UX/accessibility and security owners (governance register)

## Context

The UI must migrate concurrently with services while users move safely between legacy and modern routes. Testers need unambiguous implementation evidence without exposing or trusting browser-controlled topology data.

## Decision

Use one modern web shell with route-owned feature modules, a shared design system, unified navigation, identity state, telemetry, accessibility, error handling, and trusted Modern/Mixed markers. Keep one deployable frontend initially; introduce independently deployed micro-frontends only when team/release evidence justifies them.

The shell displays Modern, Mixed, or Legacy state from a signed/deployed same-origin route manifest or equivalent trusted configuration. It also exposes an accessible, machine-readable marker for E2E tests. Query parameters, local storage, and arbitrary API content cannot control the marker.

## Consequences

- Every service slice includes its UI routes and marker tests.
- Mixed is temporary and requires an owner/expiry criterion.
- Same-origin routing is preferred; iframes require a threat model and removal date.
- CSP, encoding, authorization, accessibility, performance and API allowlists are UI release gates.
