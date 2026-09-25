# ADR-007: UI composition and marker

**Status:** Accepted for the platform foundation and first pilot. Revisit after the first read cutover and first write-ownership transfer.

**Decision:** Build one modular shell with feature modules, not independently deployed micro-frontends initially. The trusted shell renders the **Modern experience** marker from the controlled route catalog. Legacy pages use full-page same-origin transitions, never iframes.

**Rationale:** It enables route-by-route strangling with consistent security/accessibility and lower composition risk.
