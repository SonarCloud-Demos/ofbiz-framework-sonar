CREATE TABLE outbox_event (
    event_id UUID PRIMARY KEY,
    event_type TEXT NOT NULL CHECK (char_length(event_type) <= 200),
    aggregate_id TEXT NOT NULL CHECK (char_length(aggregate_id) <= 200),
    payload JSONB NOT NULL,
    correlation_id TEXT NOT NULL CHECK (char_length(correlation_id) <= 128),
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX outbox_event_unpublished_idx
    ON outbox_event (occurred_at)
    WHERE published_at IS NULL;

CREATE TABLE inbox_message (
    consumer_name TEXT NOT NULL CHECK (char_length(consumer_name) <= 200),
    message_id UUID NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    PRIMARY KEY (consumer_name, message_id)
);

CREATE TABLE idempotency_record (
    operation_name TEXT NOT NULL CHECK (char_length(operation_name) <= 200),
    idempotency_key TEXT NOT NULL CHECK (char_length(idempotency_key) <= 200),
    request_hash TEXT NOT NULL CHECK (char_length(request_hash) = 64),
    response_status INTEGER,
    response_body JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    PRIMARY KEY (operation_name, idempotency_key),
    CONSTRAINT idempotency_completion_consistent CHECK (
        (completed_at IS NULL AND response_status IS NULL AND response_body IS NULL)
        OR (completed_at IS NOT NULL AND response_status IS NOT NULL AND response_body IS NOT NULL)
    )
);
