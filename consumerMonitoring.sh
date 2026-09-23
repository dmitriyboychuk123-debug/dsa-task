#!/bin/sh
# Follow the consumer's anomaly detection output (stack must be running, see run.sh). Ctrl+C to stop.
set -eu
cd "$(dirname "$0")"

docker compose logs -f consumer-service
