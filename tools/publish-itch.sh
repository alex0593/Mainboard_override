#!/usr/bin/env bash
# Publish the debug APK to itch.io with butler (https://itch.io/docs/butler).
#
# Usage:
#   BUTLER_API_KEY=<key> ./tools/publish-itch.sh [channel]
#
# Defaults to channel "android" on aela-0593/mainboard-override. The APK
# version comes from app/build.gradle.kts (versionName -> --userversion).
# Butler is downloaded once into tools/.butler (gitignored, never committed).
# Needs JDK 17 (export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64) and the
# Android SDK, same as any Gradle build. Get the API key from
# https://itch.io/user/settings/api-keys (wharf API, push permission).
set -euo pipefail

CHANNEL="${1:-android}"
TARGET="aela-0593/mainboard-override:${CHANNEL}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUTLER_DIR="$ROOT/tools/.butler"
BUTLER="$BUTLER_DIR/butler"
BUTLER_VERSION="v15.31.0"

if [ -z "${BUTLER_API_KEY:-}" ]; then
    echo "error: set BUTLER_API_KEY first" >&2
    exit 1
fi

if [ ! -x "$BUTLER" ]; then
    echo "downloading butler $BUTLER_VERSION..."
    mkdir -p "$BUTLER_DIR"
    curl -sL -o /tmp/butler-itch.zip \
        "https://github.com/itchio/butler/releases/download/${BUTLER_VERSION}/butler-linux-amd64.zip"
    rm -rf "$BUTLER_DIR/unpack" && mkdir -p "$BUTLER_DIR/unpack"
    unzip -o -q /tmp/butler-itch.zip -d "$BUTLER_DIR/unpack"
    install -m 0755 "$BUTLER_DIR/unpack/linux-amd64/butler" "$BUTLER"
    rm -rf "$BUTLER_DIR/unpack" /tmp/butler-itch.zip
fi

VERSION="$(grep -oP 'versionName = "\K[^"]+' "$ROOT/app/build.gradle.kts")"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
"$ROOT/gradlew" -p "$ROOT" :app:assembleDebug

STAGE="$ROOT/app/build/itchUpload"
rm -rf "$STAGE" && mkdir -p "$STAGE"
cp "$ROOT/app/build/outputs/apk/debug/app-debug.apk" "$STAGE/mainboard-override.apk"

export BUTLER_API_KEY
exec "$BUTLER" push "$STAGE" "$TARGET" --userversion "$VERSION"
