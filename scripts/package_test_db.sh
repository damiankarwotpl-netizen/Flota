#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_DIR="$REPO_ROOT/assets/flota_test_database_pack"
OUT_ZIP="$REPO_ROOT/assets/flota_test_database_pack_local.zip"

if [[ ! -d "$SRC_DIR" ]]; then
  echo "Brak katalogu źródłowego: $SRC_DIR" >&2
  exit 1
fi

cd "$REPO_ROOT/assets"
rm -f "$OUT_ZIP"
zip -r "$(basename "$OUT_ZIP")" "$(basename "$SRC_DIR")"
echo "Gotowe: $OUT_ZIP"
