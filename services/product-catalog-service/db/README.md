# Product catalog shadow store

OFBiz remains the system of record during Phase 4. In the local Compose runtime, the service refreshes
its shadow from OFBiz's token-protected `modernCatalogSnapshot` anti-corruption endpoint on each catalog
read. PostgreSQL stores the current product projection in `catalog_product`, its refresh checkpoint in
`catalog_sync_state`, and an ordered, append-only UPSERT/DELETE log in `catalog_change`. Applying a delta
and advancing its checkpoint are one transaction, so a failed refresh cannot expose partial state.

OFBiz product, price, and category mutations now append sequenced events to the `ModernCatalogChange`
transactional outbox through entity ECAs. The service bootstraps once from a snapshot, then polls this
token-protected feed by durable `source_cursor`. Replayed events and cursor advancement share one PostgreSQL
transaction; repeated batches are idempotent. Outside Compose, an atomic file store at
`/var/lib/catalog/shadow.tsv` remains available when database settings are absent. The bounded fixture in
`src/main/resources/catalog/ofbiz-shadow.tsv` is used only when neither live synchronization nor a durable
snapshot is configured. Once live synchronization is configured, refresh or persistence failures return
503 instead of presenting fixture data as though it came from OFBiz.

This proves durable local read synchronization, contract mapping, reconciliation, routing, and rollback
without implying a production replication design.

The local outbox is append-only. Production activation still requires an approved retention interval and
cleanup process whose low-water mark cannot pass the durable consumer cursor, plus monitoring for lag and
outbox growth.
