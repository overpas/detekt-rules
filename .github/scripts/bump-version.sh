#!/usr/bin/env bash
set -euo pipefail

properties="${PROPERTIES_FILE:-gradle.properties}"
current=$(sed -n 's/^version=//p' "$properties")
IFS=. read -r major minor patch <<< "$current"

case "${1:-}" in
  major) next="$((major + 1)).0.0" ;;
  minor) next="$major.$((minor + 1)).0" ;;
  patch) next="$major.$minor.$((patch + 1))" ;;
  *) echo "Usage: $0 <major|minor|patch>" >&2; exit 1 ;;
esac

sed -i.bak "s/^version=.*/version=$next/" "$properties"
rm "$properties.bak"

echo "Version: $current -> $next"
echo "version=$next" >> "${GITHUB_OUTPUT:-/dev/null}"
