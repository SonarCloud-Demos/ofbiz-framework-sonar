# First-slice scorecard

Scores use 1 (unfavorable) to 5 (favorable). Reversibility and lower write/transaction coupling receive the highest weight.

| Candidate | Value 20% | Boundary clarity 15% | Low write risk 20% | Low coupling 15% | Reversibility 20% | Observability 10% | Weighted score |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Product catalog read | 4 | 4 | 5 | 3 | 5 | 4 | 4.30 |
| Content/media read | 3 | 4 | 5 | 4 | 5 | 3 | 4.15 |
| Work-management read | 3 | 3 | 5 | 3 | 5 | 3 | 3.75 |
| Party/customer read | 4 | 3 | 4 | 2 | 4 | 4 | 3.55 |
| Order read | 5 | 3 | 3 | 1 | 4 | 4 | 3.35 |
| Inventory visibility | 4 | 3 | 3 | 2 | 4 | 4 | 3.35 |

## Conditional selection

Product catalog read is selected as the first pilot because it provides visible user value, can begin from a shadow copy with OFBiz retaining write authority, and has an immediate route fallback. The catalog still depends on categories, pricing, content, and inventory visibility, so the selection is conditional rather than automatic.

## Pilot invariants

- Product and category identifiers, membership, status, effective dates, attributes, and displayed price references match the approved legacy result.
- Deleted/expired/restricted products do not become visible.
- Shadow freshness stays within the approved threshold and every mismatch is classified.
- Authorization and catalog/store scoping match or intentionally strengthen legacy behavior.
- The modern UI marker appears only on the modern route and rollback removes it with the route switch.

## Go/no-go gates

Proceed only after production route volume and latency are known, catalog owners accept the inventory, all read dependencies have contracts, reconciliation runs continuously for the agreed window, accessibility/security/load tests pass, and edge routing rollback is rehearsed. If any condition fails, reassess content/media read as the fallback pilot.

## Phase 0 pilot validation record

This record completed the sole remaining Phase 0 gate under the explicit waiver dated 2026-09-25. Validation selects and bounds the pilot; it does not assert that the pilot is implemented or production-ready.

| Field | Validated value |
| --- | --- |
| Selected pilot | Product catalog read |
| Business outcome | Prove that users can browse and search a modern product/category experience with equivalent visibility, scoping, and displayed product facts while retaining immediate fallback to OFBiz. |
| Included routes and data | Read representations corresponding to `/catalog/control/main`, `/catalog/control/advancedsearch`, `/catalog/control/keywordsearch`, `/catalog/control/FindProductById`, and `/catalog/control/FindCategory`. The pilot read model contains `Product`, `ProductCategory`, `ProductCategoryMember`, product/category status and effective dates, product attributes and features required by those views, and price references already displayed by the selected legacy result. Exact modern routes and contracts are defined during Phase 4 and mapped to these legacy behaviors before implementation. |
| Excluded behavior | All create/update/delete operations; catalog administration; price or promotion calculation and eligibility; inventory availability or reservation; media-binary migration; store configuration; subscriptions; exports; checkout/order behavior; write-side ECAs, scheduled jobs, and external integrations. Links to excluded behavior remain on legacy routes. |
| Invariants accepted | Stable product/category identifiers; category membership; status and effective-date filtering; required attributes/features; displayed price-reference parity without moving price authority; no visibility of deleted, expired, restricted, unauthorized, or out-of-scope records; approved store/catalog scoping; classified reconciliation mismatches; approved shadow freshness; and correct modern-marker behavior. |
| Rollback boundary accepted | OFBiz remains the sole writer and source of authority. The modern store is a disposable shadow read model. A server-controlled route switch returns the complete pilot to legacy without reverse synchronization or data recovery. |
| Accountable approver | Denis Troller, repository maintainer |
| Evidence reference | This decision record; `phase-0-discovery.md`; `critical-flows.md`; `data-ownership.md`; and generated catalogs under `docs/architecture/inventory/`. |
| Validation date | 2026-09-25 |
| Decision | Approved for Phase 0 exit and Phase 1/Phase 4 planning. Not approved for implementation rollout or production traffic until the go/no-go gates below are satisfied. |

## Validation rationale and carried gates

The pilot is approved because it is predominantly read-only, has visible user value, supports shadow comparison, and can fall back by routing while OFBiz retains write authority. The repository inventory also shows why its scope must remain narrow: the provisional product-catalog context contains hundreds of routes and services plus ECAs, a scheduled job, and integration candidates that are not part of this pilot.

Before implementation or production rollout, the responsible slice team must still:

- validate the selected legacy behaviors and entity/read-model fields with catalog owners;
- collect representative route volume, latency, error, security, and support baselines;
- define API/BFF contracts, SLOs, reconciliation tolerances, and an observation window;
- inventory and contract the pricing, content, identity/authorization, and store-scoping dependencies used by the selected views;
- pass security, accessibility, load, authorization, reconciliation, and marker tests; and
- rehearse shadow refresh failure, stale-data handling, and complete route rollback.

Failure to close any applicable gate requires revising the scope or reassessing content/media read as the fallback pilot.
