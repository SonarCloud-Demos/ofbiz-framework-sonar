# ADR-003: Contract-first APIs and events

**Status:** Accepted for the platform foundation and first pilot. Revisit after the first read cutover and first write-ownership transfer.

**Decision:** Use OpenAPI for HTTP contracts, AsyncAPI/JSON Schema for events, and a common event envelope carrying ID, type/version, time, correlation, causation, and actor/tenant metadata when applicable. Commands use APIs; propagation uses facts/events.

**Rationale:** Versioned machine-readable contracts support code generation, compatibility checks, and independent deployment without sharing implementation models.
