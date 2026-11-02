# Plan: mega-refactor-004a-entity-smoke-test

- Status: approved
- Approved: 2026-11-01

Run Gradle with `JAVA_HOME=$HOME/.jdks/corretto-17.0.13`. `/usr/lib/jvm/java-17-openjdk-amd64` has no compiler, so `lwjgl3:jar` fails on it. Stage by path, not with `git add -A`.

## New APIs

No production API. All new code is test code in `core/src/smokeTest/`:

| Type | First caller |
|---|---|
| `RecordingTextureAtlas` | The atlas loader in `HeadlessHarness` |
| `HeadlessHarness` | `EntityInitSmokeTest` |
| `EntityInitSmokeTest` | Kotest |
| `RecordingTextureAtlasTest` | Kotest |

## Steps

### 1. Add the suite to the build

- Changes: in `core/build.gradle`, add the `smokeTest` JVM test suite, with `workingDir` at `assets/`, the headless
  backend, `gdx-platform:natives-desktop`, Kotest, MockK, `kotlin-reflect` and `sourceSets.test.output`. No FreeType
  natives. Make `check` depend on the suite. Step 2 adds the first spec, so this step has no commit of its own.
- Check: `./gradlew core:smokeTest` configures and compiles. `./gradlew core:test` still passes.
- Done: yes

### 2. Add the recording atlas and its test

- Changes: add `RecordingTextureAtlas` (overrides both `findRegion` forms and ignores a miss that comes through
  `containsRegion`) and `RecordingTextureAtlasTest`.
- Check: `RecordingTextureAtlasTest` passes: a miss is recorded, a hit is not, and a miss through `containsRegion` is
  not. Commit steps 1 and 2 together.
- Done: yes

### 3. Add the harness and the entity test

- Changes: add `HeadlessHarness` (the `HeadlessApplication`, the `GL20` mock, an `AssetManager` with a loader for
  `RecordingTextureAtlas`, every `TextureAsset`, `testServices`, `MegaEntityFactory.init()`). Add `EntityInitSmokeTest`
  (class discovery from a jar or a folder, the floor of more than 400 classes, one test per class, and the rule that a
  listed class which passes fails). Add `known-entity-failures.txt` with the 18 classes and a reason for each.
- Check: `./gradlew core:smokeTest` passes with 432 class tests. The run with no `DISPLAY` and no `WAYLAND_DISPLAY`
  passes. Commit.
- Done: yes

### 4. Add the suite to CI

- Changes: in `.github/workflows/ci.yml`, add `core:smokeTest` to the Gradle command and
  `core/build/reports/tests/smokeTest/` to the upload step.
- Check: read the diff. The CI run in step 6 proves it. Commit.
- Done: yes

### 5. Run the fault checks

- Changes: none stay. Make the 7 faults in `CONTRACT.md` one at a time, and revert each one. If a wrong key in `Coin.kt`
  throws, use another class that stores a region in a nullable field or a map, and write which class in the process
  log.
- Check: each fault fails as the contract says. `git status` shows no fault left. A fault that does not fail the suite
  is a defect in the suite: fix it, and run all the fault checks again.
- Done: yes

### 6. Run all the checks and push

- Changes: write the results in the branch process log. Commit the branch docs, by path.
- Check: all the automated checks in `CONTRACT.md` pass, including the `git diff` check on the three `src/main` folders.
  Then push the branch once (approved 2026-11-01) and confirm that the CI run passes and shows `core:smokeTest`.
- Done: yes

## After the merge, on `master`

These are not branch work. They are listed so that they do not get lost:

- `CLAUDE.md`: add `core:smokeTest` to "Build, run, test" and to the CI line.
- `docs/mega-refactor/_templates/branch/docs/CONTRACT.md`: add `core:smokeTest` to the automated checks.
- `ITEM.md`: close the two open questions, and correct the three wrong facts in a note.
- File the bug candidates in `docs/TODO.md`.
- `O6`: update "Current state", and ask the owner if `O6-R1` should name `core:smokeTest`.
