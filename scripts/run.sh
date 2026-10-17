#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR/backend"

echo "Starting backend in dev mode (H2 in-memory)..."
mvn spring-boot:run -Dspring-boot.run.profiles=dev

