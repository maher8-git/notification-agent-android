#!/bin/sh
set -eu
cd "$(dirname "$0")"
classes=$(mktemp -d)
trap 'rm -r "$classes"' EXIT
set -- src/main/java/notificationagent/*.java src/test/java/notificationagent/*.java
if command -v javac >/dev/null 2>&1; then
    javac --release 8 -d "$classes" "$@"
else
    java -m jdk.compiler/com.sun.tools.javac.Main -source 8 -target 8 -Xlint:-options -d "$classes" "$@"
fi
java -cp "$classes" notificationagent.PriorityEngineTest
