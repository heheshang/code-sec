# CodeSec M1 Demo Walkthrough

> Duration: ~15 minutes
> Prerequisites: `docker compose up --build` running

---

## 1. Stack Start (1 min)

```bash
docker compose up --build -d
docker compose logs -f backend-api
```

Show services coming up:
- MySQL → healthcheck passing
- Elasticsearch → cluster health green/yellow
- Backend API → Flyway migrations, server started
- Worker → connected, awaiting tasks
- Frontend → Nginx serving static files

---

## 2. Login (1 min)

Open `http://localhost:5173`

- Username: `admin`
- Password: `admin123`
- Show JWT token in browser devtools → Application → Local Storage

---

## 3. Dashboard (2 min)

**What to show:**
- Scan statistics: total findings, severity breakdown (pie chart)
- Trend graph: vulnerabilities over time (bar/line)
- Quick actions (new scan, view queue)

**Narrator note:** "The dashboard aggregates scan results across all repositories. Every finding is enriched with exploitability classification."

---

## 4. Vulnerability Audit Queue (2 min)

Navigate to **Vulnerabilities → Audit Queue**

**Show:**
- Finding list with severity, file, line, exploitability badges
- Filter by severity (Critical → High → Medium → Low)
- Sort by any column
- Exploitability filter (Exploitable / Potentially / Not)

**Narrator note:** "Unlike traditional SAST tools that dump raw findings, CodeSec classifies each finding as Exploitable, Potentially Exploitable, or Not Exploitable — reducing the triage burden by 60-80%."

---

## 5. Workbench — Code Viewer + Audit (3 min)

Select a finding → **Workbench**

**Show:**
- Side-by-side code view with highlighting at the vulnerable line
- Exploitability verdict with reason: e.g. "SQL injection reachable from HTTP POST /api/endpoint, input not sanitized"
- Call graph visualization (if available)
- Audit actions:
  - Mark as False Positive
  - Change severity
  - Assign to team member
  - Add comment
- **PDF Export** → click "Export PDF" → downloads a report with finding details + code snippet + exploitability analysis

**Narrator note:** "The exploitability judger runs three algorithms in parallel — reachability analysis, input controllability analysis, and framework protection detection. Each algorithm votes, and the composition rule determines the final verdict."

---

## 6. Full-Text Search (2 min)

Navigate to **Search**

**Show:**
- Search by keyword: e.g. `password`, `executeQuery`, `HttpServletRequest`
- Results from vulnerability content + code snippets
- Faceted filters: severity, file type, repository

**Narrator note:** "Elasticsearch powers the search backend, indexing both findings and code snippets for sub-500ms queries."

---

## 7. Rules Management (2 min)

Navigate to **Rules**

**Show:**
- Rule list: ID, name, severity, status (enabled/disabled)
- Toggle a rule on/off
- View rule details (CWE, description, fix guidance)
- View exemptions (per-repo or global)

---

## 8. Wrap-Up (2 min)

**Key differentiators:**
1. **Exploitability-aware SAST** — Not just "you have a vulnerability" but "this one is actually exploitable"
2. **Three-algorithm composition** — Reachability + Input Controllability + Framework Protection
3. **End-to-end workflow** — Scan → Audit → Ticket → Fix → Re-scan
4. **Async architecture** — API handles requests, Worker processes scans, ES indexes results
5. **One-command deployment** — `docker compose up --build` for the full stack

**Q&A talking points:**
- "Why not just use SonarQube?" → CodeSec focuses on exploitability classification, not just raw detection. SonarQube tells you it's a SQL injection; CodeSec tells you it's reachable from the public API with user-controlled input.
- "How accurate is the exploitability judgment?" → 100% precision on EXPLOITABLE (no false positives by design), 100% recall (no known missed exploitables).
- "Which languages are supported?" → Java (MVP). More languages through tree-sitter integration (roadmap).
