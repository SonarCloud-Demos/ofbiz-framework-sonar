# ADR-002: Supported implementation stacks

**Status:** Accepted for the platform foundation and first pilot. Revisit after the first read cutover and first write-ownership transfer.

**Decision:** Use Java with a current supported LTS runtime and Spring Boot for the first domain service/BFF, and TypeScript with React for the shell/features. Gradle remains the root build authority and pins non-JVM tooling. A second backend stack requires an ADR and operations ownership.

**Rationale:** The repository and team-facing build are Java/Gradle-centric; one backend stack minimizes early operational variance. TypeScript is appropriate for the required modern UI.
