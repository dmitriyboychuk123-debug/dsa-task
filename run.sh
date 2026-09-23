#!/bin/sh
# Build (cached when nothing changed) and start the stack; compose rebuilds images on every up via pull_policy: build.
set -eu
cd "$(dirname "$0")"

docker compose up -d
