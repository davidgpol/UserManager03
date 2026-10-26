#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
private="${UM03_PRIVATE_DIR:?private directory required}"
evidence="${UM03_EVIDENCE_DIR:?evidence directory required}"
mkdir -p "$evidence"
spki="$(openssl x509 -in "$private/access/https.crt" -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl base64 -A)"
curl --fail --silent --cacert "$private/access/https.crt" https://localhost:18443/login > /dev/null
# Chromium pins this project certificate; curl above checks hostname, dates and trust.
docker run --rm --init --network host --shm-size=256m --user 1001:1001 \
  --label com.hariklabs.project=usermanager03 --label com.hariklabs.lifecycle=temporary \
  --label "com.hariklabs.run=$PAPERCLIP_RUN_ID" \
  --mount "type=bind,source=$root,target=/workspace,readonly" \
  --mount "type=bind,source=$private/access,target=/private,readonly" \
  --mount "type=bind,source=$evidence,target=/evidence" \
  -e NODE_EXTRA_CA_CERTS=/private/https.crt -e "UM03_TLS_SPKI=$spki" -e "UM03_DATASET=${UM03_DATASET:-73000000}" \
  "usermanager03-browser:$PAPERCLIP_RUN_ID" node "${1:-platform/browser/smoke.cjs}"
