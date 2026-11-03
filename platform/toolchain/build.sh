#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
run_id="${PAPERCLIP_RUN_ID:?PAPERCLIP_RUN_ID is required}"
image="${UM03_TOOLCHAIN_IMAGE:-usermanager03-toolchain:$run_id}"
cd "$root"
docker buildx build --builder default --platform linux/arm64 --load \
  --build-arg "RUN_ID=$run_id" -f platform/toolchain/Dockerfile -t "$image" .
docker run --rm --network none --user "$(id -u):$(id -g)" \
  --label com.hariklabs.project=usermanager03 \
  --label com.hariklabs.lifecycle=temporary \
  --label "com.hariklabs.run=$run_id" \
  --mount "type=bind,source=$root/platform/toolchain/verify.sh,target=/verify.sh,readonly" \
  "$image" bash /verify.sh
