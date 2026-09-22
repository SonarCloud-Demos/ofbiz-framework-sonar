# Azure platform infrastructure

This directory contains the Phase 2 Terraform foundation. It is intentionally split into a one-time state bootstrap, reusable modules, and isolated environment roots.

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
