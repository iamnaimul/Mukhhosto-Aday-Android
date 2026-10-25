#!/usr/bin/env sh
# Lightweight Gradle launcher for this project. Downloads the pinned official distribution if needed.
set -eu
VERSION="8.7"
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/manual-wrapper"
DIST="$BASE/gradle-$VERSION"
ZIP="$BASE/gradle-$VERSION-bin.zip"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$BASE"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -O "$ZIP"
    else
      echo "Install curl or wget to download Gradle $VERSION." >&2; exit 1
    fi
  fi
  command -v unzip >/dev/null 2>&1 || { echo "Install unzip to extract Gradle." >&2; exit 1; }
  unzip -q -o "$ZIP" -d "$BASE"
fi
exec "$DIST/bin/gradle" "$@"
