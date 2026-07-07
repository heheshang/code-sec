# Sprint 4 — M1 Finalization Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Make M1 demo-ready with `docker compose up --build` one-click start and a reliable E2E flow.

**Current State:** docker-compose.yml exists but references Dockerfiles that don't exist. ES is in a separate compose file. No init scripts or smoke tests.

**Tech Stack:** Docker Compose, Spring Boot 3 (Maven), Vue 3 (Vite), MySQL 8, ES 8, Nginx

---

### Task 1: Backend API Dockerfile

**Files:**
- Create: `backend/api/Dockerfile`

**Step 1: Create the Dockerfile**

Multi-stage build:
- Stage 1 (build): Maven + JDK 17, build the full backend (all modules), extract layers
- Stage 2 (run): JDK 17 JRE slim, copy layers for optimal caching

```dockerfile
# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/common/ backend/common/
COPY backend/api/pom.xml backend/api/pom.xml
COPY backend/engine-adapter/ backend/engine-adapter/
# Note: es-integration module was replaced by PostgreSQL FTS (tsvector/tsquery)
# COPY backend/es-integration/ backend/es-integration/
COPY backend/gitlab-integration/ backend/gitlab-integration/
COPY backend/worker/ backend/worker/

# Download dependencies (cache layer)
RUN mvn dependency:go-offline -pl backend/api -am -B

# Copy sources and build
COPY backend/engine/ backend/engine/
COPY backend/engine/src/main/resources/rules/ backend/engine/src/main/resources/rules/
RUN mvn package -pl backend/api -am -B -DskipTests

# Stage 2 — Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/backend/api/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2: Verify**

Run: `docker build -t codesec-api:test -f backend/api/Dockerfile .`
Expected: Build succeeds, image created

**Step 3: Commit**

```bash
git add backend/api/Dockerfile
git commit -m "E-S4-DOCKER: add backend API Dockerfile"
```

---

### Task 2: Worker Dockerfile

**Files:**
- Create: `backend/worker/Dockerfile`

**Step 1: Create the Dockerfile**

Similar multi-stage build, but builds the worker module:

```dockerfile
# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/common/ backend/common/
COPY backend/worker/ backend/worker/
COPY backend/engine-adapter/ backend/engine-adapter/
RUN mvn dependency:go-offline -pl backend/worker -am -B
COPY backend/engine/ backend/engine/
RUN mvn package -pl backend/worker -am -B -DskipTests

# Stage 2 — Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/backend/worker/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 2: Verify**

Run: `docker build -t codesec-worker:test -f backend/worker/Dockerfile .`
Expected: Build succeeds

**Step 3: Commit**

```bash
git add backend/worker/Dockerfile
git commit -m "E-S4-DOCKER: add worker Dockerfile"
```

---

### Task 3: Frontend Dockerfile

**Files:**
- Create: `frontend/Dockerfile`
- Create: `frontend/nginx.conf`

**Step 1: Create Nginx config**

```nginx
server {
    listen 5173;
    server_name localhost;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend-api:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**Step 2: Create Frontend Dockerfile**

```dockerfile
# Stage 1 — Build
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2 — Serve via Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 5173
CMD ["nginx", "-g", "daemon off;"]
```

**Step 3: Verify**

Run: `docker build -t codesec-frontend:test -f frontend/Dockerfile .`
Expected: Build succeeds

**Step 4: Commit**

```bash
git add frontend/Dockerfile frontend/nginx.conf
git commit -m "E-S4-DOCKER: add frontend Dockerfile with Nginx"
```

---

### Task 4: Merge and Update docker-compose.yml

**Files:**
- Modify: `docker-compose.yml`
- Create: `.env.example`

**Step 1: Write .env.example**

```
# Database
MYSQL_ROOT_PASSWORD=root123
MYSQL_DATABASE=codesec

# JWT
JWT_SECRET=codesec-demo-secret-key-2026

# ES
# ES_HOST=elasticsearch:9200  deprecated — replaced by PG FTS

