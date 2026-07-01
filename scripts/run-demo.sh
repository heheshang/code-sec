#!/usr/bin/env bash
set -euo pipefail

echo "╔═══════════════════════════════════════╗"
echo "║   CodeSec M1 Demo — Starting Stack   ║"
echo "╚═══════════════════════════════════════╝"
echo ""

# Ensure we're in the project root
cd "$(dirname "$0")/.."

echo "→ Building and starting all services..."
docker compose up --build -d

echo ""
echo "→ Waiting for services to be healthy..."
echo "  (MySQL + ES take ~60s to initialize)"

# Wait up to 120s for the API
MAX=60
for i in $(seq 1 $MAX); do
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "  ✅ Backend API ready (attempt $i)"
    break
  fi
  if [ "$i" -eq "$MAX" ]; then
    echo "  ❌ Backend API not ready after ${MAX}s"
    echo "  Run 'docker compose logs backend-api' to debug"
  fi
  sleep 2
done

echo ""
echo "→ Running smoke test..."
if bash scripts/e2e-smoke.sh; then
  echo ""
  echo "╔═══════════════════════════════════════╗"
  echo "║   ✅ Demo Stack Ready!                ║"
  echo "╠═══════════════════════════════════════╣"
  echo "║  Frontend: http://localhost:5173      ║"
  echo "║  API:      http://localhost:8080      ║"
  echo "║  ES:       http://localhost:9200      ║"
  echo "║  Login:    admin / admin123           ║"
  echo "╚═══════════════════════════════════════╝"
else
  echo ""
  echo "⚠️  Smoke test failed — some endpoints may not work as expected."
  echo "   Check 'docker compose logs' for details."
fi
