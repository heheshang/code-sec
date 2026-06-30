#!/bin/bash
# Snapshot-window load test — repeats 1000 QPS 60s during ES snapshot window (02:00-03:00)
# Verifies P99 increment < 100ms compared to non-snapshot baseline.
# Prerequisite: ES snapshot cron registered at 02:00 daily.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULT_DIR="${SCRIPT_DIR}/../benchmark-results"
mkdir -p "${RESULT_DIR}"

echo "==> Snapshot-window load test"
echo "    Started: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo ""

# Trigger snapshot
echo "==> Triggering snapshot…"
"${SCRIPT_DIR}/es-snapshot.sh" create "snap_benchmark_$(date +%Y%m%d_%H%M%S)"

# Wait 5s for snapshot to start
sleep 5

# Run load test during snapshot
echo "==> Running load test during snapshot…"
"${SCRIPT_DIR}/load-test-search.sh" 1000 60

echo ""
echo "==> Snapshot-window load test complete."
echo "    Compare P99 with non-snapshot baseline — delta should be < 100ms."
