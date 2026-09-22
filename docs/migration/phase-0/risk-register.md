# Phase 0 risk register

Scale: probability and impact are `Low`, `Medium`, `High`, or `Critical`. Named owners and review dates are mandatory before Phase 0 closes.

| ID | Risk | Probability | Impact | Leading indicator | Mitigation / control | Owner | Review |
| --- | --- | --- | --- | --- | --- | --- | --- |
| R-001 | Hidden XML, ECA, job, or shared-table coupling breaks an extracted flow | High | Critical | Unexplained side effects or reconciliation drift | Phase 1 metadata inventory, runtime tracing, characterization tests, staged cutover | Unassigned | Before each wave |
| R-002 | Services share schemas or internal libraries and become a distributed monolith | High | High | Cross-service SQL/imports or synchronized releases | Database grants, intended architecture, contract tests, independent deployment gate | Unassigned | Monthly |
| R-003 | Dual writes lose or duplicate business changes | High | Critical | Reconciliation mismatches, orphan records | Transactional outbox, idempotency, CDC monitoring, no uncoordinated dual writes | Unassigned | Each cutover |
| R-004 | Legacy and modern authorization semantics diverge | Medium | Critical | Permission discrepancies or elevated access | Entra design, explicit mapping, fail-closed bridge, negative authorization tests | Unassigned | Each identity change |
| R-005 | UI migration lags backend extraction | High | High | Modern APIs used only through legacy pages | UI route is mandatory slice scope; Modern/Mixed marker and route catalog gates | Unassigned | Sprint review |
| R-006 | The visible implementation marker is spoofed or leaks topology | Low | Medium | Marker controlled by request input or exposing hosts | Trusted runtime manifest, CSP/encoding, allowlisted metadata, security tests | Unassigned | UI release |
| R-007 | Developer workflow requires Azure and slows delivery | Medium | High | Long feedback loops or shared-dev contention | Compose/Testcontainers, deterministic seeds, host hot reload, parity tests | Unassigned | Monthly |
| R-008 | Terraform state, drift, or emergency changes undermine reproducibility | Medium | Critical | Console-created resources or stale plans | Protected remote state, OIDC, plan review, drift checks, break-glass reconciliation | Unassigned | Every apply |
| R-009 | Service count exceeds team operational capacity | Medium | High | Unowned alerts/services or copy-paste platforms | Context justification, platform golden path, ownership/SLO entry gate | Unassigned | Architecture review |
| R-010 | Migration adapters and flags become permanent | High | High | Expired routes/flags with traffic | Owner, expiry, deletion criterion and dashboard for every transitional asset | Unassigned | Monthly |
| R-011 | Accounting or payment consistency is weakened by distributed workflows | Medium | Critical | Unbalanced postings, duplicate capture, manual fixes | Delay high-risk extraction; explicit sagas, invariants, idempotency, audit and reconciliation | Unassigned | Before financial waves |
| R-012 | Cost grows faster than migration value | Medium | High | Budget alerts or idle duplicated environments | Tags, budgets, scale-to-zero outside production, cost-per-transaction reporting | Unassigned | Monthly |
| R-013 | Production telemetry contains secrets or regulated data | Medium | Critical | Sensitive fields found in logs/traces/events | Classification, allowlist logging, redaction tests, retention/access policy | Unassigned | Security review |
| R-014 | Sonar intended architecture is stored but not enforced in analysis | Medium | High | Context view remains empty or violations never appear | Verify next analysis, capture UI/API evidence, triage and fail CI for new violations | Unassigned | Phase 0 exit |
| R-015 | Required skills or owners are unavailable | Medium | High | Decision backlog and unsupported services | Capacity plan, training, explicit ownership before wave entry | Unassigned | Program review |

## Risk acceptance

Only the role accountable for the affected business/security domain may accept a risk. Acceptance records the rationale, residual exposure, expiry date, monitoring, and contingency. “Migration deadline” alone is not sufficient rationale for accepting a Critical risk.
