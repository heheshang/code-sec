#!/bin/bash
# Seed 1M vuln + 500K snippet mock data into ES for benchmark
# Usage: ./seed-mock-data.sh [vuln_count] [snippet_count]
set -euo pipefail

ES_HOST="${ES_HOST:-localhost:9200}"
VULN_COUNT="${1:-1000000}"
SNIPPET_COUNT="${2:-500000}"
BATCH_SIZE=5000

echo "==> Seeding ES with ${VULN_COUNT} vulns + ${SNIPPET_COUNT} snippets"
echo "    ES: ${ES_HOST}"
echo ""

VULN_INDEX="codesec_vuln"
SNIPPET_INDEX="codesec_file_snippet"

# --- Seed vulns ---
seed_vulns() {
  echo "==> Seeding ${VULN_COUNT} vuln documents (batch: ${BATCH_SIZE})…"
  local total=$VULN_COUNT
  local done=0
  local start_time=$(date +%s)

  while [ $done -lt $total ]; do
    local batch_end=$((done + BATCH_SIZE))
    if [ $batch_end -gt $total ]; then batch_end=$total; fi

    # Generate bulk JSON
    local bulk_file=$(mktemp)
    for i in $(seq $((done + 1)) $batch_end); do
      local sev_idx=$((i % 5))
      local expl_idx=$((i % 3))
      local sevs=("critical" "high" "medium" "low" "info")
      local expls=("EXPLOITABLE" "POTENTIALLY_EXPLOITABLE" "NOT_EXPLOITABLE")
      local engs=("self_sast" "codeql" "sonar")
      local eng_idx=$((i % 3))

      cat >> "$bulk_file" <<EOF
{"index":{"_index":"${VULN_INDEX}","_id":"vuln-${i}"}}
{"id":"vuln-${i}","project_id":"proj-$((i % 100 + 1))","rule_id":"java/sql-injection-00$((i % 50 + 1))","severity":"${sevs[$sev_idx]}","exploitability":"${expls[$expl_idx]}","title":"SQL Injection in UserService.create$((i % 100))","description":"Potential SQL injection vulnerability found in line $((i % 500)). User input is concatenated into SQL query without parameterization.","code_snippet":"String query = \"SELECT * FROM users WHERE name = '\" + userName + \"'\";","file_path":"src/main/java/com/example/proj-$((i % 100 + 1))/UserService.java","cwe":"CWE-89","engine":"${engs[$eng_idx]}","discovered_at":"2026-06-$(printf '%02d' $((15 - i % 15)))T10:00:00","discovered_by":"scanner-self-sast"}
EOF
    done

    curl -s -X POST "${ES_HOST}/_bulk" \
      -H 'Content-Type: application/json' \
      --data-binary "@${bulk_file}" > /dev/null

    rm -f "$bulk_file"
    done=$batch_end

    if [ $((done % 50000)) -eq 0 ]; then
      local elapsed=$(($(date +%s) - start_time))
      local rate=$((done / (elapsed + 1)))
      echo "   Progress: ${done}/${total} (${rate}/s, ${elapsed}s elapsed)"
    fi
  done

  local elapsed=$(($(date +%s) - start_time))
  echo "✅ Vuln seeding complete: ${total} docs in ${elapsed}s"
}

# --- Seed snippets ---
seed_snippets() {
  echo "==> Seeding ${SNIPPET_COUNT} snippet documents…"
  local total=$SNIPPET_COUNT
  local langs=("java" "go" "python" "typescript" "javascript" "php" "csharp")

  local bulk_file=$(mktemp)
  for i in $(seq 1 $total); do
    local lang_idx=$((i % 7))
    cat >> "$bulk_file" <<EOF
{"index":{"_index":"${SNIPPET_INDEX}","_id":"snippet-${i}"}}
{"file_path":"src/main/${langs[$lang_idx]}/com/example/Service-${i}.${langs[$lang_idx]}","project_id":"proj-$((i % 100 + 1))","language":"${langs[$lang_idx]}","indexed_at":"2026-06-$(printf '%02d' $((15 - i % 15)))T10:00:00"}
EOF
  done

  curl -s -X POST "${ES_HOST}/_bulk" \
    -H 'Content-Type: application/json' \
    --data-binary "@${bulk_file}" > /dev/null
  rm -f "$bulk_file"

  echo "✅ Snippet seeding complete: ${total} docs"
}

# --- Main ---
echo "==> Checking ES health…"
curl -sf "${ES_HOST}/_cluster/health?wait_for_status=yellow&timeout=30s" > /dev/null || {
  echo "❌ ES not healthy"
  exit 1
}

echo "==> Refreshing indices…"
curl -s -X POST "${ES_HOST}/${VULN_INDEX}/_refresh" > /dev/null 2>&1 || true
curl -s -X POST "${ES_HOST}/${SNIPPET_INDEX}/_refresh" > /dev/null 2>&1 || true

seed_vulns
seed_snippets

echo ""
echo "==> Final refresh…"
curl -s -X POST "${ES_HOST}/${VULN_INDEX}/_refresh" > /dev/null
curl -s -X POST "${ES_HOST}/${SNIPPET_INDEX}/_refresh" > /dev/null

echo "==> Doc counts:"
echo -n "   vulns: "
curl -s "${ES_HOST}/${VULN_INDEX}/_count" | python3 -c "import sys,json; print(json.load(sys.stdin)['count'])" 2>/dev/null
echo -n "   snippets: "
curl -s "${ES_HOST}/${SNIPPET_INDEX}/_count" | python3 -c "import sys,json; print(json.load(sys.stdin)['count'])" 2>/dev/null

echo ""
echo "✅ Seed complete."
