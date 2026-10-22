# 002: CI on every branch, one Gradle job, aligned test libraries

- Status: active
- Type: tooling
- Depends on: none
- Related: `003`, `004`, `005`, `006`
- Branches: `mega-refactor-002a-ci-all-branches` (active, started 2026-10-22)

## Goal

Run the tests and the desktop build on every branch push and PR, so that a refactor branch gets the same checks as `master`.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Replace the 3 workflows with one `ci.yml`. Trigger on a push to any branch and on PRs to `master`. Cancel stale runs.
- One Gradle job: `engine:test core:test lwjgl3:jar` with `--continue` and no `--rerun-tasks`. Upload test reports on failure.
- Put `kotestVersion` (5.9.1) and `mockkVersion` in `gradle.properties` and use them in `engine` and `core`. Remove the 4.0.2 artifacts.
- Remove the unused `gradleVersion` property.
- Contract: the same test count, and all tests pass.

## Owner's input

Answer to review item 1 (2026-10-22, verbatim):

> Yes, align Kotest versions and support tests on all branch pushes

## Open questions

- Run CI on every branch push, including `old-v1`, `temp` and `overhaul_1`, which may fail today?
- Make CI a required status check for merges to `master` (GitHub branch protection)?

## Sources

- `NOTES.md` in this folder.
