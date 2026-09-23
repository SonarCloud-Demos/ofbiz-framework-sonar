# Phase 2 operations

## State bootstrap

Run the bootstrap with a separately approved platform identity from a runner that can resolve and reach the landing-zone private endpoint subnet. Supply the existing `privatelink.blob.core.windows.net` zone and protected Log Analytics workspace IDs. Public access and shared-key authentication are disabled from creation; do not weaken them to bootstrap from an untrusted workstation.

Before environment use, test blob version restore and container soft-delete recovery. Record the exercise date, operator, restored version, elapsed time, and cleanup evidence outside the state itself.

## Plans and applies

Pull requests may format, initialize without a backend, validate, scan, and produce a speculative plan through OIDC. Apply jobs must use protected environments, the reviewed saved plan, an environment-specific federated identity, and manual approval for staging and production.

Never upload plan files to untrusted artifacts: plans can contain sensitive provider data even when outputs are non-sensitive.

The committed Front Door endpoint is disabled and has no route or origin. Enable it only after the platform and security owners approve a Private Link path to internal APIM. A temporary public APIM origin is not an acceptable bootstrap mechanism.

PostgreSQL accepts Microsoft Entra authentication only. Supply an approved group object ID and group display name; do not assign an individual as the durable administrator and do not introduce an administrator password variable.

Configure these non-secret variables on the protected `dev-plan` GitHub environment before enabling plans:

| Variable | Purpose |
| --- | --- |
| `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID` | Environment-specific OIDC identity coordinates |
| `TF_STATE_RESOURCE_GROUP`, `TF_STATE_STORAGE_ACCOUNT`, `TF_STATE_CONTAINER` | Private remote-state coordinates |
| `AZURE_LOCATION`, `AZURE_COST_CENTER`, `AZURE_PLATFORM_OWNER` | Region and governance metadata |
| `AZURE_MONTHLY_BUDGET`, `AZURE_BUDGET_CONTACT_EMAILS_JSON` | Budget amount and JSON email list |
| `APIM_PUBLISHER_NAME`, `APIM_PUBLISHER_EMAIL` | APIM operational contact |
| `POSTGRES_ADMIN_GROUP_NAME`, `POSTGRES_ADMIN_GROUP_OBJECT_ID` | Approved Entra administrator group |

The OIDC subject must be restricted to this repository and the `dev-plan` environment. Grant the plan identity state-read access and the minimum Azure read permissions needed for refresh; do not give it apply permissions.

## Drift and emergency changes

The Terraform workflow runs a weekday refresh-only plan. Terraform exit code `2` fails the job and signals drift; the on-call platform owner must open an incident and assign an owner. Break-glass portal changes require an incident reference, approval, least-privilege time-boxed access, and a same-day Terraform reconciliation or documented rollback.

## Destruction

Ephemeral environments require an `expiry` tag and a dedicated state. Configure the protected `ephemeral` GitHub environment with the same variables as `dev-plan`, but use a separate federated identity whose subject is restricted to that environment. It needs narrowly scoped create/delete access only in the approved ephemeral subscription or management-group sandbox.

The manual workflow requires an ISO expiry date, uses the run ID for both resource naming and the state key, applies only its locally saved plan, checks refresh-only convergence, runs destroy even when an earlier step fails, and verifies the destroy plan is empty. Production destroy is not automated.

If destruction fails, preserve the workflow logs and state, open a cleanup incident immediately, and remove the resource group with the same protected identity after reviewing the remaining plan. Never reuse an ephemeral state key.
