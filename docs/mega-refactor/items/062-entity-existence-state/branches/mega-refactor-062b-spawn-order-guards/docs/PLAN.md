# Plan: mega-refactor-062b-spawn-order-guards

How to do the work in `SCOPE.md`. Write this file after `SCOPE.md` and `CONTRACT.md` are
approved. The user approves it. When the plan changes, write the reason in the process log.

- Status: approved
- Approved: 2026-10-27

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `ET/` = `engine/src/test/kotlin/com/maverick/game/engine/`,
`CT/` = `core/src/test/kotlin/com/megaman/maverick/game/`.

## Design details

- **`spawn()`** (after `canSpawn`, which keeps its place and its `onSpawnRejected`):
  - `LIVE`: refused (`false`, a log line, no `onSpawnRejected`).
  - `DESTROY_QUEUED` with the destroy still queued: a respawn. The entity and its properties go into a new respawn
    queue; the state becomes `RESPAWN_QUEUED`; `true`.
  - `DESTROY_QUEUED` while its `onDestroy` runs (the destroy is no longer in the queue): as today, a normal spawn for
    the next update.
  - `SPAWN_QUEUED`, `RESPAWN_QUEUED`: already queued, `false` (as today for a queued spawn).
  - `UNSPAWNED`: queued as today (`SPAWN_QUEUED`, or `UNSPAWNED` when a destroy waits for this spawn).
- **`destroy()`**:
  - `LIVE`: as today (`DESTROY_QUEUED`).
  - `SPAWN_QUEUED`: as today (`UNSPAWNED`, the destroy queued); the update runs it after the spawn (below).
  - `RESPAWN_QUEUED`: the respawn is cancelled, the destroy stays (`DESTROY_QUEUED`, `true`): the last call wins.
  - `UNSPAWNED` with no spawn queued, and `DESTROY_QUEUED` while its `onDestroy` runs: refused (`false`, a log line).
- **`update()`**: the spawn phase as today; the destroy phase as today, except that a destroy of an entity whose spawn
  is still queued (path D) waits for the next update; then a new respawn phase runs `spawnNow` for each queued
  respawn. A spawn or destroy call made during the respawn phase waits for the next update.
- **The state after `destroyNow`**: `RESPAWN_QUEUED` if a respawn waits, `SPAWN_QUEUED` if a spawn waits and no
  destroy waits, else `UNSPAWNED`.
- **`reset()`/`dispose()`**: clear the respawn queue before they destroy the live entities; everything ends
  `UNSPAWNED`, as today.
- **`spawned`** includes `RESPAWN_QUEUED`; `dead` (core) is already `false` for it.
- **The frozen copy** for the temporary suite is today's `GameEngine` (after `062a`), renamed `TempOldGameEngine`. It
  writes the state of its own entities (the test sources are in the engine module, so `internal` is visible) and
  gets read-only queries for the suite's classification (queue membership, "in the destroy phase", "in `onDestroy`
  of this entity").
- **The oracle** of the temporary suite and of the permanent random test: for each entity, the log alternates
  `onSpawn`/`onDestroy` and starts with `onSpawn`; after each update, an entity whose last call before the update was
  an accepted `spawn` (or, with no call, that was live before) is live, and one whose last call was a `destroy` is
  not; entities touched during the update are skipped for that update.

New APIs and their first callers:

| API | First callers |
|---|---|
| `EntityExistenceState.RESPAWN_QUEUED` | `GameEngine.spawn` (rule 2), `IGameEntity.spawned` |

## Steps

### 1. Baseline

- Changes: none. Clean build on the branch start.
- Check: the test counts and the sorted set of compiler warnings, in the scratchpad and the process log.
- Done: yes

### 2. Core tests for the intended changes, on the baseline

- Changes: `CT/entities/EntityExistenceTest.kt` gets two tests: (a) the ice shield: a thaw and a freeze in one frame,
  then an update: the shield is live (`spawned`); (b) a parent whose `onDestroy` destroys its child; the child and the
  parent get a destroy in the same frame; after the update, the child's destroy runnables ran once. Not committed in
  this step.
- Check: both fail on the baseline with the predicted values (shield not spawned; the child's runnable ran twice).
  The failure output goes in the process log. If one passes, stop and tell the owner.
- Done: yes

### 3. Temporary contract suite, on the baseline

- Changes: `ET/TempOldGameEngine.kt` (the frozen copy) and `ET/TempOldEngineContractTest.kt` with the
  classification of the four odd paths and the oracle. 10,000 seeded sequences.
- Check: on the baseline the two engines are the same code, so the suite passes with every step compared (no switch
  to the oracle needed), and it reports how often each odd path occurred (each at least once). The oracle is run on
  the baseline too and must fail (today's engine breaks "the last call wins" on paths B and D, and the alternation on
  paths A and C), so it is shown to catch them. A deliberate change to the frozen copy makes the comparison fail; the
  change is undone. Commit the suite.
- Done: yes

### 4. The guards

- Changes: `E/entities/EntityExistenceState.kt` (`RESPAWN_QUEUED`), `IGameEntity.kt` (`spawned`), `E/GameEngine.kt`
  (the rules in "Design details", the log lines). Permanent tests in `ET/entities/EntityExistenceStateTest.kt`: the
  tests of the odd paths change to the new rules; new tests per rule, for `RESPAWN_QUEUED`, for a destroy that cancels
  a respawn, for a destroy inside `onSpawn`, and the random test checks the oracle. The core tests of step 2 are
  committed with this step.
- Check: build; all tests pass; the temporary suite passes (same results until the first odd call, the oracle after
  it, each odd path counted); the core tests of step 2 pass.
- Done: yes

### 5. Contract checks

- Changes: none, or fixes.
- Check: every automated check in `docs/CONTRACT.md` on a clean build (warnings compared with step 1,
  `contact-rule-selection.txt` unchanged). Short launches of the contract rooms with no exception. Results in the
  process log; then the manual checks for the owner.
- Done: yes

### 6. The owner's play test and the log

- Changes: the `GameEngine` log lines from the owner's `logs-062b-*.txt` files go in the branch `docs/NOTES.md`,
  grouped by path and caller; the files move to the scratchpad.
- Check: the owner's play test passes.
- Done: yes

### 7. Delete the temporary suite; merge

- Changes: delete `TempOldGameEngine.kt` and `TempOldEngineContractTest.kt` in the last commit before the merge.
- Check: the build and the tests pass again; the owner approves the merge; the merge steps.
- Done: yes
