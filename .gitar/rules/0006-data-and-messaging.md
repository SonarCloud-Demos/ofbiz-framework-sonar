# ADR 0006: data and messaging

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Data owner and solution architect (governance register)

## Context

The shared Entity Engine database is a central coupling mechanism. Modern services need exclusive ownership, local transactions, reliable event publication, and independently testable recovery.

## Decision

Use Azure Database for PostgreSQL Flexible Server as the default operational relational store and Azure Service Bus Premium for business commands/events. A service exclusively owns its database objects. Use transactional outbox publication, idempotent consumers, explicit schema versions, and sagas/compensations for cross-service workflows. Use CDC only as a time-boxed migration mechanism approved per source.

## Consequences

- Cross-service SQL and distributed database transactions are prohibited.
- Schema-per-service on a shared server may be a transitional cost choice only with separate identities/grants and a documented separation trigger.
- Reporting uses event-built projections rather than operational cross-service joins.
- Backup, restore, retention, reconciliation and deletion are defined per data owner.
