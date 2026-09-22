# Product catalog pilot inventory

## Boundary

This inventory is complete for the approved search journey `/catalog/control/FindProduct`. It is not an inventory of the entire OFBiz product component. Product detail and every write capability remain legacy and outside this pilot.

## Request and UI inventory

| Kind | Artifact | Responsibility | Pilot disposition |
| --- | --- | --- | --- |
| Web application | `applications/product/ofbiz-component.xml` | Mounts `/catalog` with base permission `OFBTOOLS,CATALOG` | Legacy boundary |
| Controller | `applications/product/webapp/catalog/WEB-INF/controller.xml` | Requires HTTPS and authentication for `FindProduct` | Characterized route |
| View | `ProductScreens.xml#FindProduct` | Composes search form and results grid | Replace with route-owned modern page |
| Search form | `ProductForms.xml#FindProduct` | Accepts product ID and internal name; opts into empty-condition search | Contract input source |
| Results grid | `ProductForms.xml#ListProducts` | Calls `performFindList`, sorts, pages, and renders six fields | Contract output source |
| Navigation | `ProductForms.xml#ListProducts.productId` | Links each result to `EditProduct` | Remains legacy; modern page is `Mixed` |
| Menu | `CatalogMenus.xml#products` | Links operators to `FindProduct` | Gateway-controlled transition |

## Service and execution inventory

| Entry | Authentication | Reads | Writes or side effects | Disposition |
| --- | --- | --- | --- | --- |
| `performFindList` | Service metadata says `auth=false`; protected by the controller/webapp in this journey | Delegates to `performFind` and returns a bounded page | None observed during search characterization | Do not expose; replace with authenticated catalog API |
| `performFind` | `auth=false` | Builds conditions through `prepareFind` and delegates to `executeFind` | None observed | Framework implementation detail |
| `prepareFind` | `auth=false` | Entity metadata and request fields | None observed | Framework implementation detail |
| `executeFind` | `auth=false` | Entity Engine query | None observed | Framework implementation detail |

No service ECA rule is attached to the four read services. Product entity ECA rules run on create/update operations, including keyword indexing, but the characterized search performs no Product or ProductKeyword writes.

## Data inventory

| Entity | Fields used | Access | Pilot ownership |
| --- | --- | --- | --- |
| `Product` | `productId`, `productTypeId`, `internalName`, `brandName`, `productName`, `description` | Read and count | Legacy source; modern read projection |
| `ProductType` | `productTypeId`, localized description | Read for legacy rendering | Reference fact; denormalize only with explicit localization/freshness rules |

The search journey does not require price, inventory, promotion, category, content, image, supplier, party, review, configuration, or order entities. Those facts are explicitly excluded from the projection.

## Jobs and events

The product component schedules `purgeOldStoreAutoPromos`, but it does not serve this journey. No scheduled job, service ECA, message, email, file operation, or event publication is in the synchronous search path. A future projection feed must be selected separately; Phase 1 does not authorize database polling or cross-service table access as the permanent mechanism.

## Consumers and coupling

Static Java package references show inbound product-package consumers from Accounting, Manufacturing, Order, Security, Shipment, Category, Price, Store, Config, and Supplier code. These consumers demonstrate that the full `Product` model is broadly coupled, but they are not consumers of the selected operator search route. The pilot projection is therefore intentionally narrower than the shared Product entity or Java package.

Known direct UI consumer: authenticated catalog operators. Production access logs and identity telemetry are still required to quantify users, query mix, and peak load.

## Security model

- The legacy controller requires HTTPS and an authenticated session.
- The catalog web application requires an OFBiz manager permission plus Catalog access; `CATALOG_VIEW` is the least-privilege read permission represented in seed data.
- The underlying generic find services are unauthenticated and must never become the external modern API.
- The modern API requires an authenticated principal and `catalog.read`; the edge and service both enforce it.
- Search input is represented as typed query parameters. Sort fields are allowlisted, page size is bounded, and no entity name or raw condition crosses the boundary.
- Results may contain commercially sensitive product descriptions. The local dataset contains no evidence sufficient to downgrade production classification.

## Failure inventory

| Failure | Legacy observation | Required modern behavior |
| --- | --- | --- |
| No session | Controller redirects/challenges through OFBiz login flow | `401` API response; shell starts approved login flow |
| Authenticated without catalog access | Webapp permission boundary denies access | `403`, audited without leaking product data |
| Empty filter without opt-in | `listSize=0`, `list=null` | `200` with an empty `items` array |
| No matches | Empty page | `200` with empty `items` and `total=0` |
| Invalid sort | Legacy generic-service behavior is not a safe contract | `400` with allowlisted-sort validation error |
| Database unavailable/timeout | Legacy service/rendering error path | Bounded timeout and sanitized `503`; no fallback from API to direct database |
| Projection stale | Not represented in legacy | Serve only within approved freshness SLO; emit lag metric and alert |

