# Contract: mega-refactor-004a-entity-smoke-test

- Status: approved
- Approved: 2026-11-01, with one push of the branch for the CI check

This branch adds a test suite and changes no production code. Run Gradle with `JAVA_HOME` on JDK 17 or 21.

## Intended changes

Gameplay changes: none.

Build and CI changes on purpose:

- `core:smokeTest` is a new task, and `check` depends on it.
- CI runs `core:smokeTest` and uploads its report on a failure.

## Behavior that must not change

- `core/src/main`, `engine/src/main` and `lwjgl3/src/main` have no changes.
- `core:test` keeps its dependencies: the new `gdx-backend-headless:1.14.2` line and the desktop natives go only on the
  `smokeTest` classpath. `gdx-backend-headless:1.6.3` was already on the `core` and `lwjgl3` classpaths, through
  `gdx-tools` in `engine`.
- The game JAR gets no new dependency.
- `testServices()` and the existing tests have no changes.

## Automated checks

Experimental checks come first.

- [x] `./gradlew core:smokeTest` passes, with one test for each of the 432 classes.
- [x] `env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew core:smokeTest --rerun` passes with no display (`O6-R3`).
- [x] `RecordingTextureAtlasTest` passes: a `findRegion` miss is recorded, a hit is not, and a miss through
  `containsRegion` is not.
- [x] `EntityInitSmokeTest` checks that discovery finds more than 400 classes, so an empty scan fails.
- [x] `git diff --stat master... -- core/src/main engine/src/main lwjgl3/src/main` is empty.
- [x] `./gradlew engine:test` passes.
- [x] `./gradlew core:test` passes.
- [x] `./gradlew lwjgl3:jar` builds.
- [x] The CI run of the pushed branch passes and shows `core:smokeTest`. This needs the owner's approval to push.

## Fault checks

Each check proves that the suite fails when it must. Make each fault as a temporary local edit, run
`./gradlew core:smokeTest`, read the failure, and revert the edit. Do not commit a fault. Write each result in the
process log.

- [x] A bad region in a class that throws: in `Met.kt`, change one region name. The test for `Met` fails and names the
  class.
- [x] A silent miss: in `GravityBlock.kt`, make the `findRegion` key wrong. `init()` does not throw, and the test for
  `GravityBlock` still fails and names the region. (`Coin` uses `region!!`, so it throws and is not a silent case.)
- [x] A bad atlas: in `TextureAsset.kt`, change one source file name. The suite fails with the file name.
- [x] A missing PNG: rename one page file in `assets/sprites/sprite_sheets/`. The suite fails with the file name.
- [x] A bad constructor: add a second required parameter to `Coin`. The test for that class fails.
- [x] A new failure: remove `JetMet` from `known-entity-failures.txt`. The test for `JetMet` fails.
- [x] A stale entry: add `Met` to `known-entity-failures.txt`. The test for `Met` fails and says to remove the entry.

## Manual playthrough

None. The branch changes no game code, so the owner has no play test.

## Behaviors to check

None beyond the checks above.
