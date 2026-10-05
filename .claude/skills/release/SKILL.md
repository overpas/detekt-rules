---
name: release
description: Releases a new version end to end. Writes the changelog, merges it into develop, merges develop into main, runs the release workflow and writes the GitHub release body. Invoke when the user asks to release, publish or cut a new version, or to run the release workflow.
argument-hint: "[patch|minor|major]"
allowed-tools: Read, Edit, Write, Bash(git fetch:*), Bash(git checkout:*), Bash(git switch:*), Bash(git tag:*), Bash(git describe:*), Bash(git diff:*), Bash(git log:*), Bash(git status:*), Bash(git add:*), Bash(git commit:*), Bash(git push:*), Bash(.github/scripts/bump-version.sh:*), Bash(gh repo view:*), Bash(gh pr create:*), Bash(gh pr checks:*), Bash(gh pr merge:*), Bash(gh pr view:*), Bash(gh pr list:*), Bash(gh workflow run:*), Bash(gh run list:*), Bash(gh run watch:*), Bash(gh run view:*), Bash(gh release view:*), Bash(gh release edit:*)
---

# Release

The release workflow runs only on `main` and publishes PR titles as the release body. This skill
puts the changelog and all of `develop` on `main` first, then runs the workflow, then replaces the
release body with the changelog section.

## Audience of the changelog

- The reader is the end user of the rules, not the person who prompts the session. Describe what
  changed for that end user.
- Do not repeat or reflect the instructions, wording or goals of the prompter.
- Leave out internal refactors, CI, agent setup and test-only changes.
- Never name the project that the rules came from.

## Steps

1. Get the bump (`patch`, `minor` or `major`) from the arguments. If there is none, ask the user.
2. Run `git fetch origin --tags` and `git checkout --detach origin/main`. Get the previous tag with
   `git describe --tags --abbrev=0 --match 'v[0-9]*'` and the new version with
   `.github/scripts/bump-version.sh <bump>`. The workflow computes the version the same way.
3. Write the changelog:
    1. `git checkout -b changelog-<version> origin/develop`.
    2. Collect the facts from `git diff v<previous>..origin/develop`:

       | Source in the diff                                    | Category                     |
       |-------------------------------------------------------|------------------------------|
       | Rule lists of the rule set providers                  | Added rules, Removed rules   |
       | `@Configuration` properties and their `config` values | Changed options and defaults |
       | Visitor logic of a rule                               | Changed detection            |
       | Rule descriptions and finding messages                | Changed messages             |
       | Build, release scripts, README setup sections         | Distribution                 |

    3. Write a `## <version>` section in the changelog file, above the previous version. Use only
       the categories that have entries, in the order of the table, as `###` headings.
    4. Start each entry with `` `<rule-set-id>`: `<Rule>` `` and say what changed. For a removed
       option, a changed default or changed detection, also say how to keep the old behavior.
    5. Commit, `git push -u origin changelog-<version>` and
       `gh pr create --base develop --title "Add the <version> changelog"`.
4. Merge the changelog PR: `gh pr checks <pr> --watch --fail-fast`. When all checks pass, run
   `git checkout --detach` and `gh pr merge <pr> --merge --delete-branch`.
5. Merge `develop` into `main`:
   `gh pr create --base main --head develop --title "Release <version>" --body "Release <version>"`,
   then `gh pr checks <pr> --watch --fail-fast` and `gh pr merge <pr> --merge`. Do not delete
   `develop`.
6. Run the release: `gh workflow run release.yml --ref main -f bump=<bump>`. Get the run ID with
   `gh run list --workflow release.yml --limit 1 --json databaseId,status,createdAt` (the run can
   take a few seconds to appear), then `gh run watch <id> --exit-status`.
7. Write the release body:
    1. Make sure that `gh release view v<version>` shows the release. If the workflow released
       another version, stop.
    2. Write the changelog section and a
       `**Full Changelog**: <repo-url>/compare/v<previous>...v<version>` line to a file in the
       scratchpad, and run `gh release edit v<version> --notes-file <file>`.
8. Report the version, the release URL and the merged PRs.

If a step fails, stop. Start the report with "Skill incomplete: step N" and the error, then
propose the fix.
