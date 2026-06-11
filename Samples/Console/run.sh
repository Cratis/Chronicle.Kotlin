#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ---------------------------------------------------------------------------
# Start Chronicle kernel
# ---------------------------------------------------------------------------
echo "Starting Chronicle kernel..."
docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d

# Wait until the gRPC port is ready
echo "Waiting for Chronicle to be ready on port 35000..."
for i in $(seq 1 30); do
    if nc -z localhost 35000 2>/dev/null; then
        echo "Chronicle is ready."
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "Timed out waiting for Chronicle on port 35000."
        docker compose -f "$SCRIPT_DIR/docker-compose.yml" logs chronicle
        docker compose -f "$SCRIPT_DIR/docker-compose.yml" down
        exit 1
    fi
    sleep 1
done

# ---------------------------------------------------------------------------
# Run the sample (Ctrl-C to exit)
# ---------------------------------------------------------------------------
cleanup() {
    echo ""
    echo "Stopping Chronicle kernel..."
    docker compose -f "$SCRIPT_DIR/docker-compose.yml" down
}
trap cleanup EXIT INT TERM

if command -v gradle &>/dev/null; then
    gradle --project-dir "$REPO_ROOT" :Samples:Console:run
else
    echo "Error: 'gradle' not found in PATH. Please install Gradle 9+ and ensure it is on your PATH."
    exit 1
fi
