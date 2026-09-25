# Critical business flows and golden tests

## Purpose

These flows anchor correctness baselines and extraction sequencing. Static evidence identifies entry routes and services; business owners must validate variants, production importance, and expected results. Each flow becomes an automated golden test before its participating write paths move.

| Flow | Static entry/evidence | Participating contexts | Critical invariants | Migration/rollback boundary |
| --- | --- | --- | --- | --- |
| Browse/manage catalog | `/catalog` routes; `createProduct`; product/category services | Catalog, pricing, content | Stable IDs, category membership, attributes, effective dates | Read pilot can route back instantly while OFBiz remains writer |
| Quote/cart/checkout/order | `/ordermgr/control/quickcheckout`, `processorder`, `storeOrder` | Order, party, catalog, pricing, inventory, accounting | Price/tax totals, authorization, reservation, idempotent submission, legal status transitions | Read before write; write cutover only after saga and single-writer proof |
| Inventory reservation/movement | `reserveProductInventory*`; inventory ECAs | Inventory, catalog, order, fulfillment, manufacturing | No negative/duplicate reservation, quantity conservation, ordered status events | Stop writes, drain, reconcile quantities/reservations, switch owner |
| Shipment fulfillment | Shipment services and pick/pack webapps | Fulfillment, order, inventory, party | Shipment/item quantities, ownership transfer, tracking uniqueness | Requires order/inventory event contracts; forward recovery after dispatch |
| Invoice/payment/ledger | `/accounting/control/createInvoice`, `createPayment`; ledger ECAs | Accounting, order, party | Debits equal credits, immutable posting, currency precision, application totals | Late migration; parallel close and formal finance sign-off |
| Party/customer maintenance | Partymgr routes; `createPerson` | Party, identity, content | Identity mapping, contact validity, consent/privacy, deduplication | Identity bridge and reversible read cohort before write ownership |
| Manufacturing planning/run | Manufacturing routes; production-run/MRP services; nightly jobs | Manufacturing, catalog, inventory, work | BOM/routing version, material conservation, cost basis, job status | Product/inventory must be stable; rehearse nightly reconciliation |
| Scheduled order processing | 11 order `JobSandbox` rows | Order, inventory, payment, notification | Exactly-once business effect, retry safety, schedule/time zone | Disable legacy scheduler only after modern worker lease and replay proof |
| Secret operations | Secret auto-sync/audit purge scheduled jobs | Legacy platform, identity/platform | Least privilege, rotation continuity, audit retention | Platform replacement must prove audit/restore before job removal |

## Golden-test structure

Every flow test captures input fixtures, authenticated actor/permissions, initial records, synchronous result, committed entity changes, ECA side effects, emitted notifications/integrations, scheduled follow-up, audit evidence, and monetary/hash totals where applicable. Tests run against legacy first to capture approved behavior, then against shadow/modern implementations with intentional differences documented.

## Production validation gates

- Confirm the top routes by request volume rather than assuming route count equals importance.
- Trace runtime service/ECA/job executions with correlation IDs for at least one normal peak and one relevant business cycle.
- Interview support/operations for manual recovery paths not expressed in source.
- Validate month-end, renewal, back-order, failed-payment, and retry behavior.
- Record accepted correctness tolerances; financial and quantity invariants default to exact equality.
