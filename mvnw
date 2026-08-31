#!/bin/sh
set -eu
MAVEN_VERSION=3.9.11
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_HOME="$BASE_DIR/.mvn/wrapper/dists/apache-maven-$MAVEN_VERSION"
MAVEN_CMD="$WRAPPER_HOME/apache-maven-$MAVEN_VERSION/bin/mvn"
if [ ! -x "$MAVEN_CMD" ]; then
  mkdir -p "$WRAPPER_HOME"
  ARCHIVE="$WRAPPER_HOME/maven.zip"
  curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip" -o "$ARCHIVE"
  unzip -q "$ARCHIVE" -d "$WRAPPER_HOME"
  rm "$ARCHIVE"
fi
exec "$MAVEN_CMD" "$@"

