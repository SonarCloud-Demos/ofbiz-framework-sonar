# ADR-008: Monorepo build and dependency locking

**Status:** Accepted for the platform foundation and first pilot. Revisit after the first read cutover and first write-ownership transfer.

**Decision:** `./gradlew build` builds and verifies all old/new code and IaC validation. Ecosystem-native lockfiles are generated/verified by Gradle and committed. The single SonarQube project analyzes the entire repository.

**Rationale:** One build graph prevents local/CI drift and preserves whole-codebase analysis throughout migration.
