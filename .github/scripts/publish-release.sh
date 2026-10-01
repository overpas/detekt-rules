#!/usr/bin/env bash
set -euo pipefail

version="${1:?Usage: $0 <version>}"
tag="v$version"

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"

git add gradle.properties "releases/$version"
git commit -m "Release $version"
git tag "$tag"
git push --atomic origin HEAD:main "$tag"

gh release create "$tag" releases/"$version"/*.jar --title "$version" --generate-notes

git fetch origin develop
git checkout -B develop origin/develop
git merge --no-edit "$tag"
git push origin develop
