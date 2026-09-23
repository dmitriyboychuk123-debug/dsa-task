#!/bin/sh
# Build the project and the Docker images, then start the stack.
set -eu
cd "$(dirname "$0")"

docker compose up --build -d
