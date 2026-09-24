# ADR-005: Local platform fidelity

**Status:** Accepted for the platform foundation and first pilot. Revisit after the first read cutover and first write-ownership transfer.

**Decision:** Use Docker Compose for the integrated runtime, Testcontainers for isolated integration tests, real PostgreSQL, local/emulated narrow adapters for Azure services, and one local TLS origin. No committed authentication bypass exists.

**Rationale:** Developers can work without Azure while testing the same protocols and routing semantics.
