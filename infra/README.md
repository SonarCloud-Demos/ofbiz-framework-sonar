# Infrastructure

Phase 1 provides the Azure-independent local runtime. The approved Phase 2 target and infrastructure waiver are recorded in `docs/architecture/phase-2-decisions.md`; its repository and deferred operational gates are in `docs/architecture/phase-2-acceptance.md`.

Phase 2 produces Azure-accurate but unapplied Terraform. No Azure subscription, credentials, tenant identifiers, state, or secrets belong in this directory. Local application development remains Azure-independent.

The repository implementation gate is complete. Operational Azure validation remains deferred under the waiver in `docs/architecture/phase-2-acceptance.md`.

## Provider policy exception

The dependency-check service does not support Terraform providers. Its `package_not_found` response for AzureRM 4.46.0 is therefore not a package risk result. The project owner explicitly approved bypassing that unsupported check on 2026-09-25. AzureRM remains version-pinned and lockfile-verified; provider review uses the Terraform Registry metadata, checksums, release notes, and repository analysis instead.

The contract scaffold declares the approved provider so initialization verifies its source, version, and checksums before Azure resources are introduced.

## Planned layout

```text
infra/
  bootstrap/       remote state and deployment identity foundation
  contracts/       provider-constrained environment contract and tests
  environments/    test, stage, and production composition
  modules/         reusable Azure capability modules
```
