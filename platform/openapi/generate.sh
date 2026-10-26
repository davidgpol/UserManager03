#!/usr/bin/env bash
set -euo pipefail

# Invoke from the repository root inside the approved toolchain image.
# An output root is mandatory: use scratch for pre-publication verification.
output_root="${1:?Supply an output directory}"
test "$(java -jar /opt/openapi-generator.jar version)" = 7.26.0
java -jar /opt/openapi-generator.jar validate -i api/openapi.yaml
java -jar /opt/openapi-generator.jar generate \
  -i api/openapi.yaml -c platform/openapi/spring.json -g spring \
  --global-property apis,models,apiDocs=false,modelDocs=false,apiTests=false,modelTests=false \
  -o "$output_root/java"
java -jar /opt/openapi-generator.jar generate \
  -i api/openapi.yaml -c platform/openapi/typescript.json -g typescript-fetch \
  --global-property apiDocs=false,modelDocs=false,apiTests=false,modelTests=false \
  -o "$output_root/typescript"
