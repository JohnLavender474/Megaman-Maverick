# Plan: mega-refactor-002a-ci-all-branches

How to do the work in `SCOPE.md`. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-22

All steps run in the worktree `.worktrees/mega-refactor-002a-ci-all-branches/`.

## Steps

### 1. Baseline

- Changes: none.
- Check: run the contract command with Corretto JDK 17. Write the test counts, the JAR size and
  the compiler warning count in `NOTES.md`. Also run it with the default JDK 25 and write down
  the failure, to show the problem before the fix.
- Done: yes (2026-10-22)

### 2. JVM target 17 for all compile tasks

- Changes: in `build.gradle`, set the Kotlin JVM target to 17 for every Kotlin compile task
  (main and test), not only `compileKotlin`, and set `options.release = 17` for every Java compile
  task. Remove the per-task line that only covers `compileKotlin`. `lwjgl3/build.gradle` keeps its
  own settings unless they conflict.
- Check: the contract command passes with JDK 17 and with JDK 21. `javap -v` on one class from
  each module shows major version 61. (JDK 25 dropped on 2026-10-22, see the process log.)
- Commit: "Target Java 17 in all compile tasks".
- Done: yes (2026-10-22)

### 3. One Kotest and one MockK version

- Changes: add `kotestVersion=5.9.1` and `mockkVersion=1.14.x` (the latest 1.14 release) to
  `gradle.properties`. In `engine/build.gradle` and `core/build.gradle`, use only
  `kotest-runner-junit5`, `kotest-assertions-core` and `kotest-property` with `$kotestVersion`,
  and MockK with `$mockkVersion`. Fix any test code that the upgrade breaks, with the smallest
  change.
- Check: the contract command passes, with the same number of tests as the baseline in each
  module, all passing. The `testRuntimeClasspath` has no Kotest 4.x artifact and one MockK
  version.
- Commit: "Align Kotest and MockK versions".
- Done: yes (2026-10-22)

### 4. Remove the unused `gradleVersion` property

- Changes: delete `gradleVersion=9.4.1` from `gradle.properties`.
- Check: `git grep gradleVersion` finds no user; the build passes.
- Commit with step 3, or alone.
- Done: yes (2026-10-22)

### 5. One CI workflow

- Changes: add `.github/workflows/ci.yml` with the triggers, the cancel rule, the `gradle` job and
  the `utils` job from `SCOPE.md`. Delete `engine-tests.yml`, `game-tests.yml` and
  `utils-tests.yml`.
- Check: the YAML parses (for example with Python `yaml.safe_load`); the `utils` steps are the
  same as the old ones; run the `utils` test commands locally.
- Commit: "Run CI on every branch with one Gradle job".
- Done: yes (2026-10-22)

### 6. Local contract checks

- Changes: none.
- Check: do all "Automated checks" in `CONTRACT.md`. Write the results in the process log.
- Done: yes (2026-10-22)

### 7. CI run on GitHub

- Changes: none.
- Check: ask the owner before pushing. After the push, the `ci.yml` run passes, with the same
  test counts as the local run. Write the run link and result in the process log.
- Done: yes (2026-10-22)

### 8. Play test (owner)

- Changes: none.
- Check: the "Manual playthrough" in `CONTRACT.md`.
- Done: yes (2026-10-22)

### 9. Merge

- Changes: follow "Branch merge" in `docs/mega-refactor/CLAUDE.md`, including the owner's
  approval and, if `master` changed, a merge of `master` into the branch and the checks again.
  On `master`, close `docs/issues/2026-10-22-jdk25-test-jvm-target.md` and update the CI line in
  the root `CLAUDE.md`.
- Check: `master` builds with the contract command.
- Done: yes (2026-10-22)
