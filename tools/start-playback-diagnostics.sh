#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PROPERTIES_FILE="$ROOT_DIR/local.dev.properties"
PORT="${NUVIO_DIAGNOSTICS_PORT:-8787}"

if [[ ! -f "$PROPERTIES_FILE" ]]; then
  echo "Missing $PROPERTIES_FILE" >&2
  echo "Create it with PLAYBACK_REPORTS_API_TOKEN=<token> and PLAYBACK_REPORTS_BASE_URL=http://<this-machine-ip>:$PORT/" >&2
  exit 1
fi

TOKEN="$(awk -F= '$1 == "PLAYBACK_REPORTS_API_TOKEN" {print substr($0, index($0, "=") + 1)}' "$PROPERTIES_FILE" | tail -n 1 | tr -d '\r')"
exec python3 "$ROOT_DIR/tools/playback-diagnostics-server.py" \
  --host "${NUVIO_DIAGNOSTICS_HOST:-0.0.0.0}" \
  --port "$PORT" \
  --token "$TOKEN"
