# Plan: mega-refactor-084g-movement-state

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-11-01

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `M/` = `C/entities/megaman/`. Line numbers are at `c6b8cecf7`.

Each step is one commit. Each step ends with `./gradlew engine:test core:test lwjgl3:jar`, plus the check that the step names. Where a step changes a rule or adds an API, the test comes first, and it must fail for the expected reason before the code changes. Stage files by path, and read `git status --short` before each commit.

## Steps

### 1. Run tests on the old code

- Changes: make `runLeft` and `runRight` `internal`, so a test can call them. New `MegamanRunTest`: the velocity, the facing and `running` for each of the 4 directions and both sides, the 3 `runTime` scalars, the air case, each guard condition, the speed limit, and the `RODENT_CLAWS` difference. No other production change.
- Check: the tests pass on the old code.
- Done: yes

### 2. `RunComponent`

- Changes: tests first: `RunComponent.reset()`, `running` out of play, `running` with no component, and a room transition end that stops a player that is not `Megaman`. Then add `C/entities/state/RunComponent.kt` and `Runs.kt`. `Megaman.init()` adds the component. The controller, `MegamanWeaponsHandler.kt`, `MegamanAnimationKeySupplier.kt` and `:530` use the query. `LevelRoomHandler.kt:155` writes the query with no cast. Delete `runningFlag`, `runTime` and the 2 `onSpawn` lines. `MegamanRunTest` adds a `RunComponent` to its fixture.
- Check: 15 casts to `Megaman` remain. `MegamanRunTest` passes with no change to its expected values.
- Done: yes

### 3. One run function

- Changes: `run(delta, side)` replaces `runLeft` and `runRight`, with the `RODENT_CLAWS` difference as a check of `side` and a `TODO` that names the issue file `docs/issues/2026-11-01-rodent-claws-run-facing.md`. Delete the dead wall-slide factor. The actuators call it at the same place. `MegamanRunTest` calls the new function.
- Check: `MegamanRunTest` passes with no change to its expected values.
- Done: yes

### 4. Crouch timers

- Changes: tests first, on the old code: a crouch starts only after both delays end, and a release of `DOWN` or a ground slide restarts its delay. Then the 2 timers become fields of the object in `crouchBehavior()`.
- Check: the crouch tests pass with no change to their expected values.
- Done: yes

### 5. Typed values and the `canMove` `TODO`

- Changes: `currentAnimKey` and `airDashDirection` become fields on `Megaman`. Delete `MegamanKeys.kt` and `ConstKeys.ANIMATION_KEY`. Add the `TODO` at `canMove`.
- Check: the old-names `grep` in `CONTRACT.md`. A test: the air dash writes `airDashDirection` once, and `getSpriteDirection()` keeps it after a direction change.
- Done: yes

### 6. Final checks

- Changes: none in code, unless a check fails.
- Check: all automated checks in `CONTRACT.md`. Review the full diff against the "Frame order" section. Build, then `timeout 12 ./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`. Compare the diff with `master`, and merge `master` first if it changed the same files (`051e` is the known overlap). Then the owner's play test.
- Done: yes
