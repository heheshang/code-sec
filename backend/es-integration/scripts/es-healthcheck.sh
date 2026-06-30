#!/bin/bash
# ES Cluster health check
set -euo pipefail

ES_HOST="${ES_HOST:-localhost:9200}"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "==> Checking ES cluster health at ${ES_HOST}..."

for i in $(seq 1 ${MAX_RETRIES}); do
  if curl -sf "${ES_HOST}/_cluster/health" > /dev/null 2>&1; then
    HEALTH=$(curl -s "${ES_HOST}/_cluster/health" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    echo "✅ ES cluster is ${HEALTH} (attempt ${i}/${MAX_RETRIES})"
    curl -s "${ES_HOST}/_cluster/health?pretty"
    echo ""
    
    # Check plugins
    echo "==> Installed plugins:"
    curl -s "${ES_HOST}/_cat/plugins?v" || echo "(plugins endpoint unavailable)"
    
    exit 0
  fi
  echo "Waiting for ES... (attempt ${i}/${MAX_RETRIES})"
  sleep ${RETRY_INTERVAL}
done

echo "❌ ES cluster did not become healthy within $((MAX_RETRIES * RETRY_INTERVAL))s"
exit 1
