#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENGINE_JAR="$SCRIPT_DIR/target/code-sec-engine-1.0.0-SNAPSHOT-jar-with-dependencies.jar"

# Build if JAR doesn't exist
if [ ! -f "$ENGINE_JAR" ]; then
    echo "Building engine..." >&2
    cd "$SCRIPT_DIR" && mvn -q package -DskipTests
fi

if [ $# -eq 0 ]; then
    echo "Usage: ./run.sh <input-directory> [--output <json-file>] [--rules <rules-dir>]" >&2
    exit 1
fi

INPUT_DIR="$1"
shift

# Resolve input directory relative to script dir if not absolute
if [[ "$INPUT_DIR" != /* ]]; then
    INPUT_DIR="$SCRIPT_DIR/$INPUT_DIR"
fi

exec java -jar "$ENGINE_JAR" scan --input "$INPUT_DIR" "$@"
