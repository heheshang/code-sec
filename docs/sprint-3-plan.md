# Sprint 3 — Execution Plan

> Generated: 2026-07-01 | Status: ✅ Confirmed for Execution
> Associated: `docs/project-plan.md`, `.harness/changes/sprint-3/contract.md`

## Problem Statement

How might we take the backend skeleton built in Sprint 2 and deliver real-data features (dashboard, whitelist, PDF export), multi-language engine expansion (Go/Python), and production hardening (KMS, performance, tech debt) — all while keeping M1 end-to-end demo on track?

## Recommended Direction

**Execute the original Sprint 3 plan (7 Epics, 370h) in P0→P1→P2 priority order.** CodeQL License has arrived but will be used in Sprint 5 per original roadmap — no scope changes needed.

The 7 Epics split into three phases within the sprint:
- **Phase A (Day 1-3):** Parallel P0s — KMS (independent), DASHBOARD (depends on S2 backend), TECH (independent)
- **Phase B (Day 3-6):** P1s — RULE, PDF (both depend on backend API being stable)
- **Phase C (Day 5-8):** P2s — MULTI (tree-sitter JNI risk, check Day 5), PERF
- **Phase D (Day 8-10):** Integration testing + Bug Bash + Demo prep

## Key Assumptions to Validate

- [ ] **Backend API stability**: Frontend mock→real switch depends on Sprint 2 backend being production-ready. Validate Day 3.
- [ ] **tree-sitter JNI**: Most technical risk in Sprint 3. If not working by Day 5, defer MULTI to Sprint 4.
- [ ] **ES code quality**: 24 untested files from Sprint 2 — if tests reveal architecture issues, may need Sprint 4 rework.
- [ ] **PERF 40s→30s**: 25% improvement may need more than 40h. Day 8 checkpoint; defer to M1.5 if behind.

## MVP Scope

| Priority | Epic | Hours | Deliverable |
|----------|------|-------|-------------|
| **P0** | E-S3-KMS | 40h | Aliyun KMS for GitLab token + config encryption, AES fallback |
| **P0** | E-S3-DASHBOARD | 40h | Real dashboard stats API + DashboardView off mock |
| **P0** | E-S3-TECH | 40h | ES unit tests (≥20), common/adapter/worker tests (≥10), ES perf P99<500ms |
| **P1** | E-S3-RULE | 60h | Rule CRUD API + whitelist management UI + engine integration |
| **P1** | E-S3-PDF | 50h | PDF generation engine + export API + frontend download |
| **P2** | E-S3-MULTI | 100h | tree-sitter runtime + Go/Python AST adapter + basic detectors |
| **P2** | E-S3-PERF | 40h | Engine hotspot analysis + Caffeine cache + ForkJoinPool optimization |

## Not Doing (and Why)

- **CodeQL integration** — License confirmed but full integration deferred to Sprint 5/M2 (architecturally cleaner to do post-MULTI)
- **RabbitMQ real queue** — Sprint 4 (BlockingQueue sufficient for M1)
- **Real OAuth2 SSO** — Sprint 4 (JWT continues to work)
- **Multi-language PHP/JS/TS** — M2 (tree-sitter then scales to these)
- **K8s sandbox cluster** — M2 (Docker Compose sufficient for M1)

## Open Questions

- CodeQL License: confirmed "available" — what's the actual provision timeline? Need to lock before Sprint 5.
- Performance QG-6: is 40s→30s measured on which benchmark hardware? Should establish baseline first.
