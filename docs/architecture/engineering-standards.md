# Migration engineering and readiness standards

## Mandatory delivery gates

- Every deployable is registered in the root Gradle lifecycle and succeeds under `./gradlew build`.
- Dependency manifests have committed lockfiles; generation is idempotent and CI uses strict/frozen resolution.
- APIs/events are versioned, compatibility-tested, authenticated where applicable, and owned.
- Services own their data and never import another service implementation or query another service database.
- Every request/message carries correlation metadata; logs are structured and exclude secrets/sensitive payloads.
- Tests cover unit, database integration, contracts, authorization, failure/retry, and the slice's golden flow.
- UI routes meet WCAG accessibility gates, CSP/CSRF requirements, and modern-marker positive/negative tests.
- Terraform plans, policy/security checks, backups, restore tests, dashboards, alerts, SLOs, runbooks, and on-call ownership exist before production.

## API and event conventions

Use stable resource-oriented APIs, explicit idempotency for retriable commands, standard error envelopes, UTC timestamps, documented pagination, and bounded timeouts. Events are immutable facts in past tense. Producers retain schema compatibility; consumers tolerate additive fields and are idempotent. Dead-letter ownership and replay are tested.

## Security and privacy

Threat-model every slice. Deny by default, validate tokens at each boundary, scope managed identities, keep secrets in Key Vault, pin/scan artifacts, and record privileged/business-sensitive actions in append-only audit evidence. Classify/minimize personal and financial data and document retention/erasure behavior.

## Production readiness

A service catalog entry must name product, technical, data, and on-call owners; dependencies; data classification; SLOs; RPO/RTO; capacity; dashboards; alerts; runbooks; backup/restore evidence; cost tags; and decommission dependencies. A slice cannot launch with provisional ownership or unmeasured acceptance thresholds.

## Exception process

An exception identifies the violated rule, business reason, risk, compensating control, owner, expiry, and removal issue. Architecture and security approve it. Expired exceptions fail the delivery gate; permanent undocumented exceptions are prohibited.
