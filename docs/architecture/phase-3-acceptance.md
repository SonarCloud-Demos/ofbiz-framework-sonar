# Phase 3 acceptance matrix

## Repository and local-runtime gate

| Capability | Evidence | Status |
| --- | --- | --- |
| Controlled routing | The edge module sends only enumerated modern paths to the shell origin and retains a legacy catch-all | Complete for unapplied Terraform |
| Fail-closed catalog | `/route-manifest.json` declares legacy as the default and identifies the modern profile route | Complete |
| UI shell | The authenticated shell owns one static **Modern experience** marker and a diagnostics navigation route | Complete |
| Identity bridge seam | `/modern/profile` exposes only the authenticated local subject and bridge availability | Complete for local authentication |
| Session protection | Bridge-session cookies are Secure, HttpOnly, SameSite Strict; revocation requires a per-session CSRF token | Complete |
| Failback | `MODERN_ROUTES_ENABLED=false` returns the modern profile route to the legacy route without rebuilding | Complete |
| Security headers | Shell and BFF responses set CSP, no-sniff, no-referrer, and no-store controls | Complete |
| Automated checks | Java tests cover authentication, routing, failback, session revocation, CSRF rejection, and marker ownership; local smoke covers both runtimes | Complete |

The local Basic authentication mechanism is development scaffolding, not the production identity design. The bridge endpoint models the required trust boundary and lifecycle without claiming Entra or OFBiz production-session compatibility.

## Deferred operational gate

Before any Azure deployment or production traffic, evidence is required for:

- Front Door, WAF, and APIM deployment through one public origin with direct-origin access blocked;
- Entra federation, permission mapping, deny-by-default authorization, managed identities, and least-privilege RBAC;
- a production OFBiz identity/session bridge, including expiry, logout, revocation, fixation, replay, and deep-link tests;
- cohort and canary routing, marker-spoof probes, edge bypass tests, and rehearsed failback;
- live browser-to-origin logs, metrics, traces, dashboards, alerts, audit records, and support ownership;
- completion of every deferred Phase 2 operational item in `phase-2-acceptance.md`.

## Completion decision

Phase 3 repository and local-runtime implementation was completed on 2026-09-25 under the approved unapplied-infrastructure waiver. Deferred operational checks are not passed, and this decision authorizes neither cloud deployment nor production traffic.
