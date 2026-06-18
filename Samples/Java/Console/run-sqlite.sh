#!/usr/bin/env bash
# Run the Chronicle Kotlin console sample with SQLite.
# Usage: ./run-sqlite.sh [--docker]
#   --docker  Start Chronicle + SQLite via docker compose before running.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/run-sample.sh" --database sqlite "$@"
