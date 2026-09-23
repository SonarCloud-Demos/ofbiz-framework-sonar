# Phase 2: Azure and Terraform foundation

## Status

**Repository implementation complete; Azure exit validation deferred.** The user directed that live tests be skipped because no Azure environment is available. Phase 2 therefore does not formally pass its cloud exit criteria, and Phase 3 work must not interpret this status as runtime evidence.

Last reviewed: 2026-09-23

## Implemented repository evidence

- Terraform and the official AzureRM provider are exactly pinned.
- A separate bootstrap creates encrypted, private, versioned remote state with recovery retention and RBAC access.
- The reusable platform module creates private networking, Container Apps, ACR, Key Vault, App Configuration, Service Bus, Log Analytics, Application Insights, governance tags, and a budget.
- ACR, Key Vault, App Configuration, and Service Bus use private endpoints and linked private DNS zones.
- PostgreSQL is private, password authentication is disabled, and administration is assigned to an approved Microsoft Entra group.
- APIM uses an internal virtual network, managed identity, enforced developer-portal sign-in, client certificates, and TLS hardening.
- A Premium Front Door profile and disabled endpoint are present without an unsafe public APIM origin.
- Resource-group policy assignments deny unapproved regions and resources missing required governance tags.
- OIDC-backed speculative plans and weekday drift detection are ready to activate through the protected `dev-plan` environment.
- A manually triggered, protected workflow creates an isolated ephemeral environment, checks convergence, destroys it even after earlier failures, and verifies that no managed resources remain.
- Environment roots use Azure AD state authentication and contain no credentials or secret values.
- Pull requests format and validate every Terraform root without cloud credentials.
- Staging and production roots isolate state and select stronger availability, backup, retention, database, and APIM tiers than development.
- Supported platform resources export logs and metrics to Log Analytics.
- Native Terraform tests enforce the secure configuration invariants without an Azure subscription.
- State storage has private blob connectivity, private DNS integration, diagnostics, versioning, recovery retention, Azure AD authorization, and no public/shared-key access.

Front Door route/origin wiring belongs to Phase 4, where the strangler edge is introduced; Phase 2 supplies the disabled Premium endpoint and internal APIM foundation. Protected long-lived applies still require an approved plan-evidence store. The dependency policy rejected the upload/download artifact actions, so the repository does not bypass that control.

## Exit checklist

- [x] Version and provider pins exist in every root and module.
- [x] Remote-state bootstrap is separate from environment state.
- [x] Core private platform resources are represented as reusable Terraform.
- [ ] Provider and CI-action dependency policy is evaluated; the current dependency catalog recognizes neither Terraform providers nor the pinned setup action.
- [ ] CI creates, smoke-tests, and destroys an ephemeral environment (deferred: no Azure environment).
- [ ] Staging and production network, identity, diagnostic, budget, and recovery tests pass (deferred: no Azure environment).
- [ ] Drift detection and break-glass reconciliation are exercised (deferred: no Azure environment).
- [x] APIM, Front Door, PostgreSQL, and private endpoint configuration passes static and provider-schema validation.
- [x] Allowed-location and required-tag policies are assigned at environment resource-group scope.
- [x] Ephemeral create, convergence, destroy, and post-destroy verification are automated with isolated state and mandatory expiry tagging.
- [ ] The ephemeral workflow passes against an approved Azure subscription (deferred: no Azure environment).
- [x] Front Door remains disabled and unrouted until Phase 4 approves and tests its private origin pattern.
- [x] Staging and production roots, diagnostics, recovery configuration, and environment-specific SKUs are represented and statically validated.
- [x] Native Terraform security and policy tests pass without cloud credentials.

The remaining unchecked items are external evidence gates, not unfinished Terraform implementation. Reopen this phase when an Azure environment becomes available and record the live evidence before declaring the phase passed.
