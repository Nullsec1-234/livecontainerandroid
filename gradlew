#!/bin/bash
# Graduate wrapper script
EXECUTABLE_DIRECTORY="$(cd "$(dirname "$0")" && pwd)"
GRADLE_OPTS=""
DEFAULT_GRADLE_OPTS="-Dgradle.user.home=$HOME/.gradle -Dgradle.gradle.home=$HOME/.gradle"

# Use the same JVM that was used to start this script
if [ -n "$_JAVA_OPTIONS" ]; then
  GRADLE_OPTS=$_JAVA_OPTIONS
fi

# Default OS detection
UNAME_S=$(uname -s)
case "$UNAME_S" in
  Linux*) MACHINE=Linux ;;
  Darwin*) MACHINE=Mac ;;
  *) MACHINE="UNKNOWN" ;;
esac

# Determine the gradle distribution to use
distribution_url="https://services.gradle.org/distributions/gradle-8.5-all.zip"

# Find the gradle wrapper properties
if [ -f "$EXECUTABLE_DIRECTORY/gradle/wrapper/gradle-wrapper.properties" ]; then
  distribution_url=$(cat "$EXECUTABLE_DIRECTORY/gradle/wrapper/gradle-wrapper.properties" | grep "^distributionUrl=" | cut -d'"' -f2)
fi

# Install gradle if not already installed
if [ ! -d "$EXECUTABLE_DIRECTORY/gradle" ]; then
  echo "Downloading Gradle..."
  cd "$EXECUTABLE_DIRECTORY"
  gradle_zip="gradle.zip"
  curl -sL "$distribution_url" -o "$gradle_zip"
  unzip -q "$gradle_zip"
  rm "$gradle_zip"
fi

# Run gradle
cd "$EXECUTABLE_DIRECTORY"
java $GRADLE_OPTS -jar gradle/wrapper/gradle-wrapper.jar "$@"
