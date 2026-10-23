#!/bin/sh
set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
GRADLE_VERSION=8.11.1
GRADLE_SHA256=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6
BOOT_DIR="$APP_HOME/.gradle-bootstrap"
GRADLE_HOME="$BOOT_DIR/gradle-$GRADLE_VERSION"
ZIP="$BOOT_DIR/gradle-$GRADLE_VERSION-bin.zip"
TMP_ZIP="$ZIP.part"
URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

verify_zip() {
  if command -v sha256sum >/dev/null 2>&1; then
    printf '%s  %s\n' "$GRADLE_SHA256" "$1" | sha256sum -c - >/dev/null
  elif command -v shasum >/dev/null 2>&1; then
    actual=$(shasum -a 256 "$1" | awk '{print $1}')
    [ "$actual" = "$GRADLE_SHA256" ]
  else
    echo "Warning: SHA-256 tool not found; Gradle archive could not be verified." >&2
    return 0
  fi
}

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BOOT_DIR"
  if [ ! -f "$ZIP" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    rm -f "$TMP_ZIP"
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 2 "$URL" -o "$TMP_ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$TMP_ZIP" "$URL"
    else
      echo "curl or wget is required to bootstrap Gradle." >&2
      exit 1
    fi
    if ! verify_zip "$TMP_ZIP"; then
      rm -f "$TMP_ZIP"
      echo "Gradle archive checksum verification failed." >&2
      exit 1
    fi
    mv "$TMP_ZIP" "$ZIP"
  elif ! verify_zip "$ZIP"; then
    rm -f "$ZIP"
    echo "Cached Gradle archive checksum verification failed. Run again to re-download." >&2
    exit 1
  fi
  if ! command -v unzip >/dev/null 2>&1; then
    echo "unzip is required to bootstrap Gradle." >&2
    exit 1
  fi
  unzip -q -o "$ZIP" -d "$BOOT_DIR"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
