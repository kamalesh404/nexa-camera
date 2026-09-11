#!/bin/sh
set -eu
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION=8.10.2
DIST_DIR="$BASE_DIR/.gradle-dist"
INSTALL_DIR="$DIST_DIR/gradle-$VERSION"
ZIP_FILE="$DIST_DIR/gradle-$VERSION-bin.zip"
if [ ! -x "$INSTALL_DIR/bin/gradle" ]; then
  mkdir -p "$DIST_DIR"
  if command -v curl >/dev/null 2>&1; then curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP_FILE"; else wget -O "$ZIP_FILE" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"; fi
  unzip -q -o "$ZIP_FILE" -d "$DIST_DIR"
fi
exec "$INSTALL_DIR/bin/gradle" "$@"
