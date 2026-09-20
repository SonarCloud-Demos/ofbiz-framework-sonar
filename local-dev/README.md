# Phase 1 hybrid local stack

The stack keeps OFBiz available on `https://localhost:18443`, exposes the modern gateway on
`https://localhost:18080`, and runs the Accounting Invoice and Notification services directly on
ports `18106` and `18107` respectively.

## Prerequisites

Copy `.env.example` to `.env` and supply:

- approved internal images for PostgreSQL, OFBiz, Node, Gradle 8.14.5/JDK 17, Java runtime, and Nginx;
- strong local database passwords;
- `ARTIFACTORY_TOKEN_FILE`, pointing to a temporary file containing only the Artifactory token.
- `NPM_CONFIG_USERCONFIG` and `CORPORATE_CA_FILE`, pointing to the host npm configuration and
  corporate CA without copying either into the repository.

Generate the local gateway certificate before starting the stack:

```sh
local-dev/generate-local-tls.sh
```

The image variables are deliberately required. The stack must not fall back to Docker Hub.
The OFBiz base image must already include the PostgreSQL JDBC driver; runtime downloads are
disabled. The local derived image installs theme npm assets exclusively through the configured
Artifactory registry, mounting `.npmrc` and the corporate CA only as BuildKit secrets as documented
in `docs/migration/npm-config.md`. Neither secret is copied into the image. The reproducible install
uses `--legacy-peer-deps` because the tracked jGrowl peer range has not yet declared jQuery 4
compatibility; lifecycle scripts remain disabled.

## Start

```sh
docker compose -p erp-local --env-file local-dev/.env \
    -f local-dev/docker-compose.yml up -d --build
```

Expected routes:

- `https://localhost:18080/webtools` → legacy OFBiz through the gateway
- `https://localhost:18080/api/accounting/invoices` → modern Accounting API
- `https://localhost:18080/api/accounting/invoices/8009` → modern invoice detail
- `https://localhost:18080/api/notifications/health` → modern Notification health
- `https://localhost:18443/webtools` → direct legacy fallback
- `https://localhost:18443/accounting/control/findInvoices` → direct legacy invoice fallback

The exact gateway route `/accounting/control/findInvoices` forwards to the modern HTML page. All
other `/accounting/control/*` paths continue to OFBiz.

Both modern Java images include a JDK-compiled HTTP health probe that runs using only the runtime
image's Java executable. Compose waits for both application health endpoints to report `UP` before
starting the gateway, without assuming that curl or wget exists in the approved runtime image.

The local OFBiz entrypoint removes only the image's persisted configuration markers before invoking
the original entrypoint. This forces database credentials and external URL settings to be reapplied
when `.env` changes while preserving the database, runtime logs, and all other persistent data.

## Verify

Once every container is healthy, run the end-to-end acceptance check:

```sh
local-dev/smoke-test.sh
```

The script verifies the strangled and fallback routes, origin rewriting, representative legacy
assets, both modern health endpoints, invoice list/detail projections, and temporary header CRUD.
It removes the temporary `MSVCTEST...` invoice on success or interruption. Override
`GATEWAY_URL`, `LEGACY_URL`, or `TEST_INVOICE_ID` only when testing a non-default stack.
Read checks retry transient startup failures for up to one minute because the legacy container can
be running before its HTTPS listener is ready.

The temporary direct-database write boundary and its rollback controls are recorded in
[`docs/architecture/phase-1-invoice-write-exception.md`](../docs/architecture/phase-1-invoice-write-exception.md).
