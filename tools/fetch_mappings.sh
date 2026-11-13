#!/usr/bin/env bash
# Pobiera mappings Yarn 1.21.1 (tiny v2) do .mappings/yarn/mappings/mappings.tiny
set -euo pipefail

VER="1.21.1+build.3"
mkdir -p .mappings/yarn
curl -fsSL -o .mappings/yarn-v2.jar \
  "https://maven.fabricmc.net/net/fabricmc/yarn/${VER}/yarn-${VER}-v2.jar"
unzip -q -o .mappings/yarn-v2.jar -d .mappings/yarn
echo "mappings: .mappings/yarn/mappings/mappings.tiny"
