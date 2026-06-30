#!/bin/bash
# Monitor ES JVM heap during load test.
# Samples heap usage every 10s and reports peak.
# Prerequisite: ES running on localhost:9200.
set -euo pipefail

ES_HOST="${ES_HOST:-localhost:9200}"
DURATION="${1:-120}"
INTERVAL=10
HEAP_LIMIT_MB=4096  # 4GB

RESULT_DIR="$(dirname "$0")/../benchmark-results"
mkdir -p "${RESULT_DIR}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${RESULT_DIR}/heap-monitor-${TIMESTAMP}.csv"

echo "timestamp,heap_used_bytes,heap_used_mb,heap_max_bytes,heap_max_mb" > "${LOG_FILE}"

peak_mb=0
peak_ts=""

echo "==> Monitoring ES heap for ${DURATION}s (check every ${INTERVAL}s)…"
echo "    Heap limit: ${HEAP_LIMIT_MB}MB (4GB)"
echo ""

START=$(date +%s)
while [ $(($(date +%s) - START)) -lt $DURATION ]; do
  TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

  STATS=$(curl -s "${ES_HOST}/_nodes/stats/jvm" 2>/dev/null || echo '{}')
  HEAP_USED=$(echo "${STATS}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
nodes=d.get('nodes',{})
for n in nodes.values():
    heap=n.get('jvm',{}).get('mem',{}).get('heap_used_in_bytes',0)
    print(heap)
    break
" 2>/dev/null || echo "0")

  HEAP_MAX=$(echo "${STATS}" | python3 -c "
import sys,json
d=json.load(sys.stdin)
nodes=d.get('nodes',{})
for n in nodes.values():
    heap=n.get('jvm',{}).get('mem',{}).get('heap_max_in_bytes',0)
    print(heap)
    break
" 2>/dev/null || echo "0")

  HEAP_MB=$((HEAP_USED / 1048576))
  HEAP_MAX_MB=$((HEAP_MAX / 1048576))

  echo "${TS},${HEAP_USED},${HEAP_MB},${HEAP_MAX},${HEAP_MAX_MB}" >> "${LOG_FILE}"

  if [ $HEAP_MB -gt $peak_mb ]; then
    peak_mb=$HEAP_MB
    peak_ts=$TS
  fi

  printf "   [%s] heap: %4d MB / %4d MB (peak: %4d MB)%s\n" \
    "$TS" "$HEAP_MB" "$HEAP_MAX_MB" "$peak_mb" \
    "$([ $HEAP_MB -ge $HEAP_LIMIT_MB ] && echo ' ⚠️ OVER LIMIT' || echo '')"

  sleep $INTERVAL
done

echo ""
echo "============================================"
echo "HEAP MONITOR SUMMARY"
echo "============================================"
echo "Peak heap used: ${peak_mb} MB at ${peak_ts}"
echo "Heap limit:     ${HEAP_LIMIT_MB} MB"

if [ $peak_mb -lt $HEAP_LIMIT_MB ]; then
  echo "✅ Heap < 4GB — PASS"
else
  echo "❌ Heap >= 4GB — BLOCKED (T2 mapping may need adjustment)"
fi

echo ""
echo "Detailed log: ${LOG_FILE}"
