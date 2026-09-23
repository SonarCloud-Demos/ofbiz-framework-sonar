# ADR 0009: observability and reliability

- Status: Accepted
- Date: 2026-09-22
- Decision authority: Operations and security owners (governance register)

## Context

Distributed requests and asynchronous workflows require cross-component evidence, actionable SLOs, privacy-safe telemetry, and tested recovery. Logs alone cannot diagnose or operate the target architecture.

## Decision

Use OpenTelemetry for traces, metrics, and log correlation, exported to Application Insights, Log Analytics, and Azure Monitor. Propagate W3C trace context across HTTP and messages. Every service defines service-level indicators/objectives, alert thresholds, dashboards, ownership, runbooks, data-redaction rules, retention, and recovery evidence before production authority.

## Consequences

- Telemetry schemas and cardinality budgets are platform contracts.
- Sensitive fields are allowlisted/redacted and tested; broad payload logging is prohibited.
- Migration dashboards correlate gateway implementation, UI marker, service version, event ID, and reconciliation state without exposing secrets.
- Cutovers halt or roll back automatically/manual-by-runbook when agreed error-budget or invariant gates fail.
