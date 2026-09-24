# ADR-004: Database ownership

**Status:** Accepted for the platform foundation and first pilot. Revisit after the first read cutover and first write-ownership transfer.

**Decision:** Use PostgreSQL with exclusive schema/database credentials per service. No modern service reads OFBiz or another service database in steady state. Use outbox/inbox and sagas rather than distributed transactions.

**Rationale:** This creates enforceable ownership and avoids a distributed monolith while retaining relational guarantees inside a bounded context.
