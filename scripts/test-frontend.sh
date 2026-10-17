#!/bin/sh
set -e
cd "$(dirname "$0")/../frontend"

if [ ! -d "node_modules" ]; then
  echo "Installing frontend dependencies..."
  npm install
fi

echo "Running frontend test suite..."
npm test
echo "All frontend tests passed."

