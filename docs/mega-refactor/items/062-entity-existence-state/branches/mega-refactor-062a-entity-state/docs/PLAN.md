# Plan: mega-refactor-062a-entity-state

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-26

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `ET/` = `engine/src/test/kotlin/com/maverick/game/engine/`,
`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `CT/` = `core/src/test/kotlin/com/megaman/maverick/game/`.

## Design details

- **Who writes the state.** Kotlin does not allow `internal` on a member of an interface (checked with the compiler:
  "Modifier 'internal' is not applicable inside 'interface'"). So `IGameEntity` declares `val existenceState`, and
  `GameEntity` implements it as `final override var existenceState = UNSPAWNED` with an `internal set`. `GameEngine`
  writes it through one private function that casts to `GameEntity`; `GameEntity` is the only implementation of
  `IGameEntity`, and another implementation fails at once with a clear message. `core` is another module, so it
  cannot write the state.
- **Derived reads.** `IGameEntity.spawned` becomes `val spawned: Boolean get() = existenceState == LIVE ||
  existenceState == DESTROY_QUEUED` (no class overrides it today). `MegaGameEntity.dead` becomes
  `val dead: Boolean get() = existenceState == UNSPAWNED || existenceState == DESTROY_QUEUED`.
- **"Is a destroy queued?"** `entitiesToKill.contains(entity)` (a set lookup), only in `spawn` and `spawnNow`.
- **The log.** `GameEngine` gets `companion object { const val TAG = "GameEngine" }` and three `GameLogger.debug` lines
  for the odd paths.
- **The frozen copy.** `TempOldGameEngine` is today's `GameEngine` with one change: after the branch, `spawned` is not
  writable, so the copy keeps its own `spawned` per entity in an identity map. It keeps the `onQueueToSpawn` and
  `onQueueToDestroy` callbacks, and the test models today's `dead` with them, as `MegamanMaverickGame` does.
- **The two worlds.** The temporary suite builds two sets of the same test entities, one per engine. Each entity logs
  `init`, `onSpawn`, `onDestroy` and its component's `reset` to its world's log; a test system (a `GameSystem` in the
  engine test sources, which can override the `internal` `process`) logs the entities it processes in each update.
  The random calls, and the calls that an entity makes from inside `onSpawn`/`onDestroy`, come from a seeded plan
  that both worlds read in the same order.
- **The truth check.** When the new `dead` differs from the old one, the suite asserts that the old value is `false`
  and the new one `true`, and marks the entity. At the next `update`, if no call touched that entity in between, it
  asserts that the entity is not spawned after the update. The permanent invariant test uses the same rule on the new
  engine alone, for every entity and every call.

New and removed APIs:

| API | First callers |
|---|---|
| `EntityExistenceState`, `IGameEntity.existenceState` | `IGameEntity.spawned`, `MegaGameEntity.dead` |
| `GameEngine.TAG` | the three log lines in `GameEngine` |
| Removed: `GameEngine` `onQueueToSpawn`, `onQueueToDestroy` | (their only caller was the `dead` writes) |

## Steps

### 1. Baseline

- Changes: none. Clean build on the branch start (`./gradlew clean engine:test core:test lwjgl3:jar`).
- Check: the test counts and the sorted set of compiler warnings, saved in the scratchpad and written in the process
  log.
- Done: yes

### 2. Core tests for the intended changes, on the baseline

- Changes: `CT/entities/EntityExistenceTest.kt` with a real `GameEngine` and a relaxed MockK game (as
  `MegaPlayersTest` does): (a) an entity that was live is `dead` after `engine.reset()`; (b) the ice shield: a test
  entity with a `FreezableEntityHandler` is frozen, unfrozen and frozen again in one frame; after the update, a later
  freeze spawns the shield (`shieldEntity` is reached through the handler's public behavior; if the test needs a
  read of the private field, it uses reflection in the test only). Not committed in this step.
- Check: both tests fail on the baseline, each with the value the scope predicts. The failure output goes in the
  process log. If a test passes on the baseline, stop and tell the owner (the scope's reading is wrong). If the ice
  shield test cannot be built with mocks, tell the owner before going on.
- Done: yes

### 3. Temporary contract suite, on the baseline

- Changes: `ET/TempOldGameEngine.kt` (the frozen copy) and `ET/TempOldEngineContractTest.kt`, as in "Design
  details". 10,000 seeded sequences of 20 to 60 calls over 4 to 8 entities; calls: `spawn`, `destroy`, `update`,
  `reset`, `dispose` (at the end of a sequence only), a spawn refused by `canSpawn`, and calls made from inside
  `onSpawn`/`onDestroy`. The suite prints how often each odd path and each kind of `dead` difference occurred.
- Check: on the baseline the suite passes with 0 `dead` differences (both engines are the same code), and each odd
  path occurs at least once. A deliberate one-line change to the frozen copy (for example, destroys before spawns)
  makes the suite fail; the change is undone before the commit. Commit the suite.
- Done: yes

### 4. The engine state

- Changes:
  - `E/entities/EntityExistenceState.kt` (the enum); `IGameEntity`: `val existenceState`, `spawned` derived;
    `GameEntity`: the `internal set` field, the `spawned` field goes.
  - `E/GameEngine.kt`: the writes on every path (scope "In scope" item 2), `reset()`/`dispose()` set every entity in
    the engine or in a queue to `UNSPAWNED`, the `TAG` and the three log lines; `onQueueToSpawn` and
    `onQueueToDestroy` go.
  - `C/entities/contracts/MegaGameEntity.kt`: `dead` derived; `C/MegamanMaverickGame.kt:927-928`: the callbacks go.
  - The two test writes of `spawned` go (`PlayersTest`, `MegaPlayersTest`).
  - Permanent tests: `ET/entities/EntityExistenceStateTest.kt` (one test per transition, the odd paths with the
    actions, the state and the log line, `reset()`/`dispose()` with live and queued entities, the invariant test on
    seeded random sequences); the core tests of step 2 are committed with this step.
- Check: the build passes; all tests pass; the temporary suite passes, with both kinds of intended difference
  counted at least once and no other difference; the core tests of step 2 now pass.
- Done: yes

### 5. Contract checks

- Changes: none, or fixes.
- Check: every automated check in `docs/CONTRACT.md` on a clean build (warnings compared with step 1; the greps; a
  core write of `existenceState` does not compile, tried once and undone; `contact-rule-selection.txt` unchanged).
  Short launches of the contract rooms with no exception. Results in the process log; then the manual checks for the
  owner.
- Done: yes

### 6. The owner's play test and the log

- Changes: the log lines of the odd paths from the owner's `logs.txt`, grouped by path and caller, go in the branch
  `docs/NOTES.md` as the evidence for `062b`.
- Check: the owner's play test passes.
- Done: yes

### 7. Delete the temporary suite; merge

- Changes: delete `TempOldGameEngine.kt` and `TempOldEngineContractTest.kt` in the last commit before the merge.
- Check: the build and the tests pass again without them; the owner approves the merge; the merge steps in
  `docs/mega-refactor/CLAUDE.md`.
- Done: yes
