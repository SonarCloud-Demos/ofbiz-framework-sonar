# Phase 4: strangler edge and identity

## Status

**Repository implementation complete; live exit validation pending.** The repository now defines the Front Door/APIM edge, WAF, private origin, Entra policies, explicit legacy identity mapping, modern/legacy navigation, local authentication, rollback controls, and automated static/unit tests. Enabling production traffic still requires the environment-specific approvals and live evidence listed below.

Last reviewed: 2026-09-23

## Implemented baseline

- Front Door Premium is the single public endpoint and reaches internal APIM through Private Link. The endpoint is disabled unless `edge_enabled` is explicitly approved.
- The WAF runs the default and bot-manager managed rules in prevention mode. HTTP redirects to HTTPS and backend forwarding is HTTPS-only.
- APIM exposes only the catalog API and allowlisted legacy catalog operation. Policies validate issuer, audience, expiry, and `catalog.read`, strip incoming identity headers, add correlation, and rate-limit the catalog API.
- The catalog OFBiz web application has a fail-closed identity bridge. In `entra` mode it accepts only the APIM-produced subject/tenant context, requires an explicit subject-to-`UserLogin` mapping, rotates the session, checks that the mapped account is enabled, and uses the normal OFBiz login lifecycle.
- The shell links modern and retained legacy catalog routes through one origin and exposes trusted marker metadata.
- Local development remains independent of Entra. The local gateway provides an isolated HttpOnly `SameSite=Strict` developer session, rejects anonymous catalog API calls, and exercises login/logout in the smoke test. This endpoint exists only in `local-dev`; APIM does not expose it.
- Terraform tests cover private services, TLS, WAF, Front Door routing, diagnostics, and governance defaults. Unit tests cover mapping parsing and rejection.

## Identity bridge configuration

Deployment must set all three variables on the private OFBiz origin:

- `OFBIZ_IDENTITY_BRIDGE_MODE=entra`
- `OFBIZ_ENTRA_TENANT_ID=<approved-tenant-uuid>`
- `OFBIZ_ENTRA_SUBJECT_MAPPINGS=<entra-object-id>=<UserLoginId>[,...]`

Mappings are explicit and fail closed. Do not map groups, email addresses, display names, or mutable user principal names. APIM removes client-supplied identity headers before setting the validated subject and tenant. The OFBiz origin must remain unreachable except from APIM; the headers are not an independent authentication mechanism.

Local OFBiz must omit these variables. The modern Compose stack retains `CATALOG_SECURITY_MODE=local` and uses the local gateway cookie flow.

The deployed modern-shell origin is identity-aware and must implement same-origin `GET /auth/session`, `GET /auth/login`, and `POST /auth/logout` contracts backed by authorization-code flow with PKCE. The static Phase 3 shell consumes those contracts and never stores bearer tokens. The environment origin URLs include their route base paths: `/modern`, `/api/catalog`, and `/catalog` respectively.

## Live exit gates

The following require an approved Azure subscription, Entra registrations, private origins, DNS/certificates, and security/business owners; repository tests cannot manufacture this evidence:

- approve the Front Door→APIM private-link connection and prove direct APIM/OFBiz/service access is blocked;
- register the public PKCE client and `catalog.read` scope, then run positive and negative tenant/audience/scope tests;
- approve every Entra-subject-to-OFBiz mapping and test disabled, removed, and privilege-changed users;
- test login, logout, CSRF, session fixation, expiry, cross-route transition, and emergency rollback in ephemeral Azure;
- compare existing journey behavior and latency through the edge against the Phase 0 baseline;
- review WAF false positives, dashboards, audit events, accessibility, and the rollback rehearsal.

Do not set `edge_enabled=true` in staging or production until these gates have signed evidence. Follow [the Phase 4 operations runbook](../../runbooks/phase-4-edge-identity.md).
