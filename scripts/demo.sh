#!/usr/bin/env bash
set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"

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
SEED_BODY=$(curl -s -X POST "$BASE_URL/api/seed" -H "Content-Type: application/json")
echo "Response:"
echo "$SEED_BODY"
echo ""

# Extract demoStartDate from seed response using standard POSIX shell
DEMO_START_DATE=$(echo "$SEED_BODY" | grep -o '"demoStartDate":"[^"]*"' | cut -d'"' -f4)
if [ -z "$DEMO_START_DATE" ]; then
  DEMO_START_DATE="2026-10-19"
fi
echo "Resolved Demo Start Date from Seed: $DEMO_START_DATE"
echo ""

# Fetch tasks and workers to retrieve IDs dynamically by name (no jq or python required)
TASKS_JSON=$(curl -s "$BASE_URL/api/tasks")
WORKERS_JSON=$(curl -s "$BASE_URL/api/workers")

TASK_ID=$(echo "$TASKS_JSON" | tr '}' '\n' | grep '"name":"Customer Portal Frontend"' | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -n 1)
DIEGO_ID=$(echo "$WORKERS_JSON" | tr '}' '\n' | grep '"name":"Diego"' | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -n 1)
BRUNO_ID=$(echo "$WORKERS_JSON" | tr '}' '\n' | grep '"name":"Bruno"' | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -n 1)

echo "Resolved IDs by Name:"
echo "  - Task 'Customer Portal Frontend': ID=$TASK_ID"
echo "  - Worker 'Diego': ID=$DIEGO_ID"
echo "  - Worker 'Bruno': ID=$BRUNO_ID"
echo ""

# 2. Rejected manual assignment
echo "-----------------------------------------------------------------"
echo "STEP 2: Attempt Invalid Manual Assignment (Missing Skills -> 422)"
echo "-----------------------------------------------------------------"
echo "Attempting to assign 'Customer Portal Frontend' (requires [react, javascript])"
echo "to worker 'Diego' (skills: [python, sql, docker])..."
echo "Request: POST $BASE_URL/api/assignments"
REJECT_BODY="{\"taskId\": $TASK_ID, \"workerId\": $DIEGO_ID, \"startDate\": \"$DEMO_START_DATE\"}"
echo "Payload: $REJECT_BODY"
echo ""
echo "HTTP Response:"
curl -s -i -X POST "$BASE_URL/api/assignments" \
  -H "Content-Type: application/json" \
  -d "$REJECT_BODY"
echo ""
echo ""

# 3. Valid manual assignment
echo "-----------------------------------------------------------------"
echo "STEP 3: Perform Valid Manual Assignment (Matching Skills -> 201)"
echo "-----------------------------------------------------------------"
echo "Assigning 'Customer Portal Frontend' to worker 'Bruno' (skills: [react, javascript, css])..."
echo "Request: POST $BASE_URL/api/assignments"
VALID_BODY="{\"taskId\": $TASK_ID, \"workerId\": $BRUNO_ID, \"startDate\": \"$DEMO_START_DATE\"}"
echo "Payload: $VALID_BODY"
echo ""
echo "HTTP Response:"
curl -s -i -X POST "$BASE_URL/api/assignments" \
  -H "Content-Type: application/json" \
  -d "$VALID_BODY"
echo ""
echo ""

# 4. Auto-assignment
echo "-----------------------------------------------------------------"
echo "STEP 4: Run Auto-Assignment Algorithm"
echo "-----------------------------------------------------------------"
echo "Request: POST $BASE_URL/api/assignments/auto"
AUTO_BODY="{\"startDate\": \"$DEMO_START_DATE\"}"
echo "Payload: $AUTO_BODY"
echo ""
echo "HTTP Response:"
curl -s -i -X POST "$BASE_URL/api/assignments/auto" \
  -H "Content-Type: application/json" \
  -d "$AUTO_BODY"
echo ""
echo ""

echo "================================================================="
echo "                     DEMO SHOWCASE FINISHED                      "
echo "================================================================="
