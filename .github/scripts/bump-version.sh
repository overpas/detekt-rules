#!/usr/bin/env bash
set -euo pipefail

latest=$(git describe --tags --abbrev=0 --match 'v[0-9]*' 2>/dev/null || echo v0.0.0)
current="${latest#v}"
IFS=. read -r major minor patch <<< "$current"

case "${1:-}" in
  major) next="$((major + 1)).0.0" ;;
  minor) next="$major.$((minor + 1)).0" ;;
  patch) next="$major.$minor.$((patch + 1))" ;;
  *) echo "Usage: $0 <major|minor|patch>" >&2; exit 1 ;;
esac

echo "Version: $current -> $next"
echo "version=$next" >> "${GITHUB_OUTPUT:-/dev/null}"
