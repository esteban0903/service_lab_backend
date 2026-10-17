#!/usr/bin/env bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"

format_json() {
  if command -v jq >/dev/null 2>&1; then
    jq .
  elif command -v python3 >/dev/null 2>&1; then
    python3 -m json.tool
  elif command -v python >/dev/null 2>&1; then
    python -m json.tool
  else
    cat
  fi
}

echo "================================================================="
echo "               SERVICE LAB - DEMO SHOWCASE SCRIPT                "
echo "================================================================="
echo "Target Base URL: $BASE_URL"
echo ""

# 1. Seed demo data
echo "-----------------------------------------------------------------"
echo "STEP 1: Seed Demo Data"
echo "-----------------------------------------------------------------"
echo "Request: POST $BASE_URL/api/seed"
SEED_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/seed" -H "Content-Type: application/json")
SEED_BODY=$(echo "$SEED_RESPONSE" | sed -e '$d')
SEED_STATUS=$(echo "$SEED_RESPONSE" | tail -n 1 | cut -d: -f2)

echo "HTTP Status: $SEED_STATUS"
echo "Response:"
echo "$SEED_BODY" | format_json
echo ""

# Fetch tasks and workers to retrieve IDs dynamically
TASKS_JSON=$(curl -s "$BASE_URL/api/tasks")
WORKERS_JSON=$(curl -s "$BASE_URL/api/workers")

# Extract IDs using python
TASK_ID=$(python -c "import json, sys; tasks = json.loads(sys.stdin.read()); print(next(t['id'] for t in tasks if t['name'] == 'Customer Portal Frontend'))" <<< "$TASKS_JSON" 2>/dev/null || echo "8")
DIEGO_ID=$(python -c "import json, sys; workers = json.loads(sys.stdin.read()); print(next(w['id'] for w in workers if w['name'] == 'Diego'))" <<< "$WORKERS_JSON" 2>/dev/null || echo "4")
BRUNO_ID=$(python -c "import json, sys; workers = json.loads(sys.stdin.read()); print(next(w['id'] for w in workers if w['name'] == 'Bruno'))" <<< "$WORKERS_JSON" 2>/dev/null || echo "2")

# 2. Rejected manual assignment
echo "-----------------------------------------------------------------"
echo "STEP 2: Attempt Invalid Manual Assignment (Missing Skills -> 422)"
echo "-----------------------------------------------------------------"
echo "Attempting to assign 'Customer Portal Frontend' (requires [react, javascript])"
echo "to worker 'Diego' (skills: [python, sql, docker])..."
echo "Request: POST $BASE_URL/api/assignments"
REJECT_BODY="{\"taskId\": $TASK_ID, \"workerId\": $DIEGO_ID, \"startDate\": \"2026-10-26\"}"
echo "Payload: $REJECT_BODY"

REJECT_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/assignments" \
  -H "Content-Type: application/json" \
  -d "$REJECT_BODY")
REJECT_RESP_BODY=$(echo "$REJECT_RESPONSE" | sed -e '$d')
REJECT_STATUS=$(echo "$REJECT_RESPONSE" | tail -n 1 | cut -d: -f2)

echo "HTTP Status: $REJECT_STATUS (Expected: 422 Unprocessable Entity)"
echo "Response:"
echo "$REJECT_RESP_BODY" | format_json
echo ""

# 3. Valid manual assignment
echo "-----------------------------------------------------------------"
echo "STEP 3: Perform Valid Manual Assignment (Matching Skills -> 201)"
echo "-----------------------------------------------------------------"
echo "Assigning 'Customer Portal Frontend' to worker 'Bruno' (skills: [react, javascript, css])..."
echo "Request: POST $BASE_URL/api/assignments"
VALID_BODY="{\"taskId\": $TASK_ID, \"workerId\": $BRUNO_ID, \"startDate\": \"2026-10-26\"}"
echo "Payload: $VALID_BODY"

VALID_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/assignments" \
  -H "Content-Type: application/json" \
  -d "$VALID_BODY")
VALID_RESP_BODY=$(echo "$VALID_RESPONSE" | sed -e '$d')
VALID_STATUS=$(echo "$VALID_RESPONSE" | tail -n 1 | cut -d: -f2)

echo "HTTP Status: $VALID_STATUS (Expected: 201 Created)"
echo "Response:"
echo "$VALID_RESP_BODY" | format_json
echo ""

# 4. Auto-assignment
echo "-----------------------------------------------------------------"
echo "STEP 4: Run Auto-Assignment Algorithm"
echo "-----------------------------------------------------------------"
echo "Request: POST $BASE_URL/api/assignments/auto"
AUTO_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/api/assignments/auto" \
  -H "Content-Type: application/json" \
  -d '{"startDate": "2026-10-26"}')
AUTO_RESP_BODY=$(echo "$AUTO_RESPONSE" | sed -e '$d')
AUTO_STATUS=$(echo "$AUTO_RESPONSE" | tail -n 1 | cut -d: -f2)

echo "HTTP Status: $AUTO_STATUS (Expected: 200 OK)"
echo "Response:"
echo "$AUTO_RESP_BODY" | format_json
echo ""
echo "================================================================="
echo "                     DEMO SHOWCASE FINISHED                      "
echo "================================================================="
