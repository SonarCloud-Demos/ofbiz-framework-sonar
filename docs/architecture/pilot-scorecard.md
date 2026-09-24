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

This record is the sole remaining Phase 0 completion gate under the explicit waiver dated 2026-09-25. Validation here selects and bounds the pilot; it does not assert that the pilot is implemented or production-ready.

| Field | Required value |
| --- | --- |
| Selected pilot | Product catalog read, or a documented replacement |
| Business outcome | Specific user/business value to test |
| Included routes and data | Explicit route and entity/read-model scope |
| Excluded behavior | Writes and other behavior intentionally outside the pilot |
| Invariants accepted | Approved list from this scorecard and `critical-flows.md` |
| Rollback boundary accepted | OFBiz remains writer; route can return completely to legacy |
| Accountable approver | Named person or recorded governance body |
| Evidence reference | Ticket, decision record, or meeting record |
| Validation date | ISO date |
| Decision | Approved, rejected, or revise |

Phase 0 remains open while any field is blank. Once approved, update `phase-0-discovery.md` from “pending validation” to “complete” and retain this record as decision evidence.
