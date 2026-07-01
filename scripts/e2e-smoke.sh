#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0

check() {
  local desc="$1"
  shift
  if "$@"; then
    echo "  ✅ $desc"
    PASS=$((PASS+1))
  else
    echo "  ❌ $desc"
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

echo "=== CodeSec E2E Smoke Test ==="
echo ""

# 0. Wait for services
echo "--- 0. Service Health ---"
wait_for "$BASE_URL/actuator/health" "Backend API"

# 1. Login
echo "--- 1. Login ---"
RESP=$(curl -sf -X POST "$BASE_URL/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' 2>&1) || {
  check "Login returns token" false
  echo "  Response: $RESP"
  TOKEN=""
}
TOKEN=$(echo "$RESP" | jq -r '.token // empty')
check "Login returns token" test -n "$TOKEN"

if [ -z "$TOKEN" ]; then
  echo ""
  echo "=== Results: $PASS passed, $FAIL failed ==="
  echo "Aborting: no token (login may use different credentials)"
  exit $FAIL
fi

# 2. Dashboard stats
echo "--- 2. Dashboard ---"
DASH=$(curl -sf "$BASE_URL/api/v1/dashboard/stats" \
  -H "Authorization: Bearer $TOKEN" 2>&1) || {
  check "Dashboard stats" false
  DASH="{}"
}
TOTAL=$(echo "$DASH" | jq -r '.totalVulns // empty')
check "Dashboard returns vulnerability count" test -n "$TOTAL"
echo "  Total vulnerabilities: ${TOTAL:-N/A}"

# 3. List repositories
echo "--- 3. Repositories ---"
REPOS=$(curl -sf "$BASE_URL/api/v1/repos" \
  -H "Authorization: Bearer $TOKEN" 2>&1) || {
  check "List repositories" false
  REPOS="[]"
}
REPO_COUNT=$(echo "$REPOS" | jq -r 'length // 0')
check "Repositories accessible" test "$REPO_COUNT" -ge 0
echo "  Repository count: $REPO_COUNT"

# 4. List vulnerabilities
echo "--- 4. Vulnerabilities ---"
VULNS=$(curl -sf "$BASE_URL/api/v1/vulnerabilities?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" 2>&1) || {
  check "List vulnerabilities" false
  VULNS="{}"
}
VULN_COUNT=$(echo "$VULNS" | jq -r '.content | length // 0')
check "Vulnerabilities accessible" test "$VULN_COUNT" -ge 0
echo "  Vulnerabilities shown: $VULN_COUNT"

# 5. Audit log
echo "--- 5. Audit Log ---"
AUDIT=$(curl -sf "$BASE_URL/api/v1/audit-logs?page=0&size=5" \
  -H "Authorization: Bearer $TOKEN" 2>&1) || {
  check "Audit log" false
  AUDIT="{}"
}
AUDIT_COUNT=$(echo "$AUDIT" | jq -r '.content | length // 0')
check "Audit log accessible" test "$AUDIT_COUNT" -ge 0
echo "  Audit entries: $AUDIT_COUNT"

# 6. Rules
echo "--- 6. Rules ---"
RULES=$(curl -sf "$BASE_URL/api/v1/rules" \
  -H "Authorization: Bearer $TOKEN" 2>&1) || {
  check "List rules" false
  RULES="[]"
}
RULE_COUNT=$(echo "$RULES" | jq -r 'length // 0')
check "Rules accessible" test "$RULE_COUNT" -ge 0
echo "  Rules count: $RULE_COUNT"

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
exit $FAIL
