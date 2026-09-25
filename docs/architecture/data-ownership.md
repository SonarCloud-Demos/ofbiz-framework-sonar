# Data ownership and migration map

## Current state

All domains use the shared OFBiz Entity Engine and transaction model. Repository-level ownership is inferred from the entity definition file and consuming service/component; it is not exclusive physical ownership. The `inventory/entities.csv` catalog is the row-level source of evidence.

| Data area | Current authority | Target authority | Earliest transfer condition |
| --- | --- | --- | --- |
| Products/categories/features | OFBiz product/datamodel entities | Product catalog service | Read reconciliation, consumer inventory, compatibility projection plan |
| Prices/promotions | OFBiz product pricing entities/services | Pricing/promotions service | Calculation parity and order-consumer contract tests |
| Inventory/facilities | OFBiz product inventory/facility entities | Inventory/facility service | Reservation/movement invariants and concurrency/load proof |
| Orders/returns/quotes | OFBiz order entities | Order service | Pricing, inventory, party, payment saga contracts established |
| Parties/contacts/relationships | OFBiz party entities | Party/customer service | Identity mapping, privacy, deduplication, address/contact validation |
| Shipments | OFBiz shipment entities | Fulfillment service | Inventory/order event ordering and carrier adapter ownership |
| Invoices/payments/ledger | OFBiz accounting entities | Accounting services | Audit, close-cycle, reconciliation, segregation-of-duty approval |
| Content | OFBiz content entities and files | Content/media service plus Blob Storage | Binary retention/checksum and reference migration proven |
| Work efforts | OFBiz work-effort entities | Work-management service | Calendar and party references represented by stable IDs/contracts |
| Manufacturing | OFBiz manufacturing entities | Manufacturing service | Product/inventory ownership already stable |
| HR | OFBiz human-resource/party entities | Workforce service | Privacy/access model and party boundary agreed |
| Marketing | OFBiz marketing entities | Marketing/sales service | Party consent and campaign/event contracts agreed |

## Authority state machine

Every entity set uses the plan's five states: `legacy-owned`, `shadowed`, `modern-read`, `modern-owned`, and `legacy-detached`. The data owner records the current state, transition time, reconciliation report, rollback boundary, and remaining consumers. Direct request-path dual writes are forbidden.

## Required invariant register

Before shadowing begins, each data area records:

- primary/business keys and uniqueness rules;
- monetary precision, currency, tax, and rounding rules;
- legal state transitions and optimistic/concurrency controls;
- referential rules that currently cross contexts;
- aggregate totals and reconciliation queries;
- retention, erasure, audit, and residency requirements;
- ordering/idempotency requirements for propagated changes;
- acceptable replication lag and cutover outage;
- backup/restore and forward-recovery boundaries.

## Ownership gaps

The repository cannot establish data steward names, production data classification, record volume, retention policy, or legal hold rules. Those fields are mandatory in the service catalog before a write-owning slice passes readiness. The data-migration team owns the gap register, not the eventual business data.
