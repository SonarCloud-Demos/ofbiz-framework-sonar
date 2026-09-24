# Phase 0 discovery record

## Status

Phase 0 repository discovery is implemented and reproducible. By explicit project decision on 2026-09-25, pilot validation is the sole remaining Phase 0 completion gate. Named-owner acceptance, exhaustive runtime validation, and production baseline collection are waived as Phase 0 exit criteria. They remain recorded risks and mandatory readiness work for any affected production slice.

Run the inventory and its tests with:

```text
python3 -m unittest migration/discovery/test_generate_inventory.py
python3 migration/discovery/generate_inventory.py
```

The generator reads OFBiz component descriptors, controllers, service models, entity models, ECAs, and scheduled seed data. It writes deterministic CSV catalogs and `summary.json` under `docs/architecture/inventory/`.

## Repository baseline

| Surface | Records | Catalog |
| --- | ---: | --- |
| Components | 33 | `inventory/components.csv` |
| Web applications | 29 | `inventory/webapps.csv` |
| Request routes | 3,255 | `inventory/routes.csv` |
| Services | 3,517 | `inventory/services.csv` |
| Entities and view entities | 1,118 | `inventory/entities.csv` |
| Service/entity ECAs | 356 | `inventory/ecas.csv` |
| Seeded scheduled jobs | 22 | `inventory/scheduled-jobs.csv` |
| Integration candidates | 223 | `inventory/integrations.csv` |

No selected XML input failed parsing. Integration rows are candidates based on registered webapps and service metadata/name evidence; each outbound candidate requires runtime validation because source metadata alone cannot prove an endpoint is configured or used.

## Phase 0 deliverables

- `context-map.md` defines provisional bounded contexts, dependencies, and ownership roles.
- `data-ownership.md` defines current and target authority plus transfer gates.
- `critical-flows.md` records critical journeys, static evidence, invariants, and test gaps.
- `baseline.md` separates captured static facts from required production telemetry.
- `phase-0-adrs.md` records the foundation decisions needed for the pilot.
- `engineering-standards.md` establishes delivery and production-readiness gates.
- `pilot-scorecard.md` scores candidate first slices and selects catalog read as a conditional pilot.
- `inventory/*.csv` provides row-level traceability back to repository files.

## Ownership validation workflow

Every catalog row carries `target_context`, `owner_role`, and `owner_status`. `owner_status` is deliberately `provisional-repository-inferred`; the repository cannot identify accountable people. Before a slice enters implementation:

1. The proposed product and technical owners review every route, service, entity, ECA, job, and integration assigned to their context.
2. Conflicts are resolved in `migration/discovery/context-map.json`, never by editing generated CSV files.
3. The inventory is regenerated and reviewed in the same change.
4. The service catalog records named people/on-call groups in the organization-owned system of record.
5. The slice cannot pass readiness while any included row is `unclassified-legacy` or lacks an accepted owner.

## Exit-criteria assessment

| Phase 0 criterion | Status | Evidence or remaining gate |
| --- | --- | --- |
| Critical routes and scheduled/integration flows have an owner and dependency record | Waived for Phase 0 exit | All discovered rows have a provisional owner role and source evidence. Named-person acceptance and production-use validation move to slice readiness. |
| Candidate boundaries and first pilot have evidence-based ADRs | Complete for pilot | Context map, ADRs, and pilot scorecard select product-catalog read conditionally. |
| Performance, correctness, security, and operational baselines are measurable | Waived for Phase 0 exit | Static counts and a future measurement design exist. Concrete values remain required before an affected production cutover. |
| First slice has invariants, rollback boundaries, and acceptance tests | Pending validation | Catalog-read invariants and gates are in `critical-flows.md` and `pilot-scorecard.md`. Explicit pilot validation is the only remaining Phase 0 gate. |

Phase 0 becomes complete when the pilot validation record in `pilot-scorecard.md` is filled in and approved. The waiver does not authorize production cutover, waive security or correctness controls, establish data ownership, or satisfy a slice's readiness/completion gates. Waived discovery work must be completed just in time for every slice that relies on it.

## Explicit Phase 0 waiver

**Decision date:** 2026-09-25

**Decision:** Accept the residual uncertainty from provisional ownership, incomplete production telemetry, and incomplete runtime validation for the purpose of closing Phase 0.

**Condition:** Validate and approve the selected pilot explicitly.

**Scope:** Phase 0 exit only.

**Not waived:** production security, authorization, correctness invariants, regulatory controls, data-owner approval, operational readiness, rollback proof, reconciliation, or any slice readiness gate.

**Risk treatment:** Carry each unresolved item in the applicable slice backlog and close it before that slice can receive production traffic or write ownership.
