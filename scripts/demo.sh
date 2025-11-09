#!/usr/bin/env bash
set -euo pipefail

mvn -q -DskipTests package
JAR=target/queuectl-1.0.0.jar

rm -f queuectl.db queuectl.log || true
rm -rf logs || true

java -jar "$JAR" enqueue "echo hello from job 1"
java -jar "$JAR" enqueue "sh -c 'echo will fail; exit 1'" --id bad1 --max-retries 2 --backoff-base 2
java -jar "$JAR" enqueue "sleep 2" --priority 5

( java -jar "$JAR" worker start --count 2 ) &
WPID=$!
sleep 10
java -jar "$JAR" worker stop
sleep 2 || true

java -jar "$JAR" status
java -jar "$JAR" dlq list
java -jar "$JAR" list
