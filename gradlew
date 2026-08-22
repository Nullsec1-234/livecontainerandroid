#!/usr/bin/env bash
BASEDIR="$(cd "$(dirname "$0")" && pwd)"
if [ -n "$JAVA_HOME" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="java"
fi
exec "$JAVA" -jar "$BASEDIR/gradle/wrapper/gradle-wrapper.jar" "$@"
