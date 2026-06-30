#!/bin/bash
# ES Snapshot restore — disaster recovery drill
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> ES Restore from snapshot"
echo "⚠️  DANGER: This will overwrite existing indices!"
echo ""

# List available snapshots
"${SCRIPT_DIR}/es-snapshot.sh" list

echo ""
echo "Enter snapshot name to restore (or Ctrl-C to abort):"
read -r SNAP_NAME

"${SCRIPT_DIR}/es-snapshot.sh" restore "${SNAP_NAME}"
