# Product catalog pilot data profile

## Scope and provenance

This is a repeatable local profile of the H2 dataset created by `./gradlew loadAll` on 2026-09-23. It proves the profiling and reconciliation shape; it is not production evidence.

## Local results

| Measure | Result |
| --- | ---: |
| Product rows | 72 |
| Distinct product types | 10 |
| Missing/blank `internalName` | 0 |
| Missing/blank `productName` | 3 |
| Missing/blank `brandName` | 72 |
| Missing/blank `description` | 1 |

Largest local type populations are `FINISHED_GOOD` (36), `DIGITAL_GOOD` (14), `SERVICE_PRODUCT` (11), and `ASSET_USAGE` (4). The remaining types contain two or fewer rows each.

## Contract consequences

- `productId` is the reconciliation key and external stable identifier.
- `productTypeId` is required for the local data but must still be validated against production exceptions.
- `internalName`, `brandName`, `productName`, and `description` remain nullable in the API because local completeness does not prove production completeness and `brandName` is absent throughout the local dataset.
- API ordering must use an allowlisted field plus `productId` as the deterministic tie-breaker.
- Backfill and incremental reconciliation compare row presence and a canonical hash of the six projected fields after an explicitly defined null/Unicode normalization.

## Reconciliation query shape

For each run, compare:

1. total legacy and projection row counts for the included population;
2. missing IDs on either side;
3. duplicate projection IDs;
4. per-field null/blank counts; and
5. per-ID canonical field hashes.

Any mismatch blocks authority. Reconciliation output contains identifiers and field names, not unrestricted product descriptions.

## Production profile gate

Before Phase 5 implementation traffic is enabled, the data owner must run the equivalent profile against a production-safe snapshot and record row count, growth rate, type distribution, field completeness, duplicate keys, maximum field lengths, sensitivity, retention, query selectivity, peak request rate, and p50/p95/p99 latency. Production values belong in the approved operational evidence store when repository publication is restricted.

