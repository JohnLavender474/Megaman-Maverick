# Notes: 002 CI on every branch, one Gradle job, aligned test libraries

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Testing, CI and bug tracking (2026-10-22)

Copied as written from the review section "ITEM 1. CI on all branches, `lwjgl3:jar`, Kotest alignment".

````text
(1) Current state
- There are three workflows. All of them run only on `push: branches: [ master ]`: `.github/workflows/engine-tests.yml:3-5`, `game-tests.yml:3-5`, `utils-tests.yml:3-5`. They do not run on other branches or on pull requests.
- Engine and core run in two separate jobs, each with `--rerun-tasks` (`engine-tests.yml:24`, `game-tests.yml:24`). That flag also recompiles everything. It gives no benefit in CI because the `build/` dirs are not cached between runs.
- No job builds `lwjgl3`, so a break in the launcher or in packaging is not caught.
- Kotest versions are mixed the same way in `engine/build.gradle:13-16` and `core/build.gradle:18-21`: `kotest-runner-junit5-jvm:4.6.0` together with `kotest-runner-junit5:4.0.2`, `kotest-assertions-core:4.0.2` and `kotest-property:4.0.2`. The local Gradle cache shows what resolves: runner, framework and assertions-core at 4.6.0, plus leftover 4.0.2 jars (`kotest-assertions-jvm`, `kotest-property-jvm`, `kotest-mpp-jvm`, `kotest-fp-jvm`). So two Kotest generations are on the test classpath now. MockK is `1.13.7` (2023).
- Toolchain: wrapper Gradle 8.10.1 (`gradle/wrapper/gradle-wrapper.properties`), Kotlin 2.3.20, JDK 17. `gradleVersion=9.4.1` in `gradle.properties:3` is not used by anything (grep). It misleads readers.
- Test usage is simple: 56 test files, 55 use `DescribeSpec`, 49 use `shouldBe`, and one uses `kotest-property` (`checkAll`/`Arb.int`). An upgrade touches very little test code.
- The repo is public (GitHub API: `private: false`), so Actions minutes cost nothing.

(2) Options for the Kotest version
- A. Kotest 5.9.1 (the last 5.x). Pros: works with Kotlin 2.x consumers on the JVM and with JDK 17. The 4.6 to 5.x change is small for `DescribeSpec`/`shouldBe`. Low risk. Cons: a maintenance-only line.
- B. Kotest 6.0.x (current). Pros: the current line, made for Kotlin 2.2+. Cons: more API removals than 5.x. It needs a check that the plain JUnit5 runner path still works without the Kotest Gradle plugin for JVM-only modules. I believe it does, but I did not verify it.
- C. Keep 4.6.0 and only remove the 4.0.2 lines. Pros: smallest change. Cons: 4.6 dates from 2021 and was built for Kotlin 1.5. The risk grows with each Kotlin upgrade.

(3) Recommendation
- Option A now, defined once. Add `kotestVersion=5.9.1` and `mockkVersion=1.14.x` to `gradle.properties` and use them in both modules. Then the two modules cannot drift apart again. Use these 3 deps only: `kotest-runner-junit5`, `kotest-assertions-core` and `kotest-property`. Move to 6.x later as a separate small change if wanted.
- Replace the 3 workflows with one `ci.yml`. The owner said "all branch pushes", so use these triggers:
  - `on: push: branches: ['**']` and `pull_request: branches: [master]`
  - `concurrency: { group: ${{ github.workflow }}-${{ github.ref }}, cancel-in-progress: true }`
  - job `gradle`: checkout, `setup-java@v4` (temurin 17), `gradle/actions/setup-gradle@v4`, then `./gradlew --continue engine:test core:test lwjgl3:jar`. Remove `--rerun-tasks`. Upload `engine/build/reports/tests/` and `core/build/reports/tests/` on failure.
  - job `utils`: the current pytest and node steps, unchanged. Item 2 adds a step later.
- Risks:
  - Branch pushes that also have a PR run twice. That is acceptable, or the job can skip the push run when a PR exists.
  - `lwjgl3:jar` triggers `generateAssetList`, which writes `assets/assets.txt`. The file is git-ignored (`.gitignore:181`), so it is harmless.
  - The Kotest upgrade can reveal tests that pass today only because of the mixed classpath. Run the suite once on the branch and compare test counts with master: same count, same results.
  - Side finding, out of scope but a CI risk: `engine/build.gradle:8-9` has `api gdx-freetype:1.11.0` and `api gdx-tools:1.6.3`. `gdx-tools` 1.6.3 is from 2015 and brings in old `gdx-backend-lwjgl`, which is in the Gradle cache. Note it for item 31/39. Do not fix it in item 1.

(4) Open questions
- Run CI on every branch push, or only on `mega-refactor-*`, `fix/*` and PRs? The owner said all. Confirm that this includes `old-v1`, `temp` and `overhaul_1`, which may fail today.
- Make CI a required status check for merges to `master`? That needs a GitHub branch-protection setting.

(5) Backlog wording
Title: CI on every branch and PR, one Gradle job, aligned test libraries
- Replace the 3 workflows with `ci.yml`: trigger on push to any branch and on PRs to `master`, and cancel stale runs.
- One Gradle job: `engine:test core:test lwjgl3:jar` with `--continue`, no `--rerun-tasks`, and test reports uploaded on failure.
- Put `kotestVersion` (5.9.1) and `mockkVersion` in `gradle.properties` and use them in `engine` and `core`. Remove the 4.0.2 artifacts.
- Contract: same test count, all tests pass. Remove the unused `gradleVersion` property.

(6) Dependencies: none. Items 2, 3, 4 and 5 add steps or tasks to this workflow.
````
