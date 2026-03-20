#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/android-native"

ensure_supported_java() {
  local java_bin current_home major

  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    current_home="$JAVA_HOME"
  else
    java_bin="$(command -v java 2>/dev/null || true)"
    if [[ -z "$java_bin" ]]; then
      current_home=""
    else
      current_home="$(cd "$(dirname "$(dirname "$(readlink -f "$java_bin")")")" && pwd)"
    fi
  fi

  if [[ -n "$current_home" && -x "$current_home/bin/java" ]]; then
    major="$($current_home/bin/java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p' | head -n1)"
    if [[ -z "$major" ]]; then
      major="$($current_home/bin/java -version 2>&1 | sed -n 's/.*version "1\.\([0-9][0-9]*\).*/\1/p' | head -n1)"
    fi
  else
    major=""
  fi

  if [[ -z "$major" || "$major" -le 21 ]]; then
    return
  fi

  for candidate in \
    "$HOME/.local/share/mise/installs/java/17.0.2" \
    "/root/.local/share/mise/installs/java/17.0.2"
  do
    if [[ -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      return
    fi
  done
}

normalize_args() {
  local arg
  for arg in "$@"; do
    case "$arg" in
      :android-native:*)
        printf ':%s\n' "${arg#:android-native:}"
        ;;
      android-native:*)
        printf '%s\n' "${arg#android-native:}"
        ;;
      *)
        printf '%s\n' "$arg"
        ;;
    esac
  done
}

if ! command -v gradle >/dev/null 2>&1; then
  echo "gradle is required on PATH to run this repository build." >&2
  exit 1
fi

ensure_supported_java
mapfile -t args < <(normalize_args "$@")
exec gradle -p "$PROJECT_DIR" "${args[@]}"
