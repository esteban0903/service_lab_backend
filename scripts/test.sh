#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR/backend"

echo "Running backend test suite..."
mvn clean test
echo "All tests passed."

