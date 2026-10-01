#!/usr/bin/env bash
set -uo pipefail

file_path="$(jq -r '.tool_input.file_path // .tool_input.notebook_path // empty')"
[[ -z "$file_path" ]] && exit 0

dir="$(dirname "$file_path")"
while [[ ! -d "$dir" ]]; do
    dir="$(dirname "$dir")"
done

git_dir="$(git -C "$dir" rev-parse --path-format=absolute --git-dir 2>/dev/null)" || exit 0
common_dir="$(git -C "$dir" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)" || exit 0
project_common_dir="$(git -C "${CLAUDE_PROJECT_DIR:-$PWD}" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)" || exit 0

if [[ "$common_dir" == "$project_common_dir" && "$git_dir" == "$common_dir" ]]; then
    echo "Edits in the main checkout are blocked: $file_path. This project requires each session to work in its own worktree. Do not ask the user: call EnterWorktree now, then follow the Git rules of AGENTS.md and retry the edit in the worktree." >&2
    exit 2
fi
exit 0
