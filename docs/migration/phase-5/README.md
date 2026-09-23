# Phase 5: product catalog vertical slice

## Status

**Repository implementation complete; live exit validation pending.** The canonical read API, six-field projection, accessible search UI, temporary OFBiz adapter, continuous transactional synchronization, shadow metrics, local cohort/rollback routing, staging/reconciliation SQL, fail-closed flags, and hybrid verification path are implemented. Production-safe profiling, deployed identity/cohort evidence, owner approvals, load evidence, and rollback/restore rehearsal remain mandatory before production authority changes.

Last reviewed: 2026-09-23

## Slice boundary

The pilot owns authenticated search of the six approved `Product` fields: `productId`, `productTypeId`, `internalName`, `brandName`, `productName`, and `description`. PostgreSQL owns the modern read projection. OFBiz remains the system of record and continues to own product creation and editing. Product links therefore transition to `/catalog/control/EditProduct`, and `/modern/catalog/products` must display **Mixed** until those legacy dependencies leave scope or are migrated.

The API implements the canonical contract in `contracts/api/product-catalog-v1.yaml`: exact product-ID filtering, case-insensitive escaped internal-name filtering, allowlisted sorting, deterministic product-ID tie-breaking, zero-based paging, a total count, and projection timestamp. It does not expose the generic OFBiz find service.

OFBiz exposes a temporary authenticated, catalog-view-authorized adapter at `/rest/product-catalog-migration/products`. It is unpublished from API documentation and returns only the canonical fields. The modern adapter requires a token file, requires HTTPS outside isolated local mode, and can enable synchronization and shadowing independently. Synchronization reads a complete product-ID-ordered snapshot, rejects empty/incomplete/stale results, stages it under a unique batch, promotes it in one transaction, and retains the prior projection on failure. Shadow mismatches and errors are metrics; shadow results never affect the user response and logs omit product data.

## Disabled-by-default rollout

`CATALOG_PILOT_READ_ENABLED` defaults to `false`; without an explicit `true`, the controller is not registered in Entra mode. The isolated `local` security mode enables it for the developer stack only. The Phase 4 public edge remains disabled independently. A production deployment must not enable either control until the gates below have signed evidence.

The controlled rollout order is:

1. Load a full, production-safe export into `catalog_product_stage` under a unique batch UUID. The export must use the approved six-field boundary and a monotonic source version.
2. Run `operations/reconcile-product-batch.sql` against the current projection. Record counts and identifiers/field names only; do not publish unrestricted descriptions.
3. For initial backfill or an approved full refresh, run `operations/promote-product-batch.sql`, then reconcile again. Any mismatch blocks promotion or traffic.
4. Keep the projection current through the approved legacy adapter and repeat reconciliation continuously. The adapter must tolerate duplicates and out-of-order versions; older `source_version` values cannot overwrite newer rows.
5. Shadow the same allowlisted read samples against OFBiz and the projection. Compare normalized fields, ordering, totals, latency, and failures without returning shadow results to users.
6. Add staff/test cohorts at APIM or the feature-management layer, preserving legacy as the default. Record cohort membership changes and route decisions. Do not infer cohorts from mutable display names or client headers.
7. Enable `CATALOG_PILOT_READ_ENABLED=true` only for the approved service deployment and cohort route. Keep the UI marker **Mixed** while edit navigation remains legacy.
8. After the agreed observation and rollback window, remove the shadow/fallback adapter only through a separately reviewed change. This repository state does not authorize that removal.

## Local hybrid verification

Initialize and start OFBiz on the host if it is not already running:

```sh
./gradlew loadAll
./gradlew ofbiz
```

Obtain a local REST token using the documented OFBiz REST authentication flow and store only the token in a mode-restricted file. Do not put the administrator password or token in shell history, Compose environment variables, or Git. Then set `CATALOG_LEGACY_TOKEN_FILE` to that file and run:

```sh
./modern start-hybrid
./modern smoke-hybrid
```

The hybrid override connects the modern service to host OFBiz over the local HTTP connector, enables one-minute synchronization and shadowing, and keeps the token in a Compose secret. The local gateway proxies retained `/catalog/*` pages to OFBiz over its repository TLS certificate. Its disabled certificate verification is confined to this local gateway; deployed APIM/private origins must validate TLS normally.

After local sign-in, `POST /auth/local/pilot/modern` assigns the developer-only staff cohort and routes `/catalog/control/FindProduct` to the modern **Mixed** page. `POST /auth/local/pilot/legacy` clears it and rolls the route back to OFBiz. `forceLegacy=true` always selects OFBiz for the explicit Legacy catalog link. Production cohorts must come from approved server-side identity/feature management, never from this local cookie mechanism.

## Required evidence and stop conditions

Before any live cohort, obtain the Phase 1 production profile, domain/data/security approvals, source read-only trace, authenticated/forbidden/authorized HTTP results, invalid-input results, shadow-match target, maximum projection-age target, accessibility review, and load evidence against the 300 ms P95 service SLO. Reconciliation mismatch, stale projection, unexplained count drift, elevated errors/latency, or an identity-policy failure immediately stops expansion.

Rollback disables the cohort route and `CATALOG_PILOT_READ_ENABLED`, then sends users to `/catalog/control/FindProduct`. Because OFBiz owns all writes, rollback has no competing-write merge. Preserve the projection and reconciliation record for diagnosis. Restore must be rehearsed from an approved full export before authority is declared.

## Repository verification

Run `./modern test` for unit/API tests and `./modern start && ./modern smoke` for the isolated authenticated UI/API journey. Use the hybrid commands above to exercise the OFBiz transition, continuous synchronization, shadow comparison, staff cohort, and rollback locally. Production load, recovery, identity, cohort governance, and observation-window evidence still require approved environment dependencies and cannot be inferred from local fixtures.
