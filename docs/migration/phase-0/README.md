# Phase 0: modernization program bootstrap

## Status

**Complete.** The charter, guardrails, target decisions, risk posture, ownership model, business targets, compliance scope, and budget authority were approved on 2026-09-22. Personal names and authoritative operational values are maintained in the organization's governance and operations systems rather than duplicated in this repository.

Last reviewed: 2026-09-22

## Charter

### Mission

Incrementally replace the OFBiz modular monolith with secure, independently deployable Azure services and route-owned modern UI slices, without interrupting retained business journeys. Use a strangler approach, Terraform-managed infrastructure, a monorepo, and a reproducible local developer runtime.

### Scope

In scope:

- application services, user interfaces, data ownership, integrations, scheduled work, identity integration, platform engineering, observability, security, delivery pipelines, and operational handover;
- temporary coexistence of OFBiz and modern components behind one controlled edge;
- removal of OFBiz and all migration scaffolding after verified cutover.

Out of scope unless separately approved:

- feature expansion unrelated to a migration slice;
- one-for-one conversion of every OFBiz package into a service;
- permanent database sharing between modern services;
- a big-bang UI rewrite or a cloud-only developer workflow.

### Program principles

1. A migration unit is a business vertical slice: data, service contracts, implementation, UI route, security, tests, operations, Terraform, rollout, and deletion work.
2. The modern service is not authoritative until reconciliation and rollback evidence satisfy its agreed gates.
3. The UI migrates concurrently with services and visibly identifies Modern, Mixed, and Legacy routes.
4. Every transitional adapter, route, replica, feature flag, and permission has an owner and expiry criterion.
5. Production infrastructure changes are reviewed Terraform changes; emergency changes must be reconciled immediately afterward.
6. Security, accessibility, observability, recovery, and cost are acceptance criteria, not later hardening phases.

## Decision rights and required appointments

Named assignments are maintained in the approved organizational governance register. One person may hold more than one role for a small program, but accountability remains explicit in that system of record.

| Role | Accountable for | Named owner | Approval evidence |
| --- | --- | --- | --- |
| Executive sponsor | Funding, priority, organizational impediments | Governance register | Written charter approval |
| Product owner | Journey scope, value measures, feature parity and retirement approval | Governance register | Approved journey catalog |
| Chief/solution architect | Target architecture, ADR arbitration and exceptions | Governance register | ADR approvals |
| Platform owner | Azure landing zone, Terraform, CI/CD and developer platform | Governance register | Platform SLO and runbook approval |
| Security owner | Threat models, identity, compliance controls and risk acceptance | Governance register | Security plan approval |
| Data owner | Data classification, retention, migration and reconciliation policy | Governance register | Data-ownership policy approval |
| Operations owner | Production SLOs, incident response, recovery and on-call readiness | Governance register | Operational readiness approval |
| UX/accessibility owner | Design system, route markers, accessibility and journey consistency | Governance register | UI migration standard approval |
| Domain owners | Bounded-context behavior, contracts and cutover | Governance register by context | Context ownership catalog |

## Phase 0 deliverables

| Deliverable | Artifact | Status |
| --- | --- | --- |
| Program charter and scope | This document | Approved |
| Current technical baseline | [technical-baseline.md](technical-baseline.md) | Established from Sonar and repository evidence |
| Business/SLO baseline framework | [measurement-baseline.md](measurement-baseline.md) | Approved; authoritative values are maintained operationally |
| Risk register | [risk-register.md](risk-register.md) | Approved; ownership is maintained in the governance register |
| Architecture decisions | [decision-register.md](decision-register.md) and `docs/adr/` | ADRs 0001–0009 accepted |
| Security and engineering guardrails | [guardrails.md](guardrails.md) | Approved |
| Intended architecture | Sonar model `4987d1a6-81fe-4f94-86a2-75d56491b7a2` | Created and API read-back verified |
| Cost/tagging convention | [guardrails.md](guardrails.md#cost-and-resource-governance) | Approved |
| Pilot selection method | [pilot-scorecard.md](pilot-scorecard.md) | Ready for Phase 1 evidence |

## Phase 0 exit criteria

Phase 0 is complete only when all boxes are checked:

- [x] Every role in the decision-rights table has a named, consenting owner in the governance register.
- [x] Sponsor and product owner approved charter, scope, success measures, and funding boundary.
- [x] Security and data owners approved compliance scope, classifications, retention, residency, and audit obligations.
- [x] Operations and product owners approved measurable SLOs and critical-journey baselines.
- [x] Platform owner approved environment, cost, tagging, Terraform-state, and CI identity strategy.
- [x] ADRs 0001–0009 were accepted.
- [x] Intended-architecture enforcement and initial triage responsibilities were accepted.
- [x] No unresolved decision blocks landing-zone or golden-path implementation.

## Phase 1 handoff

1. Begin the context, service/ECA, entity, job, route, and critical-journey discovery.
2. Populate the machine-readable data-ownership and route-ownership catalogs.
3. Score and select the pilot from measured Phase 1 evidence.
4. Add repository references to approved operational dashboards and governance records where access policy permits.
