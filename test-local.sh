#!/bin/sh
set -eu
cd "$(dirname "$0")"
classes=$(mktemp -d)
trap 'rm -r "$classes"' EXIT
mode=${1:-test}
set -- src/main/java/notificationagent/*.java src/test/java/notificationagent/*.java
if command -v javac >/dev/null 2>&1; then
    javac --release 8 -encoding UTF-8 -d "$classes" "$@"
else
    java -m jdk.compiler/com.sun.tools.javac.Main -source 8 -target 8 -Xlint:-options -encoding UTF-8 -d "$classes" "$@"
fi
if [ "$mode" = "--demo" ]; then
    java -cp "$classes" notificationagent.HabitDemo
    exit 0
fi

java -cp "$classes" notificationagent.PriorityEngineTest

java -cp "$classes" notificationagent.IngestionBoundaryTest

java -cp "$classes" notificationagent.SessionIntakeTest

java -cp "$classes" notificationagent.DaypartFeedbackTest

java -cp "$classes" notificationagent.HabitDemoTest

java -cp "$classes" notificationagent.LowPrioritySummaryTest
