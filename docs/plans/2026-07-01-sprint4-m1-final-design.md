# Sprint 4 — M1 Finalization Plan

> Generated: 2026-07-01 | Status: ✅ Approved
> Sprint 3 completed: 7 epics delivered (KMS, DASHBOARD, TECH, RULE, PDF, MULTI, PERF)

## Goal

Make M1 **demo-ready**: a single command starts the entire stack, and the end-to-end flow
(GitLab MR → Scan → Tickets → Audit → PDF) works reliably on a demo machine.

## Scope

### P0 — Docker Compose One-Click Start

| Task | Description |
|------|-------------|
| `docker-compose.yml` | Service definitions: MySQL 8, ES 8, backend API, worker, frontend (Nginx) |
| Init scripts | Flyway auto-migration, ES index creation, seed data |
| Health checks | Wait-for-it dependency ordering, health endpoints |
| `.env` template | Default config (JWT_SECRET, DB, ES host) |
| Single start command | `docker compose up --build` |

### P1 — E2E Integration + Bug Bash

| Task | Description |
|------|-------------|
| Full-chain smoke test | GitLab webhook → scan → finding → ticket → audit → PDF → dashboard (automated script) |
| Known bug fixes | Issues uncovered during E2E testing |
| Frontend polish | Loading states, error handling, null-safety edge cases |

### P2 — Quality Pass

| Task | Description |
|------|-------------|
| ES perf baseline | P99 query latency benchmark, index tuning |
| ForkJoinPool migration | Replace `Executors.newFixedThreadPool` with `ForkJoinPool` in engine judger |
| ES unit test gap | Verify ≥30 existing tests cover the codebase |
| M1 checklist audit | Verify QG-1~8 pass status |

### P3 — Demo Preparation

| Task | Description |
|------|-------------|
| Demo script | Step-by-step walkthrough covering all M1 features |
| 3 dry runs | Rehearse with full stack, fix issues found |
| Demo backup | Recorded video fallback |

## Out of Scope (deferred to M2)

- SCA / Dependency-Check
- K8s sandbox cluster
- OAuth2 SSO
- RabbitMQ real queue (BlockingQueue sufficient)
- Multi-language PHP/JS/TS

## Architecture (Docker Compose)

```
┌──────────────────────────────────────────────────┐
│                Docker Compose                      │
│                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  MySQL 8  │  │   ES 8   │  │  Frontend        │ │
│  │  :3306    │  │  :9200   │  │  Nginx → Vue SPA │ │
│  └────┬─────┘  └────┬─────┘  └────────┬─────────┘ │
│       │             │                  │           │
│  ┌────▼─────────────▼──────────────────▼─────────┐ │
│  │          Backend API (Spring Boot)             │ │
│  │          :8080                                 │ │
│  └────────────────────┬──────────────────────────┘ │
│                       │                            │
│  ┌────────────────────▼──────────────────────────┐ │
│  │          Worker (Spring Boot)                  │ │
│  └───────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────┘
```

## Success Criteria

- [ ] `docker compose up --build` starts all 5 services
- [ ] Health endpoint `GET /api/v1/health` returns 200
- [ ] Frontend accessible at `http://localhost:3000`
- [ ] E2E smoke script passes (webhook → scan → ticket → audit → PDF)
- [ ] Demo script walkthrough ≤ 15 minutes
- [ ] 3 dry runs without critical failure
