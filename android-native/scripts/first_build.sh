#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_KIND="${1:-debug}"

java_major_version() {
  local java_bin="$1/bin/java"
  [[ -x "$java_bin" ]] || return 1
  "$java_bin" -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -n 1
}

pick_java_home() {
  if [[ -n "${JAVA_HOME:-}" ]]; then
    local current_major
    current_major="$(java_major_version "$JAVA_HOME" || true)"
    if [[ "$current_major" == "17" || "$current_major" == "21" ]]; then
      echo "$JAVA_HOME"
      return 0
    fi
  fi
  for candidate in \
    "$HOME/.local/share/mise/installs/java/21.0.2" \
    "$HOME/.local/share/mise/installs/java/17.0.2"
  do
    if [[ -x "$candidate/bin/java" ]]; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}

if ! JAVA_HOME_RESOLVED="$(pick_java_home)"; then
  echo "ERROR: Could not resolve JAVA_HOME. Set JAVA_HOME to JDK 17 or 21." >&2
  exit 1
fi
export JAVA_HOME="$JAVA_HOME_RESOLVED"
export PATH="$JAVA_HOME/bin:$PATH"

SDK_PATH="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -n "$SDK_PATH" ]]; then
  cat > "$ROOT_DIR/local.properties" <<PROPS
sdk.dir=$SDK_PATH
PROPS
  echo "Wrote $ROOT_DIR/local.properties using sdk.dir=$SDK_PATH"
else
  echo "WARNING: ANDROID_HOME / ANDROID_SDK_ROOT not set. local.properties was not generated." >&2
fi

if [[ -x "$ROOT_DIR/gradlew" ]]; then
  GRADLE_CMD=("$ROOT_DIR/gradlew")
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD=(gradle)
else
  echo "ERROR: Neither ./gradlew nor gradle is available." >&2
  exit 1
fi

case "$BUILD_KIND" in
  debug)
    TASKS=(":app-admin:assembleDebug" ":app-driver:assembleDebug")
    ;;
  release)
    TASKS=(":app-admin:assembleRelease" ":app-driver:assembleRelease")
    ;;
  *)
    echo "ERROR: Unknown build kind '$BUILD_KIND'. Use 'debug' or 'release'." >&2
    exit 1
    ;;
esac

echo "Using JAVA_HOME=$JAVA_HOME"
echo "Running: ${GRADLE_CMD[*]} ${TASKS[*]}"
cd "$ROOT_DIR"
"${GRADLE_CMD[@]}" "${TASKS[@]}"
