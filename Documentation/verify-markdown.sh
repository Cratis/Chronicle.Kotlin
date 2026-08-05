#!/bin/bash

# Markdown Verification Script
# This script runs the same markdown linting and link verification that runs in CI

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "=========================================="
echo "Markdown Verification"
echo "=========================================="
echo ""

# Check if running from repository root or Documentation folder
if [ "$(basename "$PWD")" = "Documentation" ]; then
    cd ..
fi

echo "Working directory: $PWD"
echo ""

# Step 1: Markdown Linting
echo "=========================================="
echo "Step 1: Running markdownlint..."
echo "=========================================="
echo ""

if ! command -v npx &> /dev/null; then
    echo "Error: npx is not installed. Please install Node.js and npm."
    exit 1
fi

LINT_EXIT_CODE=0
npx markdownlint-cli2 "Documentation/**/*.md" || LINT_EXIT_CODE=$?

echo ""
if [ $LINT_EXIT_CODE -eq 0 ]; then
    echo "✓ Markdown linting passed!"
else
    echo "✗ Markdown linting failed with exit code $LINT_EXIT_CODE"
fi
echo ""

# Step 2: Link Verification
echo "=========================================="
echo "Step 2: Running link verification..."
echo "=========================================="
echo ""
echo "This may take a few minutes to check all links..."
echo ""

# linkinator serves the local files over http://localhost:<port>/, so a blanket
# "skip localhost" pattern silently skips the entire crawl and reports success
# after scanning zero links. Skip only what genuinely cannot resolve locally:
#   /chronicle/    cross-product links into the aggregated docs site
#   .../<folder>/  directory links the site resolves to that folder's index page
LINK_EXIT_CODE=0
LINK_OUTPUT=$(npx linkinator "Documentation/**/*.md" --markdown --recurse --verbosity error --status-code "403:ok" --skip "/chronicle/ ^https?://[^/]+/Documentation/[A-Za-z0-9._-]+/$" 2>&1) || LINK_EXIT_CODE=$?
echo "$LINK_OUTPUT"

# linkinator exits 0 when it scans nothing at all, which is indistinguishable
# from "everything passed" — guard against the checker silently going no-op.
LINK_COUNT=$(echo "$LINK_OUTPUT" | grep -oiE "scanned [0-9]+ links" | grep -oE "[0-9]+" | head -1)
if [ -z "$LINK_COUNT" ] || [ "$LINK_COUNT" -eq 0 ]; then
    echo "✗ Link verification scanned 0 links — the checker itself is broken."
    LINK_EXIT_CODE=1
fi

echo ""
if [ $LINK_EXIT_CODE -eq 0 ]; then
    echo "✓ Link verification passed!"
else
    echo "✗ Link verification failed with exit code $LINK_EXIT_CODE"
fi
echo ""

# Final summary
echo "=========================================="
echo "Summary"
echo "=========================================="
if [ $LINT_EXIT_CODE -eq 0 ] && [ $LINK_EXIT_CODE -eq 0 ]; then
    echo "✓ All checks passed!"
    exit 0
else
    echo "✗ Some checks failed:"
    [ $LINT_EXIT_CODE -ne 0 ] && echo "  - Markdown linting"
    [ $LINK_EXIT_CODE -ne 0 ] && echo "  - Link verification"
    exit 1
fi
