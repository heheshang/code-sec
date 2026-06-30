#!/bin/bash
# Download and install IK Analyzer plugin for Elasticsearch 8.13.0
set -euo pipefail

ES_VERSION="8.13.0"
IK_VERSION="${ES_VERSION}"
PLUGIN_DIR="$(dirname "$0")/../docker/plugins/ik"

echo "==> Installing IK Analyzer plugin v${IK_VERSION} for ES ${ES_VERSION}"

mkdir -p "${PLUGIN_DIR}"

IK_ZIP="elasticsearch-analysis-ik-${IK_VERSION}.zip"
IK_URL="https://github.com/infinilabs/analysis-ik/releases/download/v${IK_VERSION}/${IK_ZIP}"

TMP_DIR=$(mktemp -d)
trap 'rm -rf ${TMP_DIR}' EXIT

echo "==> Downloading ${IK_URL}..."
curl -fsSL -o "${TMP_DIR}/${IK_ZIP}" "${IK_URL}" || {
  echo "WARN: GitHub download failed. Trying alternative URL..."
  curl -fsSL -o "${TMP_DIR}/${IK_ZIP}" \
    "https://release.infinilabs.com/analysis-ik/stable/${IK_ZIP}" || {
    echo "ERROR: Failed to download IK plugin. Please download manually from:"
    echo "  https://github.com/infinilabs/analysis-ik/releases/tag/v${IK_VERSION}"
    exit 1
  }
}

echo "==> Extracting IK plugin..."
unzip -qo "${TMP_DIR}/${IK_ZIP}" -d "${PLUGIN_DIR}"

# Verify plugin structure
if [ -f "${PLUGIN_DIR}/plugin-descriptor.properties" ]; then
  echo "✅ IK Analyzer plugin installed successfully at ${PLUGIN_DIR}"
  cat "${PLUGIN_DIR}/plugin-descriptor.properties" | head -5
else
  # Some zip files nest the content in a subdirectory
  NESTED_DIR=$(find "${PLUGIN_DIR}" -name "plugin-descriptor.properties" -maxdepth 2 | head -1)
  if [ -n "${NESTED_DIR}" ]; then
    NESTED_PARENT=$(dirname "${NESTED_DIR}")
    echo "==> Flattening nested plugin directory from ${NESTED_PARENT}"
    cp -r "${NESTED_PARENT}"/* "${PLUGIN_DIR}/"
    rm -rf "${NESTED_PARENT}"
    echo "✅ IK Analyzer plugin installed successfully (flattened)"
  else
    echo "ERROR: plugin-descriptor.properties not found"
    exit 1
  fi
fi

echo ""
echo "==> Next: Start ES with 'docker compose -f docker/docker-compose.yml up -d'"
echo "==> Verify: curl -s http://localhost:9200/_cat/plugins | grep analysis-ik"
