#!/usr/bin/env bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "=========================================="
echo "Seeding demo data via POST $BASE_URL/api/seed"
echo "=========================================="
curl -s -X POST "$BASE_URL/api/seed" -H "Content-Type: application/json"
echo ""

