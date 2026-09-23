# Phase 4 edge and identity operations

## Enablement

1. Apply with `edge_enabled=false`; confirm the WAF, APIM APIs/policies, Front Door origin, diagnostics, and private-link request exist.
2. Approve the private-link request and verify origin health from Front Door.
3. Confirm APIM, OFBiz, shell, and catalog origins reject direct public traffic.
4. Configure Entra tenant/client/audience values and approved immutable subject mappings. Never put a client secret in Terraform or the browser.
5. Run the ephemeral login/logout, authorization, CSRF, marker, legacy-transition, and direct-origin-negative suite.
6. Rehearse the rollback below. Obtain security, application, and operations approval.
7. Set `edge_enabled=true` for the approved environment and monitor WAF blocks, 401/403 rates, origin health, latency, and correlation IDs.

## Emergency rollback

1. Set `edge_enabled=false` and apply the previously reviewed Terraform plan. This disables the Front Door route without opening an origin.
2. If the incident is limited to a modern route, restore its route catalog cohort to legacy and deploy the generated APIM/local route configuration; do not expose OFBiz directly.
3. Revoke affected sessions and Entra grants when identity integrity is uncertain. Remove compromised subject mappings before restoring traffic.
4. Preserve Front Door, APIM, OFBiz authentication, and application logs under the incident retention policy.
5. Re-enable only after negative authorization and session-transition tests pass and the incident owner approves.

## Local verification

`./modern start` followed by `./modern smoke` proves that anonymous API access fails, local login succeeds without Entra, the modern marker/CSP are present, logout revokes access, and local database data remains intact. `./modern stop` preserves developer data.
