#!/bin/sh
# Checksum-verified bootstrap for the official Gradle 8.13 wrapper JAR.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$ROOT/gradle/wrapper/gradle-wrapper.jar"
SHA="81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f"
URL="https://downloads.gradle.org/distributions/gradle-8.13-wrapper.jar"
check() {
  if command -v sha256sum >/dev/null 2>&1; then
    ACTUAL=$(sha256sum "$1" | cut -d ' ' -f 1)
  elif command -v shasum >/dev/null 2>&1; then
    ACTUAL=$(shasum -a 256 "$1" | cut -d ' ' -f 1)
  else
    echo "A SHA-256 checking utility is required" >&2
    exit 1
  fi
  [ "$ACTUAL" = "$SHA" ] || {
    echo "Invalid Gradle wrapper JAR SHA-256; execution blocked" >&2
    exit 1
  }
}
if [ ! -f "$JAR" ]; then
  TMP="$JAR.download.$$"
  trap 'rm -f "$TMP"' EXIT HUP INT TERM
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error "$URL" --output "$TMP"
  elif command -v wget >/dev/null 2>&1; then
    wget -qO "$TMP" "$URL"
  else
    echo "curl or wget is required for the initial wrapper download" >&2
    exit 1
  fi
  check "$TMP"
  mv "$TMP" "$JAR"
  trap - EXIT HUP INT TERM
fi
check "$JAR"
JAVA=java
if [ -n "${JAVA_HOME:-}" ]; then JAVA="$JAVA_HOME/bin/java"; fi
exec "$JAVA" -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
