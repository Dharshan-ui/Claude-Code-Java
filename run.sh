#!/bin/sh
set -e

cd "$(dirname "$0")"
mvn -q -B package -Ddir=/tmp/claude-agent-build

exec java --enable-preview -jar /tmp/claude-agent-build/claude-agent.jar "$@"