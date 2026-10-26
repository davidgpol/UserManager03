#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
: "${UM03_PRIVATE_DIR:?set private configuration directory}"
: "${UM03_APP_IMAGE:?set verified image}"
: "${PAPERCLIP_RUN_ID:?set actual run id}"
command="${1:-status}"
case "$command" in
  status) docker compose -f "$root/platform/runtime/compose.yaml" ps ;;
  up) docker compose -f "$root/platform/runtime/compose.yaml" up -d --wait --wait-timeout 150 ;;
  recreate) docker compose -f "$root/platform/runtime/compose.yaml" up -d --force-recreate --wait --wait-timeout 150 ;;
  restart-app) docker compose -f "$root/platform/runtime/compose.yaml" restart app ;;
  *) echo 'usage: operate.sh {status|up|recreate|restart-app}' >&2; exit 64 ;;
esac
