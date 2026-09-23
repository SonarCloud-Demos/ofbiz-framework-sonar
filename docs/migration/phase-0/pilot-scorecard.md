# Pilot selection scorecard

## Method

Score each candidate from 1 (favorable/low) to 5 (unfavorable/high), except business value and learning value, where 5 is best. Record evidence; do not score from intuition alone.

| Criterion | Weight | Meaning |
| --- | ---: | --- |
| Business value | 3 | User or operational value delivered by the slice |
| Learning value | 3 | Exercises edge, identity, data, UI, deployability and observability |
| Data complexity | 3 | Volume, relationships, quality, history and ownership ambiguity |
| Coupling | 3 | Calls, shared tables, ECA rules, jobs and consumers |
| Irreversibility | 4 | Financial/legal impact and difficulty of compensation |
| Security/compliance risk | 4 | Sensitivity, privilege and audit exposure |
| Rollback feasibility | 4 | Ability to restore legacy authority safely and quickly |
| UI journey size | 2 | Number/complexity of pages and interaction states |
| Operational load | 2 | Scale, latency, availability and on-call complexity |
| Team readiness | 3 | Available ownership and domain/platform skills |

Use the weighted evidence in a decision workshop; do not treat the total as an automatic answer.

## Candidate worksheet

| Candidate | Value | Learning | Data | Coupling | Irreversibility | Security | Rollback | UI | Operations | Readiness | Evidence link |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Notifications | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | Phase 1 |
| Read-only product/catalog journey | 3 | 5 | 2 | 2 | 1 | 2 | 1 | 2 | 2 | 3 | [Phase 1 evidence](../phase-1/product-catalog-pilot.md) |
| Read-only invoice search/detail | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | TBD | Phase 1 |

## Mandatory pilot properties

The pilot must:

- be a real, user-visible vertical slice with a modern UI route and trusted marker;
- exercise service build/deployment, API contracts, data ownership or projection, identity, Terraform, telemetry and rollback;
- have characterization tests and measurable legacy-versus-modern comparison;
- be reversible without financial or regulatory ambiguity;
- have named business, domain, platform, security, data and operational owners;
- produce reusable learning without becoming a throwaway technology demonstration.

Do not choose ledger posting, payment capture, period close, or another high-impact irreversible operation as the first slice.

## Decision

The read-only product/catalog journey was approved on 2026-09-23. Its high learning value exercises identity, routing, projection data, UI markers, reconciliation, and rollback without transferring write authority. The narrow six-field projection, read-only operation, and immediate gateway rollback account for the favorable complexity, irreversibility, and rollback scores. Team readiness remains conditional on the named operational assignments held in the governance register.
