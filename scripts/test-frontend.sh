#!/bin/sh
set -e
cd "$(dirname "$0")/../frontend"

echo "Running frontend test suite..."
npm test
echo "All frontend tests passed."
