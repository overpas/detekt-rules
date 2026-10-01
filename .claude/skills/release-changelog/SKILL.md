---
name: release-changelog
description: Writes the changelog of a released version into the changelog file and into the GitHub release body. Invoke after a release workflow run, when the user asks to write or fix the changelog or the release notes of a version, or when a release body lists only PR titles.
---

# Release changelog

The release workflow publishes PR titles as the release body. This skill replaces them with the
changes that the end user of the rules sees.

## Audience

- The reader is the end user of the rules, not the person who prompts the session. Describe what
  changed for that end user.
- Do not repeat or reflect the instructions, wording or goals of the prompter.
- Leave out internal refactors, CI, agent setup and test-only changes.
- Never name the project that the rules came from.

## Steps

1. Find the range: the target tag and the previous `v*` tag (`git tag --sort=-v:refname`).
2. Collect the facts from `git diff <previous>..<target>`:

   | Source in the diff                                    | Category                     |
   |-------------------------------------------------------|------------------------------|
   | Rule lists of the rule set providers                  | Added rules, Removed rules   |
   | `@Configuration` properties and their `config` values | Changed options and defaults |
   | Visitor logic of a rule                               | Changed detection            |
   | Rule descriptions and finding messages                | Changed messages             |
   | Build, release scripts, README setup sections         | Distribution                 |

3. Write a `## <version>` section in the changelog file, above the previous version. Use only the
   categories that have entries, in the order of the table, as `###` headings.
4. Start each entry with `` `<rule-set-id>`: `<Rule>` `` and say what changed. For a removed
   option, a changed default or changed detection, also say how to keep the old behavior.
5. Commit on a new branch and open a PR, as the Git rules of the workspace require.
6. Show the section to the user. After the user confirms, replace the release body with the section
   text and a `**Full Changelog**: <repo-url>/compare/v<previous>...v<target>` line:
   `gh release edit v<target> --notes-file <file>`.
