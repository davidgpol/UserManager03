#!/usr/bin/env bash
set -euo pipefail

# Run inside the project toolchain image. Never uses the host's Java/Node/npm.
test "$(uname -m)" = aarch64
java -version
java -version 2>&1 | grep -F 'Temurin-25.0.5+8'
mvn -version
mvn -version | grep -F 'Apache Maven 3.10.0'
test "$(node --version)" = v24.22.1
test "$(npm --version)" = 11.19.1
test "$(java -jar /opt/openapi-generator.jar version)" = 7.26.0
node --version
npm --version
java -jar /opt/openapi-generator.jar version
java -jar /opt/openapi-generator.jar config-help -g spring
