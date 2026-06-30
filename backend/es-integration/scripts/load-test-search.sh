#!/bin/bash
# ES Search load test using Apache Bench (ab) — simulates concurrent search traffic.
# Prerequisite: ES seeded with 1M vulns (seed-mock-data.sh)
# Usage: ./load-test.sh [qps] [duration_sec]
set -euo pipefail

API_HOST="${API_HOST:-localhost:8080}"
QPS="${1:-1000}"
DURATION="${2:-60}"
CONCURRENCY=$((QPS / 10))  # 10 concurrent to sustain 1000 QPS

RESULT_DIR="$(dirname "$0")/../benchmark-results"
mkdir -p "${RESULT_DIR}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_FILE="${RESULT_DIR}/load-test-${TIMESTAMP}.txt"

QUERIES=(
  "/api/v1/search/vulns?q=sql%20injection&page=1&page_size=20"
  "/api/v1/search/vulns?q=SELECT%20FROM%20WHERE&severity=critical&page=1&page_size=20"
  "/api/v1/search/vulns?q=deserialization&exploitability=EXPLOITABLE&page=1&page_size=20"
  "/api/v1/search/vulns?q=xss&engine=self_sast&page=1&page_size=20"
  "/api/v1/search/vulns?q=CWE-89&page=1&page_size=20"
  "/api/v1/search/snippets?q=src/main&page=1&page_size=20"
  "/api/v1/search/vulns?q=injection&severity=high&sortBy=_score&sortOrder=desc"
  "/api/v1/search/vulns?q=user&page=1&page_size=100"
  "/api/v1/search/vulns?q=password&severity=critical,high&page=1&page_size=20"
  "/api/v1/search/vulns?q=buffer&page=1&page_size=20"
)

echo "============================================" | tee "${RESULT_FILE}"
echo "ES Search Load Test" | tee -a "${RESULT_FILE}"
echo "============================================" | tee -a "${RESULT_FILE}"
echo "API: ${API_HOST}" | tee -a "${RESULT_FILE}"
echo "Target QPS: ${QPS}" | tee -a "${RESULT_FILE}"
echo "Duration: ${DURATION}s" | tee -a "${RESULT_FILE}"
echo "Concurrency: ${CONCURRENCY}" | tee -a "${RESULT_FILE}"
echo "Started: $(date -u +"%Y-%m-%dT%H:%M:%SZ")" | tee -a "${RESULT_FILE}"
echo "" | tee -a "${RESULT_FILE}"

total_requests=0
total_failures=0
declare -a p50_arr
declare -a p95_arr
declare -a p99_arr

for i in $(seq 1 ${#QUERIES[@]}); do
  query="${QUERIES[$((i-1))]}"
  url="${API_HOST}${query}"
  echo "--- Query $i: ${query} ---" | tee -a "${RESULT_FILE}"

  # Use curl in a loop for QPS simulation (simpler than ab for custom URLs)
  local start=$(date +%s%3N)
  local reqs=0
  local fails=0
  local times=()
  local end_time=$(($(date +%s) + DURATION))

  while [ $(date +%s) -lt $end_time ]; do
    for j in $(seq 1 $CONCURRENCY); do
      local t0=$(date +%s%3N)
      if curl -sf -o /dev/null -w "%{http_code}" "${url}" 2>/dev/null | grep -q "200"; then
        local t1=$(date +%s%3N)
        times+=($((t1 - t0)))
      else
        fails=$((fails + 1))
      fi
      reqs=$((reqs + 1))
    done
    # Throttle to maintain target QPS
    local elapsed_ms=$(($(date +%s%3N) - start))
    local expected_ms=$(( (reqs * 1000) / QPS ))
    if [ $elapsed_ms -lt $expected_ms ]; then
      sleep 0.$(printf "%03d" $((expected_ms - elapsed_ms))) 2>/dev/null || true
    fi
  done

  total_requests=$((total_requests + reqs))
  total_failures=$((total_failures + fails))

  # Calculate percentiles
  if [ ${#times[@]} -gt 0 ]; then
    IFS=$'\n' sorted=($(printf '%s\n' "${times[@]}" | sort -n))
    local n=${#sorted[@]}
    local p50=${sorted[$((n * 50 / 100))]:-N/A}
    local p95=${sorted[$((n * 95 / 100))]:-N/A}
    local p99=${sorted[$((n * 99 / 100))]:-N/A}

    echo "   Requests: ${reqs}, Failures: ${fails}" | tee -a "${RESULT_FILE}"
    echo "   P50: ${p50}ms, P95: ${p95}ms, P99: ${p99}ms" | tee -a "${RESULT_FILE}"

    p50_arr+=(${p50})
    p95_arr+=(${p95})
    p99_arr+=(${p99})
  fi
  echo "" | tee -a "${RESULT_FILE}"
done

echo "============================================" | tee -a "${RESULT_FILE}"
echo "SUMMARY" | tee -a "${RESULT_FILE}"
echo "============================================" | tee -a "${RESULT_FILE}"
echo "Total requests: ${total_requests}" | tee -a "${RESULT_FILE}"
echo "Total failures: ${total_failures}" | tee -a "${RESULT_FILE}"
echo "Error rate: $(echo "scale=4; ${total_failures} / ${total_requests} * 100" | bc 2>/dev/null || echo "N/A")%" | tee -a "${RESULT_FILE}"
echo "" | tee -a "${RESULT_FILE}"

# Overall P99 (max of all per-query P99s)
echo "Overall P50 (max): $(printf '%s\n' "${p50_arr[@]}" | sort -n | tail -1)ms" | tee -a "${RESULT_FILE}"
echo "Overall P95 (max): $(printf '%s\n' "${p95_arr[@]}" | sort -n | tail -1)ms" | tee -a "${RESULT_FILE}"
echo "Overall P99 (max): $(printf '%s\n' "${p99_arr[@]}" | sort -n | tail -1)ms" | tee -a "${RESULT_FILE}"

P99_MAX=$(printf '%s\n' "${p99_arr[@]}" | sort -n | tail -1)
if [ -n "${P99_MAX}" ] && [ "${P99_MAX}" != "N/A" ] && [ "${P99_MAX}" -lt 500 ]; then
  echo "✅ P99 < 500ms — PASS" | tee -a "${RESULT_FILE}"
else
  echo "⚠️  P99 >= 500ms — QG-6 BLOCKED" | tee -a "${RESULT_FILE}"
fi

echo "" | tee -a "${RESULT_FILE}"
echo "Results saved to: ${RESULT_FILE}"
