#!/usr/bin/env bash
set -e

# Resolve repository root
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR/backend"

echo "Building backend application..."
mvn clean package -DskipTests
echo "Build successful."

