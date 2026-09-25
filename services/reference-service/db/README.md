# Reference persistence patterns

Flyway owns the service schema under `src/main/resources/db/migration`. `V1__message_reliability.sql` supplies three transaction-local primitives:

- write the business change and `outbox_event` in one database transaction;
- claim `(consumer_name, message_id)` in `inbox_message` before applying an incoming fact, treating a key conflict as an already processed delivery; and
- claim `(operation_name, idempotency_key)` before a command, reject reuse with a different request hash, and persist the completed response for safe replay.

Publishers select unpublished outbox rows in occurrence order using `FOR UPDATE SKIP LOCKED`, publish the versioned fact, and set `published_at`. A crash after publish but before marking may redeliver, so consumers must always use the inbox key. Cleanup retention must exceed the maximum broker redelivery and client retry windows.
