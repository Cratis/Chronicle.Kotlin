#!/usr/bin/env bash
# Run the Chronicle Kotlin console sample.
#
# Usage:
#   ./run-sample.sh [--database mongodb|postgresql|mssql|sqlite] [--docker]
#
# Options:
#   --database  Storage backend for Chronicle read models (default: mongodb).
#               The corresponding Chronicle server must be running — use --docker
#               to start it automatically via docker compose.
#   --docker    Start Chronicle (and any required infrastructure) via docker compose
#               before running the sample. Stops the containers when the sample exits.
#
# Environment variables (override any flag):
#   CHRONICLE_CONNECTION   Chronicle gRPC endpoint (default: localhost:35000).
#   CHRONICLE_SINK_TYPE    Override the sink type sent to Chronicle (MongoDB | SQL).
#
# Examples:
#   ./run-sample.sh
#   ./run-sample.sh --database postgresql --docker
#   ./run-sample.sh --database mssql

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DATABASE="mongodb"
USE_DOCKER=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --database) DATABASE="${2:?'--database requires a value'}"; shift 2 ;;
        --docker)   USE_DOCKER=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

case "$DATABASE" in
    mongodb)    SINK_TYPE="MongoDB"; COMPOSE_PROFILE="mongodb" ;;
    postgresql) SINK_TYPE="SQL";     COMPOSE_PROFILE="postgresql" ;;
    mssql)      SINK_TYPE="SQL";     COMPOSE_PROFILE="mssql" ;;
    sqlite)     SINK_TYPE="SQL";     COMPOSE_PROFILE="sqlite" ;;
    *)
        echo "Unknown database: $DATABASE. Use: mongodb, postgresql, mssql, sqlite"
        exit 1
        ;;
esac

export CHRONICLE_SINK_TYPE="${CHRONICLE_SINK_TYPE:-$SINK_TYPE}"

if "$USE_DOCKER"; then
    echo "▶  Starting Chronicle ($DATABASE) via docker compose..."
    docker compose --profile "$COMPOSE_PROFILE" -f "$SCRIPT_DIR/docker-compose.yml" up -d
    echo "✓  Chronicle  → http://localhost:8080  (Workbench)"
    echo "✓  gRPC       → localhost:35000"
    echo ""
    trap 'echo ""; echo "▶  Stopping containers..."; docker compose --profile "$COMPOSE_PROFILE" -f "$SCRIPT_DIR/docker-compose.yml" down' EXIT
fi

echo "▶  Running sample (database=$DATABASE, sinkType=$CHRONICLE_SINK_TYPE)"

if command -v gradle &>/dev/null; then
    gradle --project-dir "$REPO_ROOT" :Samples:Kotlin:Console:run
else
    echo "Error: 'gradle' not found in PATH. Please install Gradle 9+ and ensure it is on your PATH."
    exit 1
fi
