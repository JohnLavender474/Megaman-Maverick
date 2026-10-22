# Process log: mega-refactor-002a-ci-all-branches

## Status

- State: merged (2026-10-22)
- Goal: CI on every branch push, one Gradle job, aligned test libraries, JDK toolchain.
- Done: branch start; scan; `docs/SCOPE.md` and `docs/CONTRACT.md` (approved); `docs/PLAN.md` (approved).
- Next: none.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-22

- Branch start on `master` (wave 1, with `019a` and the other wave 1 branch).
- Planning step 1 (scan) and step 2: wrote the draft `docs/SCOPE.md`.
- The user approved `docs/SCOPE.md` with answers to its open questions. Planning step 3: wrote the draft `docs/CONTRACT.md`.
- The user approved `docs/CONTRACT.md`. Planning step 4: wrote the `docs/PLAN.md` (approved).
- The user approved `docs/PLAN.md`.
- Plan step 1: baseline taken (shared by the wave 1 branches). See `docs/NOTES.md`.
- Step 2 (`89bb322e9`): `targetCompatibility = 17`, `options.release = 17` for every Java compile
  task, and the project-level `kotlin.compilerOptions.jvmTarget` 17 (covers main and test).
  JDK 25 still failed: Gradle 8.10.1 cannot run on it. A daemon JVM file fixed that in a test, but
  it was outside the scope; the owner chose to skip JDK 25 (plan and contract changed: JDK 21
  instead of JDK 25). Proof with JDK 21: the old build fails at `:engine:compileTestKotlin`
  (Java 17, Kotlin 21); the new build passes with 427 and 49 tests and Java 17 class files.
- Steps 3-4 (`8fcaf781e`): Kotest 5.9.1 and MockK 1.14.11 from `gradle.properties`; removed
  `gradleVersion`. `SpriteSystemTest` failed once; cause and fix in `docs/NOTES.md`. After the fix:
  427 engine and 49 core tests pass, the same counts as the baseline.
- Step 5 (`d80163553`): `.github/workflows/ci.yml` replaces the three workflows (jobs `gradle`,
  `utils-pytest`, `utils-node`). The YAML parses; the `utils` steps are unchanged. Local `utils`
  runs: level_analyzer 2 passed, level_builder 1 passed, diagnostics 35 passed, tiled_extensions
  30 passed.
- Step 6, local contract checks (Corretto JDK 17, clean build): build succeeds; `engine:test`
  50 specs, 427 tests, 0 failures; `core:test` 4 specs, 49 tests, 0 failures; JAR builds
  (35,895,548 bytes); 133 compiler warnings (same as the baseline); test classpaths hold only
  Kotest 5.9.1 and MockK 1.14.11; `javap` shows major version 61 for classes in `engine`, `core`
  and `lwjgl3`. JDK 21 passed in step 2.
- Step 7: this shell has no GitHub credentials, so the owner pushed the branch. CI run
  `https://github.com/JohnLavender474/Megaman-Maverick/actions/runs/39854839528` on `8aa008c41`:
  `gradle` (1 min), `utils-pytest` and `utils-node` all passed. Only the `CI` workflow ran. The job
  log needs authentication, so the test counts in the log were not read here; the `gradle` job
  ran `engine:test core:test lwjgl3:jar` and passed.
- The owner approved the changes and the merge (no separate play test result was reported; the
  branch changes no game code).
- Merge step 2: `master` gained the 28 new tests of `008a`, written against Kotest 4, and this
  branch moves all tests to Kotest 5, so `master` was merged into the branch first. Checks:
  clean build (Corretto JDK 17), 427 engine and 77 core tests pass under Kotest 5, 133 warnings.
  Squash-merged into `master`.

