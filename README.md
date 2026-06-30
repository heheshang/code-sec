# CodeSec — Code Security Audit Platform

CodeSec is an automated SAST (Static Application Security Testing) platform that scans source code for security vulnerabilities, judges exploitability via call-graph analysis, and integrates with GitLab for MR-level feedback.

## Architecture

```
┌─────────────────────────────────────────────────┐
│                    Frontend                      │
│            Vue 3 + TypeScript + Vite            │
└──────────────────────┬──────────────────────────┘
                       │ HTTP/REST
┌──────────────────────▼──────────────────────────┐
│                   Backend API                    │
│         Spring Boot 3 / Java 17                 │
│                                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ Security │ │ Repo Mgt │ │ Vuln Mgt         │ │
│  │ JWT/RBAC │ │ CRUD     │ │ Findings/Tickets │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ Scan Mgt │ │ Webhook  │ │ Audit Log        │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
└──────┬──────────────┬──────────────┬─────────────┘
       │              │              │
       ▼              ▼              ▼
┌──────────┐ ┌──────────────┐ ┌──────────────┐
│  Worker  │ │ Engine-      │ │ ES-          │
│  Queue   │ │ Adapter      │ │ Integration  │
│  Consumer│ │              │ │ Search/Index │
└────┬─────┘ └──────┬───────┘ └──────┬───────┘
     │              │                │
     ▼              ▼                ▼
┌──────────┐ ┌──────────────┐ ┌──────────────────┐
│  Engine  │ │ GitLab       │ │ Elasticsearch    │
│  SAST    │ │ Integration  │ │ (Vuln/Snippet)   │
│  Scan    │ │ MR Commenter │ │                  │
└──────────┘ └──────────────┘ └──────────────────┘
     │
     ▼
┌──────────┐ ┌──────────────┐ ┌──────────────────┐
│Detectors │ │ Call Graph   │ │ Exploitability   │
│SQL/ XSS/ │ │ Analysis     │ │ Judger           │
│Crypto/   │ │              │ │                  │
│Password  │ └──────────────┘ └──────────────────┘
└──────────┘
```

### Key Components

| Module | Description | Tech |
|--------|-------------|------|
| **`backend/api`** | REST API — auth, repo, scan, vuln, ticket, webhook, audit | Spring Boot 3 / JPA / Flyway |
| **`backend/engine`** | SAST scan engine — detectors, AST parser, call graph, exploitability | Java 17, JavaParser |
| **`backend/engine-adapter`** | Abstraction layer decoupling API from engine | Spring |
| **`backend/es-integration`** | Elasticsearch — vuln/snippet indexing, full-text search | Spring Data ES |
| **`backend/gitlab-integration`** | GitLab — webhook receiver, MR diff scan, comment reporter | GitLab REST API |
| **`backend/worker`** | Async scan queue consumer | Spring Boot |
| **`backend/common`** | Shared lib — crypto (AES-GCM, KMS), base types | Spring |
| **`frontend`** | Dashboard, scan management, vulnerability browser, search | Vue 3 / Pinia / TypeScript |

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0
- Elasticsearch 8.x (optional, for search features)
- Node.js 18+ (for frontend)

### Backend

```bash
# Start infrastructure
docker compose up -d mysql

# Build all modules
mvn clean install -f backend/pom.xml

# Run API server
mvn spring-boot:run -f backend/api/pom.xml

# Run worker (separate terminal)
mvn spring-boot:run -f backend/worker/pom.xml
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

### Full Stack (Docker)

```bash
docker compose up --build
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/codesec` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `root123` | DB password |
| `JWT_SECRET` | *(auto-generated)* | JWT signing key |
| `ES_HOST` | `localhost:9200` | Elasticsearch host |

## Scan Engine

The engine runs a multi-phase pipeline:

1. **Parse** — AST parsing with JavaParser
2. **Detect** — Rule-based detectors match vulnerability patterns
   - SQL Injection (MyBatis/JPA/Hibernate)
   - Cross-Site Scripting (XSS)
   - Weak Cryptography
   - Hardcoded Credentials
3. **Judge** — Call-graph reachability + exploitability analysis
   - Taint tracking from user input to vulnerable sinks
   - Framework protection detection (Spring Security, ESAPI)
   - Input controllability scoring
4. **Report** — Structured findings with severity, exploitability, and fix guidance

## Development

```bash
# Run all tests
mvn test -f backend/pom.xml

# Run specific module tests
mvn test -f backend/engine/pom.xml

# Lint frontend
cd frontend && npm run lint

# Type-check frontend
cd frontend && npm run type-check
```

## License

MIT
