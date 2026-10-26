#!/usr/bin/env bash
set -euo pipefail
cd /workspace
python3 platform/backend/generate-unicode.py
mvn -B -f backend/pom.xml "$@"
