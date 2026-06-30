#!/bin/bash
# ES Snapshot cron entry — registers daily snapshot at 02:00
# Add to crontab: 0 2 * * * /path/to/es-snapshot-cron.sh >> /var/log/es-snapshot.log 2>&1
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="/tmp/es-snapshot-cron-$(date +%Y%m%d).log"

echo "=== ES Snapshot Cron: $(date -u +"%Y-%m-%dT%H:%M:%SZ") ===" | tee -a "${LOG_FILE}"

"${SCRIPT_DIR}/es-snapshot.sh" create "snap_$(date +%Y%m%d_%H%M%S)" 2>&1 | tee -a "${LOG_FILE}"

EXIT_CODE=$?
if [ ${EXIT_CODE} -eq 0 ]; then
  echo "✅ Daily snapshot completed successfully" | tee -a "${LOG_FILE}"
else
  echo "❌ Daily snapshot FAILED (exit code: ${EXIT_CODE})" | tee -a "${LOG_FILE}"
fi

exit ${EXIT_CODE}
