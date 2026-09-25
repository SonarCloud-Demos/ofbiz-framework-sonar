# Provisional context map

## Boundary rules

This map is a migration hypothesis derived from repository paths and metadata. It is not a claim that current OFBiz components are already isolated. Current entities, services, ECAs, and transactions cross these boundaries freely.

| Target context | Current primary evidence | Provisional owner role | Key dependencies |
| --- | --- | --- | --- |
| Product catalog | `applications/product`, product entity model | Catalog product team | Party, pricing, inventory, content |
| Pricing and promotions | Product price/promotion service models | Pricing product team | Catalog, party/customer, order |
| Inventory and facility | Inventory/facility service models | Inventory product team | Catalog, party, order, fulfillment, manufacturing |
| Order management | `applications/order`, order entity model | Order product team | Party, catalog, pricing, inventory, accounting |
| Shipping and fulfillment | Shipment model and product shipment services | Fulfillment product team | Order, inventory, party, accounting |
| Party and customer | `applications/party`, party entity model | Customer product team | Identity, content, order, accounting |
| Accounting | `applications/accounting`, accounting model | Finance product team | Orders, party, products, fulfillment; last major extraction |
| Content and media | `applications/content`, content model | Content product team | Party, catalog, work management |
| Work management | `applications/workeffort`, work-effort model | Work product team | Party, content |
| Manufacturing | `applications/manufacturing`, manufacturing model | Manufacturing product team | Catalog, inventory, work, accounting |
| Marketing and sales | `applications/marketing`, marketing model | Marketing product team | Party, catalog, work |
| Workforce | `applications/humanres`, HR model | Workforce product team | Party, work, accounting |
| Identity and access | Framework security and `securityext` | Identity platform team | Party identity mapping and every protected context |
| Experience platform | Themes and `commonext` | Experience platform team | All user-facing contexts through contracts only |
| API edge | `framework/rest-api` | Platform team | Published APIs; no domain ownership |
| Legacy platform | Remaining framework engines | Legacy platform team | Temporary shared runtime during strangling |
| Shared legacy data | Residual `applications/datamodel` definitions | Data migration team | Must shrink as authority transfers |

Marketing/sales and workforce are included here even though the initial target plan under-specified them; the repository contains material code and data for both. Their final service decomposition requires business validation rather than silent omission.

## Intended dependency direction

```text
UI shell -> feature UI -> BFF/API contracts
                           |
                           v
                    owning domain service
                           |
             +-------------+-------------+
             v                           v
      owned database                domain events

modern service -> temporary legacy adapter -> OFBiz
```

No modern service imports another service's implementation or reads its database. Runtime collaboration is through versioned APIs/events. Temporary legacy adapters translate models and are removed after detachment.

## Current coupling hotspots

- Entity and Service Engines are shared by almost all application domains.
- Package analysis reports 71 top-level Java package nodes and 399 dependencies, with `base.util`, `entity`, `service`, `security`, `webapp`, and `widget` as hubs.
- Route-to-service dispatch, service ECAs, entity ECAs, and scheduled jobs create edges not represented by Java imports.
- Accounting, orders, product/inventory, party, and fulfillment share transaction and data boundaries; they require invariant discovery before writes move.
- Themes/widgets and security/session behavior span every webapp.

## Boundary-validation checklist

For each context, the owner must validate the generated inventory, classify inbound/outbound integrations, identify authoritative entities, document cross-context invariants, and approve the API/event boundary. A context split is rejected if it merely mirrors a package/table or requires synchronous implementation-level coupling to operate.
