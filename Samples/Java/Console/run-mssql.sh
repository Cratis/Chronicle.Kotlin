#!/usr/bin/env bash
# Run the Chronicle Kotlin console sample with Microsoft SQL Server.
# Usage: ./run-mssql.sh [--docker]
#   --docker  Start Chronicle + SQL Server via docker compose before running.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/run-sample.sh" --database mssql "$@"
