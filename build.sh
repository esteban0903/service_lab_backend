#!/usr/bin/env bash
set -e

echo "=== Building Task Assignment Backend ==="
mvn clean package -DskipTests
echo "=== Backend build completed successfully ==="