# GitLab (optional, for demo)
GITLAB_URL=
GITLAB_TOKEN=
```

**Step 2: Rewrite docker-compose.yml**

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-codesec}
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      timeout: 3s
      retries: 10
    restart: unless-stopped
  # elasticsearch:  # deprecated — replaced by PG FTS
  #   image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms1g -Xmx2g"
      - cluster.name=codesec-cluster
      - bootstrap.memory_lock=true
    ulimits:
      memlock: { soft: -1, hard: -1 }
      nofile: { soft: 65536, hard: 65536 }
    ports:
      - "9200:9200"
    volumes:
      # - es-data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q 'green\\|yellow'"]
      interval: 10s
      timeout: 5s
      retries: 30
      start_period: 60s
    restart: unless-stopped

  backend-api:
    build:
      context: .
      dockerfile: backend/api/Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE:-codesec}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123}
      JWT_SECRET: ${JWT_SECRET:-codesec-demo-secret-key-2026}
      # ES_HOST: elasticsearch:9200  deprecated — replaced by PG FTS
      SPRING_PROFILES_ACTIVE: test
    depends_on:
      mysql: { condition: service_healthy }
      # elasticsearch: { condition: service_healthy }  deprecated

  backend-worker:
    build:
      context: .
      dockerfile: backend/worker/Dockerfile
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE:-codesec}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123}
      JWT_SECRET: ${JWT_SECRET:-codesec-demo-secret-key-2026}
      # ES_HOST: elasticsearch:9200  deprecated — replaced by PG FTS
    depends_on:
      mysql: { condition: service_healthy }

  frontend:
    build:
      context: .
      dockerfile: frontend/Dockerfile
    ports:
      - "5173:5173"
    depends_on:
      - backend-api

volumes:
  mysql-data:
  es-data:
```

**Step 3: Commit**

```bash
git add docker-compose.yml .env.example
git commit -m "E-S4-DOCKER: merge ES into compose, add .env, health checks"
```

---

### Task 5: E2E Smoke Test Script

**Files:**
- Create: `scripts/e2e-smoke.sh`
- Create: `scripts/run-demo.sh`

**Step 1: Create e2e-smoke.sh**

A bash script that:
1. Waits for all services to be healthy
2. Logs in as admin → gets JWT token
3. Creates a repo
4. Triggers a scan
5. Checks scan status
6. Lists findings
7. Creates/views tickets
8. Exports PDF
9. Checks dashboard stats

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0

check() {
  local desc="$1"
  shift
  if "$@"; then
    echo "✅ $desc"
    PASS=$((PASS+1))
  else
    echo "❌ $desc"
    FAIL=$((FAIL+1))
  fi
}

wait_for() {
  local url="$1" label="$2" max=30
  for i in $(seq 1 $max); do
    if curl -sf "$url" > /dev/null 2>&1; then
      echo "  $label ready (attempt $i)"
      return 0
    fi
    sleep 2
  done
  echo "  $label NOT ready after ${max}s"
  return 1
}

echo "=== E2E Smoke Test ==="

# 0. Wait for services
wait_for "$BASE_URL/actuator/health" "Backend API"

