# Azure platform infrastructure

This directory contains the Terraform platform foundation and Phase 4 strangler edge. It is intentionally split into a one-time state bootstrap, reusable modules, and isolated environment roots.

## Layout

- `bootstrap/state`: creates the private, versioned storage account used for Terraform state.
- `modules/platform`: creates the shared Azure platform baseline.
- `environments/dev`, `staging`, and `prod`: compose isolated long-lived environments with environment-specific reliability controls.
- `environments/ephemeral`: creates a run-scoped platform used only by the create/verify/destroy workflow.
- `backend/*.hcl.example`: documents per-environment backend coordinates without credentials.

Terraform authenticates with Azure CLI locally and OIDC in CI. Never pass secrets as Terraform variables. Applications obtain secrets through managed identity and Key Vault references.

## Validate locally

```bash
terraform -chdir=infra/bootstrap/state init -backend=false
terraform -chdir=infra/bootstrap/state validate
terraform -chdir=infra/environments/dev init -backend=false
terraform -chdir=infra/environments/dev validate
terraform -chdir=infra/environments/staging init -backend=false
terraform -chdir=infra/environments/staging validate
terraform -chdir=infra/environments/prod init -backend=false
terraform -chdir=infra/environments/prod validate
terraform -chdir=infra/modules/platform test
terraform fmt -check -recursive infra
```

Cloud plans require an approved subscription, tenant, remote-state coordinates, and OIDC identity. See [Phase 2 operations](../docs/migration/phase-2/operations.md) before applying.

## Phase 4 edge

The platform module creates a Front Door Premium endpoint, prevention-mode WAF, private-link APIM origin, APIM route definitions, and Entra validation policies. `edge_enabled` defaults to `false`; enable it only after private-link approval, origin health, identity mapping, session-transition tests, and the rollback rehearsal in the Phase 4 runbook pass.

Long-lived and ephemeral environments require the public Entra client ID/API audience plus private HTTPS origin URLs. These are identifiers and coordinates, not credentials. The public PKCE client has no client secret. Front Door is the only public application endpoint; the APIM post-provision update disables its public network access.
