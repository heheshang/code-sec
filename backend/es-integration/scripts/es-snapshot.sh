#!/bin/bash
# ES Snapshot management script
# Usage: ./es-snapshot.sh [create|list|restore|delete] [snapshot_name]
set -euo pipefail

ES_HOST="${ES_HOST:-localhost:9200}"
REPO_NAME="local_backup"
ACTION="${1:-create}"
SNAPSHOT_NAME="${2:-snap_$(date +%Y%m%d_%H%M%S)}"

echo "==> ES Snapshot tool — action: ${ACTION}"

create_repo() {
  echo "==> Creating snapshot repository '${REPO_NAME}'..."
  curl -s -X PUT "${ES_HOST}/_snapshot/${REPO_NAME}" -H 'Content-Type: application/json' -d '{
    "type": "fs",
    "settings": {
      "location": "/usr/share/elasticsearch/backup",
      "compress": true,
      "max_snapshot_bytes_per_sec": "40mb",
      "max_restore_bytes_per_sec": "40mb"
    }
  }' | python3 -m json.tool 2>/dev/null || cat
  echo ""
}

create_snapshot() {
  echo "==> Creating snapshot '${SNAPSHOT_NAME}'..."
  RESPONSE=$(curl -s -X PUT "${ES_HOST}/_snapshot/${REPO_NAME}/${SNAPSHOT_NAME}?wait_for_completion=false" \
    -H 'Content-Type: application/json' -d '{
    "indices": "*",
    "ignore_unavailable": true,
    "include_global_state": true
  }')
  echo "${RESPONSE}" | python3 -m json.tool 2>/dev/null || echo "${RESPONSE}"
  
  # Wait for completion
  echo "==> Waiting for snapshot to complete..."
  for i in $(seq 1 180); do
    STATUS=$(curl -s "${ES_HOST}/_snapshot/${REPO_NAME}/${SNAPSHOT_NAME}" | \
      python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('snapshots',[{}])[0].get('state','PENDING'))" 2>/dev/null || echo "PENDING")
    echo "   Snapshot status: ${STATUS} (${i}/180)"
    if [ "${STATUS}" = "SUCCESS" ]; then
      echo "✅ Snapshot '${SNAPSHOT_NAME}' completed successfully"
      return 0
    elif [ "${STATUS}" = "FAILED" ] || [ "${STATUS}" = "PARTIAL" ]; then
      echo "❌ Snapshot failed with status: ${STATUS}"
      curl -s "${ES_HOST}/_snapshot/${REPO_NAME}/${SNAPSHOT_NAME}" | python3 -m json.tool 2>/dev/null || cat
      return 1
    fi
    sleep 10
  done
  echo "⚠️  Snapshot timed out after 30 minutes"
  return 1
}

list_snapshots() {
  echo "==> Listing snapshots in '${REPO_NAME}'..."
  curl -s "${ES_HOST}/_snapshot/${REPO_NAME}/_all?pretty"
}

restore_snapshot() {
  echo "==> Restoring snapshot '${SNAPSHOT_NAME}'..."
  echo "⚠️  This will close indices before restore. Continue? (y/n)"
  read -r CONFIRM
  if [ "${CONFIRM}" != "y" ]; then
    echo "Aborted."
    exit 0
  fi
  curl -s -X POST "${ES_HOST}/_snapshot/${REPO_NAME}/${SNAPSHOT_NAME}/_restore?wait_for_completion=true" \
    -H 'Content-Type: application/json' -d '{
    "indices": "*",
    "ignore_unavailable": true,
    "include_global_state": false
  }' | python3 -m json.tool 2>/dev/null || cat
}

delete_snapshot() {
  echo "==> Deleting snapshot '${SNAPSHOT_NAME}'..."
  curl -s -X DELETE "${ES_HOST}/_snapshot/${REPO_NAME}/${SNAPSHOT_NAME}" | python3 -m json.tool 2>/dev/null || cat
}

# Main
create_repo

case "${ACTION}" in
  create) create_snapshot ;;
  list)   list_snapshots ;;
  restore) restore_snapshot ;;
  delete) delete_snapshot ;;
  *)      echo "Unknown action: ${ACTION}. Use create|list|restore|delete" ; exit 1 ;;
esac
