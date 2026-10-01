#!/usr/bin/env bash
set -euo pipefail

version="${1:?Usage: $0 <version>}"

gh release create "v$version" build/releases/"$version"/*.jar \
  --target "${GITHUB_SHA:?}" \
  --title "$version" \
  --generate-notes
