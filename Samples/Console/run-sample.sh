#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ---------------------------------------------------------------------------
# Run the sample (Ctrl-C to exit)
# ---------------------------------------------------------------------------
if command -v gradle &>/dev/null; then
    gradle --project-dir "$REPO_ROOT" :Samples:Console:run
else
    echo "Error: 'gradle' not found in PATH. Please install Gradle 9+ and ensure it is on your PATH."
    exit 1
fi
