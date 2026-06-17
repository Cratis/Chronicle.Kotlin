#!/usr/bin/env bash
# Run the Chronicle Kotlin console sample with PostgreSQL.
# Usage: ./run-postgresql.sh [--docker]
#   --docker  Start Chronicle + PostgreSQL via docker compose before running.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/run-sample.sh" --database postgresql "$@"
