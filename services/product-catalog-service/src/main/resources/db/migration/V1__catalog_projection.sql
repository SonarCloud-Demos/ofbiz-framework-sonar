CREATE TABLE catalog_product (
    product_id TEXT PRIMARY KEY,
    product_name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    source_version BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE integration_outbox (
    event_id UUID PRIMARY KEY,
    aggregate_type TEXT NOT NULL,
    aggregate_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    event_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX integration_outbox_unpublished_idx
    ON integration_outbox (occurred_at)
    WHERE published_at IS NULL;

CREATE TABLE handled_message (
    consumer_name TEXT NOT NULL,
    message_id UUID NOT NULL,
    handled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (consumer_name, message_id)
);

INSERT INTO catalog_product (product_id, product_name, description, source_version)
VALUES
    ('GZ-1000', 'Gizmo', 'Deterministic local golden-path seed product.', 1),
    ('WG-1111', 'Widget', 'Second deterministic product for search tests.', 1);
