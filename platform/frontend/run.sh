#!/usr/bin/env bash
set -euo pipefail

# Host-side wrapper. It deliberately uses only the approved project image.
root="$(cd "$(dirname "$0")/../.." && pwd)"
run_id="${PAPERCLIP_RUN_ID:?PAPERCLIP_RUN_ID is required}"
image="usermanager03-toolchain:7e663ce2-43fc-488e-a1ee-d1a44950d594"
command="${1:-verify}"
scratch="${PAPERCLIP_RUN_SCRATCH_DIR:?PAPERCLIP_RUN_SCRATCH_DIR is required}/frontend"
mkdir -p "$scratch"
case "$command" in
  generate) container_command=(bash platform/frontend/generate-api.sh /scratch) ;;
  verify) container_command=(bash -c 'npm --prefix frontend ci && npm --prefix frontend run generate:unicode && npm --prefix frontend run test && npm --prefix frontend run typecheck && npm --prefix frontend run build') ;;
  *) echo "uso: $0 {generate|verify}" >&2; exit 64 ;;
esac

docker run --rm --user "$(id -u):$(id -g)" \
  --label com.hariklabs.project=usermanager03 \
  --label com.hariklabs.lifecycle=temporary \
  --label "com.hariklabs.run=$run_id" \
  --mount "type=bind,source=$root,target=/workspace" \
  --mount "type=bind,source=$scratch,target=/scratch" \
  --workdir /workspace -e NPM_CONFIG_CACHE=/scratch/npm-cache "$image" "${container_command[@]}"
