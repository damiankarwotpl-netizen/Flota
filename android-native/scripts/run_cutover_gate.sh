#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORT_PATH="$ROOT_DIR/docs/NATIVE_ANDROID_MIGRATION_EXECUTION_LOG.md"
RUN_TS="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

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

normalize_multiline() {
  sed 's/|/\\|/g' | tr '\n' ' ' | sed 's/  */ /g' | sed 's/^ *//; s/ *$//'
}

JAVA_STATUS="PASS"
JAVA_DETAIL=""
if JAVA_HOME_RESOLVED="$(pick_java_home 2>/dev/null)"; then
  export JAVA_HOME="$JAVA_HOME_RESOLVED"
  export PATH="$JAVA_HOME/bin:$PATH"
  JAVA_MAJOR="$(java_major_version "$JAVA_HOME" || echo "unknown")"
  JAVA_DETAIL="JAVA_HOME=$JAVA_HOME (major=$JAVA_MAJOR)"
else
  JAVA_STATUS="FAIL"
  JAVA_DETAIL="Could not resolve JDK 17/21"
fi

if [[ -x "$ROOT_DIR/gradlew" ]]; then
  GRADLE_CMD=("$ROOT_DIR/gradlew")
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD=(gradle)
else
  GRADLE_CMD=()
fi

GRADLE_STATUS="PASS"
GRADLE_DETAIL=""
if [[ ${#GRADLE_CMD[@]} -eq 0 ]]; then
  GRADLE_STATUS="FAIL"
  GRADLE_DETAIL="Neither ./gradlew nor gradle is available"
else
  GRADLE_DETAIL="Using ${GRADLE_CMD[*]}"
fi

SIGNING_STATUS="PASS"
SIGNING_DETAIL="All FLOTA_RELEASE_* variables are set"
for var_name in FLOTA_RELEASE_STORE_FILE FLOTA_RELEASE_STORE_PASSWORD FLOTA_RELEASE_KEY_ALIAS FLOTA_RELEASE_KEY_PASSWORD; do
  if [[ -z "${!var_name:-}" ]]; then
    SIGNING_STATUS="WARN"
    SIGNING_DETAIL="Release signing vars are missing (expected in production cutover)"
    break
  fi
done

BUILD_STATUS="SKIPPED"
BUILD_DETAIL="Build not attempted"
BUILD_LOG=""
if [[ "$JAVA_STATUS" == "PASS" && "$GRADLE_STATUS" == "PASS" ]]; then
  set +e
  BUILD_LOG="$(cd "$ROOT_DIR" && "${GRADLE_CMD[@]}" :app-admin:assembleDebug :app-driver:assembleDebug 2>&1)"
  BUILD_CODE=$?
  set -e

  if [[ $BUILD_CODE -eq 0 ]]; then
    BUILD_STATUS="PASS"
    BUILD_DETAIL="Debug builds assembled for app-admin and app-driver"
  else
    BUILD_STATUS="FAIL"
    BUILD_DETAIL="Debug build failed (exit=$BUILD_CODE): $(printf '%s' "$BUILD_LOG" | tail -n 8 | normalize_multiline)"
  fi
else
  BUILD_STATUS="SKIPPED"
  BUILD_DETAIL="Skipped because prerequisites failed"
fi

OVERALL="FAIL"
if [[ "$JAVA_STATUS" == "PASS" && "$GRADLE_STATUS" == "PASS" && "$BUILD_STATUS" == "PASS" ]]; then
  if [[ "$SIGNING_STATUS" == "PASS" || "$SIGNING_STATUS" == "WARN" ]]; then
    OVERALL="PASS"
  fi
fi

cat > "$REPORT_PATH" <<'REPORT'
# Native Android migration execution log

## Latest run
- Timestamp (UTC): __RUN_TS__
- Overall gate: __OVERALL__

## Gate checks
| Check | Status | Details |
| --- | --- | --- |
| Java toolchain (17/21) | __JAVA_STATUS__ | __JAVA_DETAIL__ |
| Gradle availability | __GRADLE_STATUS__ | __GRADLE_DETAIL__ |
| Debug build (`:app-admin:assembleDebug :app-driver:assembleDebug`) | __BUILD_STATUS__ | __BUILD_DETAIL__ |
| Release signing inputs (`FLOTA_RELEASE_*`) | __SIGNING_STATUS__ | __SIGNING_DETAIL__ |

## Completion interpretation
- Migration can be marked operationally complete only after QA/UAT + signed release cutover are executed and approved in real target environments.
- If this file reports a failing gate, treat migration as not yet operationally complete.
REPORT

sed -i \
  -e "s|__RUN_TS__|$RUN_TS|g" \
  -e "s|__OVERALL__|$OVERALL|g" \
  -e "s|__JAVA_STATUS__|$JAVA_STATUS|g" \
  -e "s|__JAVA_DETAIL__|$JAVA_DETAIL|g" \
  -e "s|__GRADLE_STATUS__|$GRADLE_STATUS|g" \
  -e "s|__GRADLE_DETAIL__|$GRADLE_DETAIL|g" \
  -e "s|__BUILD_STATUS__|$BUILD_STATUS|g" \
  -e "s|__BUILD_DETAIL__|$BUILD_DETAIL|g" \
  -e "s|__SIGNING_STATUS__|$SIGNING_STATUS|g" \
  -e "s|__SIGNING_DETAIL__|$SIGNING_DETAIL|g" \
  "$REPORT_PATH"

echo "Wrote $REPORT_PATH"
