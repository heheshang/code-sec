#!/bin/bash
# migrate-kms.sh — Migrate existing AES-encrypted tokens to KMS
#
# This script invokes a Spring Boot CommandLineRunner (KmsMigrationRunner)
# that handles the actual decryption/re-encryption using the application's
# CryptoService beans.
#
# Usage:
#   ./scripts/migrate-kms.sh              # execute migration
#   ./scripts/migrate-kms.sh --dry-run     # preview only, no changes
#
# Prerequisites:
#   - ALIBABA_CLOUD_ACCESS_KEY_ID / ALIBABA_CLOUD_ACCESS_KEY_SECRET set
#   - codesec.crypto.provider=kms in application.yml
#   - KMS key-id configured and accessible
#   - API jar built: mvn -pl api package -DskipTests

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
API_JAR="${PROJECT_DIR}/backend/api/target/code-sec-backend-api-1.0.0-SNAPSHOT.jar"

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
    DRY_RUN=true
    echo "[DRY-RUN] Preview mode — no changes will be made"
fi

# Config — override via environment or keep defaults
DB_HOST="${CODESEC_DB_HOST:-localhost}"
DB_PORT="${CODESEC_DB_PORT:-3306}"
DB_USER="${CODESEC_DB_USER:-root}"
DB_PASS="${CODESEC_DB_PASS:-root123}"
DB_NAME="${CODESEC_DB_NAME:-codesec}"

echo "=== KMS Migration Script ==="
echo "Project: ${PROJECT_DIR}"
echo "Jar:     ${API_JAR}"
echo "DB:      ${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "Time:    $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo ""

# Step 1: Check jar exists
if [[ ! -f "${API_JAR}" ]]; then
    echo "[ERROR] API jar not found at ${API_JAR}"
    echo "Build it first: mvn -pl api package -DskipTests -Dspring-boot.repackage.skip=false"
    echo ""
    echo "Quick build: cd ${PROJECT_DIR}/backend && mvn -pl common,api package -DskipTests"
    exit 1
fi

# Step 2: Build Spring Boot command
SPRING_ARGS=(
    "--spring.profiles.active=kms-migrate"
    "--spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    "--spring.datasource.username=${DB_USER}"
    "--spring.datasource.password=${DB_PASS}"
    "--kms-migration.dry-run=${DRY_RUN}"
)

echo "--- Step 1: Running KMS migration via Spring Boot ---"
echo "Profile: kms-migrate"
echo "Dry-run: ${DRY_RUN}"
echo ""

java -jar "${API_JAR}" "${SPRING_ARGS[@]}"

EXIT_CODE=$?
if [[ ${EXIT_CODE} -eq 0 ]]; then
    echo ""
    echo "=== Migration ${DRY_RUN:+preview }completed successfully ==="
    if [[ "${DRY_RUN}" == "true" ]]; then
        echo "Run without --dry-run to execute."
    fi
else
    echo ""
    echo "=== Migration failed (exit code: ${EXIT_CODE}) ==="
    exit ${EXIT_CODE}
fi
