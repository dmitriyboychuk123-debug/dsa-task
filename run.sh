#!/bin/sh
# Start the stack. Images are built only if missing; use buildAndRun.sh to rebuild after code changes.
set -eu
cd "$(dirname "$0")"

docker compose up -d
