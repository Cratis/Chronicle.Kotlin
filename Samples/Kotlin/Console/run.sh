#!/usr/bin/env bash
# Start Chronicle via docker compose and run the console sample.
#
# Usage:
#   ./run.sh [--database mongodb|postgresql|mssql|sqlite]
#
# Options:
#   --database  Storage backend for Chronicle (default: mongodb).
#               Starts the matching docker compose profile and sets CHRONICLE_SINK_TYPE.
#
# Environment variables (override any flag):
#   CHRONICLE_CONNECTION   Chronicle gRPC endpoint (default: localhost:35000).
#   CHRONICLE_SINK_TYPE    Override the sink type sent to Chronicle (MongoDB | SQL).
#
# Examples:
#   ./run.sh
#   ./run.sh --database postgresql
#   ./run.sh --database mssql

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DATABASE="mongodb"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --database) DATABASE="${2:?'--database requires a value'}"; shift 2 ;;
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

# ---------------------------------------------------------------------------
# Start Chronicle kernel
# ---------------------------------------------------------------------------
echo "▶  Starting Chronicle ($DATABASE) via docker compose..."
docker compose --profile "$COMPOSE_PROFILE" -f "$SCRIPT_DIR/docker-compose.yml" up -d

# Wait until Chronicle's health endpoint reports ready (port 8080).
# This is more reliable than waiting for the gRPC port alone because Chronicle
# accepts TCP connections on 35000 during startup but isn't ready for gRPC until
# migrations complete and the service is fully initialized.
echo "Waiting for Chronicle to be ready..."
for i in $(seq 1 60); do
    if curl -s http://localhost:8080/health 2>/dev/null | grep -q "Healthy"; then
        echo "Chronicle is ready."
        break
    fi
    if [ "$i" -eq 60 ]; then
        echo "Timed out waiting for Chronicle health check."
        docker compose --profile "$COMPOSE_PROFILE" -f "$SCRIPT_DIR/docker-compose.yml" logs
        docker compose --profile "$COMPOSE_PROFILE" -f "$SCRIPT_DIR/docker-compose.yml" down
        exit 1
    fi
    sleep 1
done

# ---------------------------------------------------------------------------
# Run the sample (Ctrl-C to exit)
# ---------------------------------------------------------------------------
cleanup() {
    echo ""
    echo "▶  Stopping Chronicle ($DATABASE)..."
    docker compose --profile "$COMPOSE_PROFILE" -f "$SCRIPT_DIR/docker-compose.yml" down
}
trap cleanup EXIT INT TERM

echo "▶  Running sample (database=$DATABASE, sinkType=$CHRONICLE_SINK_TYPE)"
echo ""

if command -v gradle &>/dev/null; then
    gradle --project-dir "$REPO_ROOT" :Samples:Kotlin:Console:run
else
    echo "Error: 'gradle' not found in PATH. Please install Gradle 9+ and ensure it is on your PATH."
    exit 1
fi
