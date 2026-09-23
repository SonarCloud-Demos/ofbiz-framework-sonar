# Phase 1: boundary discovery and behavior characterization

## Status

**Complete.** The read-only product/catalog journey was approved as the Phase 1 pilot and passed the Phase 1 exit review on 2026-09-23. Legacy OFBiz remains authoritative; this decision completes discovery and does not authorize implementation traffic or cutover.

Last reviewed: 2026-09-23

## Scope and evidence rules

Phase 1 maps behavior rather than treating an OFBiz component as a future service. Discovery must cover Java and Groovy implementations plus entity, service, controller, widget, ECA, and scheduled-job XML. Static repository evidence is a hypothesis until it is checked against runtime traces and business workshops.

The catalogs use these states:

- `observed` means the repository contains direct evidence;
- `hypothesis` means a proposed boundary still needs owner and runtime validation;
- `approved` is reserved for a decision recorded by the accountable owners.

Unknown ownership or security facts remain `TBD`; they are never inferred from a package name.

## Initial context map

The machine-readable map is [context-map.yaml](../../../architecture/context-map.yaml). The first discovery scope contains Product Catalog, Billing and Payments, Notifications, Party, and the Legacy OFBiz boundary. The first three correspond to the candidates approved for evaluation in Phase 0.

| Candidate | Static evidence | Initial boundary observation | Evidence still required |
| --- | --- | --- | --- |
| Notifications | `sendMail` and `sendMailFromScreen` are framework services; callers occur in order, accounting, party, product, shipment, security, and work-effort code. Communication state is stored in party/marketing entities. | Delivery is cross-cutting, but template selection, communication history, and domain-trigger ownership are entangled. A user-visible pilot route is not yet identified. | Caller frequency, async/job paths, delivery side effects, retry semantics, PII classification, and candidate UI journey. |
| Read-only product/catalog | `/catalog/control/FindProduct` is authenticated. `Product` is defined in the shared data model; product services include `findProductById`, writes, pricing, content, review, and association behavior. | A read projection can be isolated from writes, but the projection boundary must exclude pricing, inventory, content, and promotion ownership unless explicitly contracted. | Query mix, required fields, permissions, volumes, freshness target, consumers, and route-level latency baseline. |
| Read-only invoice search/detail | `/accounting/control/findInvoices`, `/ar/control/findInvoices`, and `/ap/control/findInvoices` expose related legacy searches. `Invoice` is a shared data-model entity and invoice logic resides in accounting. | Search/detail is reversible and read-only, but financial sensitivity, role checks, party data, status interpretation, and accounting joins increase risk. | Exact detail route, permission matrix, query/join inventory, data classification, volumes, reconciliation keys, and audit requirements. |

The read-only product/catalog journey is the selected pilot. Its observed contract, scope, initial characterization coverage, and rollback boundary are recorded in [product-catalog-pilot.md](product-catalog-pilot.md).

## Discovery inventories

### Service calls and side effects

The candidate inventory records confirmed entry points. The selected product search slice has a complete journey-level inventory in [product-catalog-inventory.md](product-catalog-inventory.md); it intentionally does not claim to inventory the entire Product component.

| Candidate | Entry point | Authentication | Known implementation or definition | Status |
| --- | --- | --- | --- | --- |
| Notifications | `sendMail` | Service metadata to verify per call path | `framework/common/servicedef/services_email.xml` | observed |
| Notifications | `sendMailFromScreen` | Service metadata to verify per call path | `framework/common/servicedef/services_email.xml` | observed |
| Product catalog | `findProductById` | `auth="true"` | `applications/product/servicedef/services.xml` | observed |
| Invoice search | `performFind` | `auth="false"`; controller security and entity permissions require separate verification | `framework/common/servicedef/services.xml` | observed |

An unauthenticated service declaration does not imply an unauthenticated browser journey. Characterization must test the effective controller, service, and entity-permission chain.

### UI routes and marker state

Routes are cataloged in [route-ownership.yaml](../../../architecture/route-ownership.yaml). The existing route remains `legacy`; the planned modern search route is recorded as `mixed` because result navigation returns to legacy product editing. Runtime marker conformance remains a Phase 3/4 implementation gate.

### Data ownership

Entities are cataloged in [data-ownership.yaml](../../../architecture/data-ownership.yaml). During discovery, Legacy OFBiz remains the sole authority and writer. Proposed modern owners are hypotheses, not permission grants or implementation commitments.

## Required characterization work

For each candidate, capture golden requests and responses using deterministic seed records and test:

1. anonymous, authenticated, unauthorized, and authorized behavior;
2. field values, ordering, pagination, localization, and error mapping;
3. entity writes, service/ECA invocations, messages, email, files, and scheduled work;
4. timeout and dependency-failure behavior;
5. stable reconciliation keys and legacy-versus-projection comparisons.

Read-only candidates must prove through before/after database and side-effect assertions that the characterized journey performs no business writes.

## Exit checklist

- [x] Local runtime characterization and before/after entity evidence cover the selected journey.
- [x] The approved pilot decision validates the selected context boundary and terminology.
- [x] Service, ECA, entity, job, consumer, and UI inventories are complete for the selected slice.
- [x] A repeatable local data profile records quality, volume, sensitivity assumptions, and reconciliation keys.
- [x] Critical search behavior and read-only side effects have characterization tests; effective HTTP authorization and failure injection are contract requirements for implementation.
- [x] The accountable user approved the scored pilot and its rollback path.
- [x] The selected legacy and planned routes, draft API, implementation state, and marker state are recorded.

The evidence and residual gates are summarized in [exit-review.md](exit-review.md). Production traffic, identity, latency, database-statement, and production-safe data-profile evidence remain Phase 5 entry requirements; Phase 1 completion must not be read as evidence that those production controls already exist.
