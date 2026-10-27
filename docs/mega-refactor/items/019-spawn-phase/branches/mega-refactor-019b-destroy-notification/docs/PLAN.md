# Plan: mega-refactor-019b-destroy-notification

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-27

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `ET/` = `engine/src/test/kotlin/com/maverick/game/engine/`,
`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `CT/` = `core/src/test/kotlin/com/megaman/maverick/game/`.

## Design details

- **Engine hooks.** `IGameEntity` gets `fun onSpawnQueued() {}` and `fun onDestroyQueued() {}`. `GameEngine` calls
  them inside `spawn()`/`destroy()`, after it writes the new state, in exactly the accepting cases of scope items 1 and
  7: a call that changes "live after the next update" (`onSpawnQueued`: no to yes; `onDestroyQueued`: yes to no).
- **Game side.** `MegaGameEntity` gets `runnablesOnDestroyQueued` (an `OrderedMap`, like `runnablesOnDestroy`) and
  overrides `onDestroyQueued()` to run it. A runnable may remove itself, so the override copies the values into a
  reusable array on the entity before it runs them (no allocation per call).
- **Spawner.** `Spawner` gets a private `release()` that clears the held spawn and removes its runnable
  (`ConstKeys.SPAWNER`, an existing key if there is one, else a new key) from the entity's `runnablesOnDestroyQueued`.
  When `test()` makes a spawn, it puts a runnable under that key that calls `release()` and then `onReleased()`.
  `onSpawnRejected()` and `reset()` call `release()`. The `dead` poll in `test()` goes. `onReleased()` is an empty
  open method; `SpawnerForBoundsEntered` overrides it.
- **Bounds spawner.** `SpawnerForBoundsEntered.onReleased()` sets `isEntered = thisBounds().overlaps(otherBounds())`
  at the moment of the destroy call. So an entity that dies off screen leaves `isEntered` false, and its area spawns
  it when it enters the camera (`TODO` case 2); an entity that dies on screen leaves it true, so it respawns only after
  the area leaves the camera and comes back (as today).
- **Shared camera bounds.** `MegaLevelScreen` owns one `GameRectangle` and sets it from
  `gameCamera.getRotatedBounds()` once per frame, before `spawnsMan.update`. `SpawnerFactory.spawnerForWhenInCamera`
  takes that rectangle instead of the camera, and `SpawnersLayerBuilder` passes it (through the builder parameters).
  `onReleased()` reads the same rectangle, so it sees the camera of the frame's spawn pass.
- **Frozen copy** for the temporary suite: today's `GameEngine` (after `062b`) as `TempOldGameEngine`. The suite
  logs the hook calls of the new engine in a separate list, so the compared action logs are the same.

New APIs and their first callers:

| API | First callers |
|---|---|
| `IGameEntity.onDestroyQueued()` | `GameEngine.destroy` (calls); `MegaGameEntity` (overrides) |
| `IGameEntity.onSpawnQueued()` | `GameEngine.spawn` (calls); no override (owner's decision) |
| `MegaGameEntity.runnablesOnDestroyQueued` | `Spawner` (its release runnable) |
| `Spawner.onReleased()` | `Spawner` (calls); `SpawnerForBoundsEntered` (overrides) |
| The shared spawn camera rectangle | `MegaLevelScreen` (writes); bounds spawners (read) |

## Steps

### 1. Baseline

- Changes: none. Clean build on the branch (after the `master` merge).
- Check: the test counts and the sorted set of compiler warnings, in the scratchpad and the process log.
- Done: yes

### 2. Core spawner tests, on the baseline

- Changes: `CT/spawns/SpawnerReleaseTest.kt`, with a real `GameEngine`, a relaxed MockK game (as
  `EntityExistenceTest`), test entities, `SpawnerForEvent` and `SpawnerForBoundsEntered` built with their
  constructors (they exist on the baseline), and a helper that runs one frame in the level screen's order (the spawn
  pass, the engine spawns of the pass, `engine.update`, then the events). Cases: the three intended changes of the
  contract (`TODO` case 1, `TODO` case 2, pooled reuse), and the unchanged cases (a rejected spawn frees the spawner;
  `reset()` frees it; an entity killed on screen respawns only after its area leaves and enters the camera; an event
  spawner holds its spawn while its entity lives). Not committed in this step.
- Check: on the baseline the three intended-change tests fail with the predicted values and the others pass. The
  output goes in the process log. If a test does not behave as predicted, stop and tell the owner.
- Done: yes

### 3. Temporary contract suite, on the baseline

- Changes: `ET/TempOldGameEngine.kt` (the frozen copy) and `ET/TempOldEngineContractTest.kt`: 10,000 seeded sequences
  with nested calls (the `062b` call mix); after every call the action logs, return values and states of both
  engines must be equal.
- Check: passes on the baseline; a deliberate change to the frozen copy makes it fail (undone). Commit the suite.
- Done: yes

### 4. The engine hooks

- Changes: `E/entities/IGameEntity.kt`, `E/GameEngine.kt` (the two hooks). Permanent tests in
  `ET/entities/EntityExistenceHooksTest.kt` (scope item 9: each accepting case once with its new state, no call on
  the other cases, and the random-sequence rule "a hook runs exactly when a call changes live after the next update").
- Check: build; all tests pass; the temporary suite passes (the hooks change no action, return value or state).
- Done: yes

### 5. The spawners

- Changes: `C/entities/contracts/MegaGameEntity.kt` (`runnablesOnDestroyQueued`, the override), `C/spawns/Spawner.kt`,
  `SpawnerForBoundsEntered.kt`, `SpawnerFactory.kt`, `SpawnersLayerBuilder.kt` (and the builder parameters),
  `C/screens/levels/MegaLevelScreen.kt` (the shared rectangle; the `TODO` at `:938-942` goes). The core tests of
  step 2 are committed with this step.
- Check: build; all tests pass; the step 2 tests pass; the greps of the contract.
- Done: yes

### 6. Contract checks

- Changes: none, or fixes.
- Check: every automated check in `docs/CONTRACT.md` on a clean build (warnings compared with step 1,
  `contact-rule-selection.txt` unchanged). Short launches of the contract rooms with no exception. Results in the
  process log; then the manual checks for the owner.
- Done: yes

### 7. Delete the temporary suite; merge

- Changes: delete `TempOldGameEngine.kt` and `TempOldEngineContractTest.kt` in the last commit before the merge.
- Check: the owner's play test passes; the build and the tests pass again; the owner approves the merge; the merge
  steps.
- Done: yes