# 1. Login
echo "--- 1. Login ---"
TOKEN=$(curl -sf -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')
check "Login returns token" test -n "$TOKEN"

# 2. Create repo
echo "--- 2. Create Repo ---"
REPO_ID=$(curl -sf -X POST "$BASE_URL/api/v1/repos" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"demo-repo","gitlabUrl":"https://gitlab.example.com/demo","language":"java"}' | jq -r '.id')
check "Repo created: id=$REPO_ID" test "$REPO_ID" -gt 0 2>/dev/null

# 3. Trigger scan
echo "--- 3. Trigger Scan ---"
SCAN_ID=$(curl -sf -X POST "$BASE_URL/api/v1/scans" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"repoId\":$REPO_ID,\"branch\":\"main\"}" | jq -r '.id')
check "Scan triggered: id=$SCAN_ID" test "$SCAN_ID" -gt 0 2>/dev/null

# 4. Check scan status
echo "--- 4. Scan Status ---"
sleep 2
STATUS=$(curl -sf "$BASE_URL/api/v1/scans/$SCAN_ID" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.status')
check "Scan has status" test -n "$STATUS"

# 5. List dashboard stats
echo "--- 5. Dashboard ---"
DASH=$(curl -sf "$BASE_URL/api/v1/dashboard/stats" \
  -H "Authorization: Bearer $TOKEN")
check "Dashboard returns data" echo "$DASH" | jq -e '.totalVulns | type == "number"' > /dev/null

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
exit $FAIL
```

**Step 2: Create run-demo.sh**

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "=== Starting CodeSec Demo Stack ==="
docker compose up --build -d

echo "Waiting for services..."
sleep 30

echo "Running smoke test..."
bash scripts/e2e-smoke.sh http://localhost:8080

echo ""
echo "Frontend: http://localhost:5173"
echo "API:      http://localhost:8080"
echo "Login:    admin / admin123"
```

**Step 3: Make scripts executable**

```bash
chmod +x scripts/e2e-smoke.sh scripts/run-demo.sh
```

**Step 4: Commit**

```bash
git add scripts/e2e-smoke.sh scripts/run-demo.sh
git commit -m "E-S4-DEMO: add E2E smoke test and demo start script"
```

---

### Task 6: ForkJoinPool Optimization

**Files:**
- Modify: `backend/engine/src/main/java/com/codesec/engine/judge/ExploitabilityJudger.java`

**Change:**
- Replace `Executors.newFixedThreadPool(4)` with `ForkJoinPool` for better work-stealing performance
- Set parallelism to `Runtime.getRuntime().availableProcessors() - 1`

Find line 96 and 127 — the two `Executors.newFixedThreadPool(4)` calls.

```diff
- import java.util.concurrent.ExecutorService;
- import java.util.concurrent.Executors;
+ import java.util.concurrent.ForkJoinPool;

- this.executor = Executors.newFixedThreadPool(4, r -> {
-     Thread t = new Thread(r);
-     t.setDaemon(true);
-     return t;
- });
+ this.executor = new ForkJoinPool(
+     Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
+ );
```

Need to keep the `executor` field type compatible. Change to `ExecutorService` or keep `ForkJoinPool` since it IS an `ExecutorService`.

Commit:

```bash
git add backend/engine/src/main/java/com/codesec/engine/judge/ExploitabilityJudger.java
git commit -m "E-S4-PERF: replace fixed thread pool with ForkJoinPool for work-stealing"
```

---

### Task 7: ES Performance Baseline Test (DEPRECATED — replaced by PG FTS)

> **Note**: ES integration was replaced by PostgreSQL tsvector/tsquery full-text search.
> The following tasks are retained for historical reference only.

**Files:**
- ~~Check: `backend/es-integration/src/test/java/com/codesec/search/indexer/EsIndexListenerTest.java`~~ — module removed
- ~~Create: `backend/es-integration/src/test/java/com/codesec/search/performance/EsSearchPerformanceTest.java`~~ — module removed

---

### Task 8: Demo Script

**Files:**
- Create: `docs/demo-script.md`

Outline the full 15-minute demo walkthrough covering:

1. **Start** (1 min) — `docker compose up --build`
2. **Login** (1 min) — admin/admin123 → Dashboard
3. **Dashboard** (2 min) — Stats, trend chart, severity distribution
4. **Vulnerabilities** (2 min) — Audit queue, filter by severity, exploitability
5. **Workbench** (3 min) — Code viewer, audit actions, PDF export
6. **Search** (2 min) — Full-text search across findings
7. **Rules** (2 min) — Rule list, enable/disable, sync from engine, exemptions
8. **Wrap-up** (2 min) — Key differentiators (exploitability-aware, E2E workflow)

Commit:

```bash
git add docs/demo-script.md
git commit -m "E-S4-DEMO: add demo walkthrough script"
```

---

## Execution Order

```
Task 1 (API Dockerfile)       → depends on nothing
Task 2 (Worker Dockerfile)    → depends on nothing (parallel with Task 1)
Task 3 (Frontend Dockerfile)  → depends on nothing (parallel with Task 1)
Task 4 (compose.yml)          → depends on Task 1,2,3
Task 5 (Smoke tests)          → depends on Task 4 (can be parallel with 6/7)
Task 6 (ForkJoinPool)         → independent
Task 7 (ES perf test)         → independent
Task 8 (Demo script)          → independent
```

Parallel groups:
- Group A (parallel): Task 1, 2, 3, 6, 7, 8
- Group B (after A): Task 4
- Group C (after B): Task 5
