# Scope: mega-refactor-002a-ci-all-branches

What this branch changes and why. The user approves this file before `CONTRACT.md` is written.

- Status: approved
- Approved: 2026-10-22

## Goal

Every branch push and every PR to `master` runs the engine tests, the game tests, the desktop
JAR build and the `utils/` tests. The test libraries have one version each. The build works no
matter which JDK runs Gradle.

## Item

`002` (CI on every branch, one Gradle job, aligned test libraries), all of its proposed scope,
plus the fix for `docs/issues/2026-10-22-jdk25-test-jvm-target.md`, which the item notes say
belongs here.

## Current state

Scan of 2026-10-22 on `master` at `612edecf8`:

- Three workflows (`.github/workflows/engine-tests.yml`, `game-tests.yml`, `utils-tests.yml`)
  run only on `push` to `master`. Nothing runs on other branches or on PRs.
- The engine and game jobs run separately, each with `--rerun-tasks`. That flag recompiles
  everything and gives no benefit, because CI does not cache `build/`.
- No job builds `lwjgl3`, so a break in the launcher or the JAR is not found.
- `engine/build.gradle:13-17` and `core/build.gradle:18-22` mix Kotest 4.6.0
  (`kotest-runner-junit5-jvm`) with 4.0.2 (`kotest-runner-junit5`, `kotest-assertions-core`,
  `kotest-property`). MockK is 1.13.7. The versions are written in each module, not in
  `gradle.properties`.
- `gradle.properties:3` has `gradleVersion=9.4.1`, which nothing uses. The wrapper is Gradle
  8.10.1.
- `build.gradle:61` sets the Kotlin JVM target to 17 only for `compileKotlin`. With JDK 25,
  `compileTestKotlin` targets 25 while Java targets 17, and the build fails.
- The repo is public, so Actions minutes cost nothing.
- Nothing from this repo is pushed yet in this session. CI on a branch runs only after the branch
  is pushed to GitHub.

## In scope

- Replace the 3 workflows with one `.github/workflows/ci.yml`:
  - triggers: `push` to any branch, and `pull_request` to `master`;
  - cancel a running job when a newer push arrives on the same branch;
  - job `gradle`: JDK 17 (Temurin), `gradle/actions/setup-gradle`,
    `./gradlew --continue engine:test core:test lwjgl3:jar`, no `--rerun-tasks`, test reports
    uploaded on failure;
  - job `utils`: the current Python and Node steps, unchanged.
- Add `kotestVersion` and `mockkVersion` to `gradle.properties`. Use them in `engine` and
  `core`. Use three Kotest artifacts only: `kotest-runner-junit5`, `kotest-assertions-core`,
  `kotest-property`. Target Kotest 5.9.1 and MockK 1.14.x.
- Remove the unused `gradleVersion` property.
- Set the JVM target to 17 for all Kotlin and Java compile tasks, including the test tasks, so
  that the build also runs when Gradle runs on a newer JDK that Gradle 8.10.1 supports (for
  example JDK 21). JDK 25 is out of scope (owner, 2026-10-22): Gradle 8.10.1 itself cannot run
  on it.

## Out of scope

- The other `engine/build.gradle` dependency problems (`gdx-freetype:1.11.0`, `gdx-tools`,
  `gdx-controllers-desktop`). They belong to `032`.
- Branch protection on GitHub (a required status check). That is a GitHub setting for the owner.
- New tests. Items `003`–`006` add them later.
- Any game code change.

## Affected areas and risks

- Files: `.github/workflows/`, `gradle.properties`, `build.gradle`, `engine/build.gradle`,
  `core/build.gradle`.
- The Kotest upgrade can change how tests are found or reported. A test that passes today only
  because of the mixed classpath can fail. Check: the same test counts as the baseline (427
  engine, 49 core), and all pass.
- Kotest 5 removed some 4.x APIs. The tests use mostly `DescribeSpec` and `shouldBe`, and one
  file uses `kotest-property`, so few changes are expected.
- A push to a branch that also has a PR runs CI twice. That is acceptable.

## Overlap with other branches

Wave 1 runs `019a` (`MegaLevelScreen.kt`) and `008a` (the launcher and boot code) at the same
time. Neither changes the files of this branch. If `008a` adds tests, they run under the new
Kotest version after both merge; the second branch to merge checks that.

## Open questions

None open.

- (2026-10-22) CI runs on a push to any branch, including old branches.
- (2026-10-22) Skip JDK 25. It is very new, and Gradle 8.10.1 cannot run on it (it fails to
  compile the build script: "Unsupported class file major version 69"). Supporting it needs a
  newer Gradle or a daemon JVM file, which this branch does not add.
