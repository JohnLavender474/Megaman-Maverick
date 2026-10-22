# Contract: mega-refactor-002a-ci-all-branches

The gameplay that this branch must not change, and how to check it. Do the checks before the
branch merges, and write the result in the branch process log.

- Status: approved
- Approved: 2026-10-22

This branch changes only the build, the test libraries and CI. The game code does not change, so
the main proof is that the build and the tests give the same result as `master`.

## Baseline

Take it on the branch before any change, with the command below and Corretto JDK 17. Expected
from the `001a` merge: `engine:test` 427 tests and `core:test` 49 tests, all pass; the JAR builds.
Write the exact numbers in `NOTES.md`.

## Automated checks

```
JAVA_HOME=$HOME/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar --console=plain
```

- [x] With JDK 17: the build succeeds; `engine:test` and `core:test` run the same number of tests
  as the baseline, and all pass; the JAR builds.
- [x] With JDK 21 (`JAVA_HOME=$HOME/.jdks/openjdk-21.0.2`): the same command succeeds with the
  same results. Before the change it failed at `:engine:compileTestKotlin`. (Changed from JDK 25
  on 2026-10-22: the owner chose to skip JDK 25, which Gradle 8.10.1 cannot run on.)
- [x] The compiled classes still target Java 17 (class file major version 61), checked with
  `javap -v` on one class from each module.
- [x] No Kotest 4.x or second MockK version is on the test classpath
  (`./gradlew engine:dependencies core:dependencies --configuration testRuntimeClasspath`).
- [x] The `utils/` tests pass locally as in the old `utils-tests.yml` steps.

## CI checks

These need the branch on GitHub. Pushing is the owner's call.

- [x] After a push of this branch, the `ci.yml` run passes on GitHub: the `gradle` job and the
  `utils` jobs (run `39854839528`, 2026-10-22).
- [ ] The run logs show the same test counts as the local run.
- [x] The old workflows (`engine-tests.yml`, `game-tests.yml`, `utils-tests.yml`) no longer run.

## Manual playthrough

| Level | What to check |
|-------|---------------|
| Boot | `./run-game-debug.sh` starts, and the main menu shows. |
| Any level | Start a level and play a few rooms. Nothing changes; this only checks that the build change did not break the run. |

## Behaviors to check

None beyond the playthrough. No game code changes.

## Intended changes

None in gameplay. The build now also works when Gradle runs on JDK 21. JDK 25 still does not
work, because Gradle 8.10.1 cannot run on it
(`docs/issues/2026-10-22-jdk25-test-jvm-target.md` stays open with this note).
