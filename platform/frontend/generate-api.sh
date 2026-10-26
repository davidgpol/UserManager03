#!/usr/bin/env bash
set -euo pipefail

# Run inside the approved toolchain image from the repository root.
# The generated client is intentionally copied verbatim; do not hand-edit it.
output_dir="frontend/src/generated/api"
scratch_dir="${1:?Supply a writable scratch directory}"
rm -rf "$scratch_dir/typescript"
java -jar /opt/openapi-generator.jar generate \
  -i api/openapi.yaml \
  -c platform/openapi/typescript.json \
  -g typescript-fetch \
  --global-property apiDocs=false,modelDocs=false,apiTests=false,modelTests=false \
  -o "$scratch_dir/typescript"
rm -rf "$output_dir"
mkdir -p "$(dirname "$output_dir")"
cp -a "$scratch_dir/typescript/." "$output_dir/"
rm -rf "$output_dir/.openapi-generator" "$output_dir/.openapi-generator-ignore" "$output_dir/git_push.sh"
