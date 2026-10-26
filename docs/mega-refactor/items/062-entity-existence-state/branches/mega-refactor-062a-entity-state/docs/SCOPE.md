# Scope: mega-refactor-062a-entity-state

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-26

## Goal

The engine is the only writer of one existence state on each entity, on every path. `spawned` and `dead` become
read-only values derived from that state, so they can no longer disagree. The callers do not change in this branch.

## Item

`062`, "Proposed scope", first bullet:

> `062a`: an engine `EntityState { UNSPAWNED, SPAWN_QUEUED, LIVE, DESTROY_QUEUED }` on each entity, written only by
> the engine (`internal set`) in every path (`spawn`, `spawnNow`, `destroy`, `destroyNow`, `reset`, `dispose`).
> `spawned` and `dead` become getters derived from it, with today's values on every path except the
> `reset`/`dispose` fix. The `dead` writes in `MegamanMaverickGame` go. Odd transitions keep today's behavior and are
> logged. Engine tests for each transition.

The owner's decision of 2026-10-26: the engine-owned state field (option (b) in the item `NOTES.md`).

## Current state

Based on `master` at `cd046dade`. `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

- **`spawned`** (`E/entities/IGameEntity.kt:15`, a public `var`; `GameEntity.kt:23`): the engine sets it to `true`
  after `onSpawn` (`E/GameEngine.kt:77`) and to `false` after `onDestroy` (`:100`). The engine reads it in
  `reset()`/`dispose()` (`:132`, `:146`). About 14 game readers (`ReactorMan`, `WilyFinalBoss`, `QuestionBlock`,
  `FreezableEntityHandler`, `WilyCapsuleTentacle`, `LaserBeamer`, the pool check in `C/MegamanMaverickGame.kt:933`,
  debug text). Two tests write it (`PlayersTest`, `MegaPlayersTest`); both entities are never spawned, so the write
  does nothing.
- **`dead`** (`C/entities/contracts/MegaGameEntity.kt:28`, `var dead = true`): written only by the engine callbacks
  `onQueueToSpawn` (→ `false`) and `onQueueToDestroy` (→ `true`) in `C/MegamanMaverickGame.kt:927-928`. These two
  callbacks have no other user. So `dead` means "the last queue call was a destroy, or the entity was never queued".
  About 99 reads in 65 files; the item `NOTES.md` sorts them by question.
- **Only one implementation:** `GameEntity` is the only class that implements `IGameEntity` (no test mocks it).
- **Name clash:** 55 files declare a member named `state` (enemy and boss state machines), so the new property cannot
  be called `state`. No file declares `existenceState` or `EntityExistenceState`.
- **The engine order:** `update()` applies all queued spawns first, then all queued destroys
  (`E/GameEngine.kt:110-118`). A spawn call is refused only when the entity is already in the spawn queue; a destroy
  call only when it is already in the kill queue.

### The four states and the two old flags

The four states are the four combinations of two facts: "applied now" (in the engine) and "will be live after the
next update". `spawned` = applied now; `!dead` = will be live.

| State | Applied now | Will be live | `spawned` | `dead` |
|---|---|---|---|---|
| `UNSPAWNED` | no | no | `false` | `true` |
| `SPAWN_QUEUED` | no | yes | `false` | `false` |
| `LIVE` | yes | yes | `true` | `false` |
| `DESTROY_QUEUED` | yes | no | `true` | `true` |

"Will be live" follows the engine order: an entity with a queued destroy is not live after the update, also when a
spawn is queued too.

### Where the derived values differ from today

On all normal paths (spawn, update, destroy, update; a rejected spawn; a double destroy; a spawn and a destroy of an
unspawned entity in one frame; a spawn of a live entity; inside `onSpawn` and `onDestroy`) the derived values are
the same as today. They differ on two paths, and on both today's `dead` is `false` for an entity that is not in the
engine after the update:

1. **`engine.reset()` and `dispose()`** (`E/GameEngine.kt:127-153`) call `destroyNow` directly and clear the queues,
   so they never write `dead`. Today, after a level ends, Megaman and every entity that was live or queued to spawn
   keep `dead = false`. With the state, they are `UNSPAWNED` (`dead = true`).
   - Effect: Megaman is `dead = true` from the end of a level until his next spawn is queued. Today that is so only
     before the first level start (`dead` starts `true`). In that window, `MegaLevelScreen` reads `player.dead`
     (`:927` no pause while dead, `:933` force resume while paused and dead, `:962`, `:1058` draw the death fade).
     So the second and later level starts behave as the first one does today.
2. **A spawn call while a destroy is queued** (a live entity is destroyed and spawned again in one frame, or an
   unspawned entity gets a destroy call and then a spawn call). Today the spawn call sets `dead = false`, but the
   update runs `onSpawn` and then destroys the entity, so it ends out of the engine with `dead = false`. With the
   state, it stays `dead = true`, which is true.
   - Effect: `FreezableEntityHandler.setFrozen` (`C/entities/utils/FreezableEntityHandler.kt:57-67`) spawns the ice
     shield only if `shieldEntity.dead`. If an enemy is unfrozen and frozen again in one frame, today the shield ends
     destroyed with `dead = false`, so every later freeze of that enemy has no shield. With the state, the next freeze
     spawns the shield. [inferred from the code; the core test in "In scope" item 6 proves it on the baseline first]

An exact copy of today's `dead` on path 2 would need a second engine field ("the last queue call"), which is today's
flag moved into the engine. The owner chose the true value (see "Decisions").

## In scope

1. **Engine state:** `enum class EntityExistenceState { UNSPAWNED, SPAWN_QUEUED, LIVE, DESTROY_QUEUED }` in
   `E/entities/`. `IGameEntity` gets `val existenceState: EntityExistenceState`; only the engine writes it (the exact way to
   keep `core` from writing is a plan detail, for example an `internal` setter on `GameEntity`).
2. **The engine writes the state** on every path, with the rules of the table above:
   - `spawn` (accepted): `UNSPAWNED` → `SPAWN_QUEUED`, unless a destroy is queued (it stays `UNSPAWNED`); `LIVE` and
     `DESTROY_QUEUED` stay.
   - `destroy` (accepted): `LIVE` → `DESTROY_QUEUED`; `SPAWN_QUEUED` → `UNSPAWNED`; `UNSPAWNED` stays.
   - `spawnNow`, after `onSpawn` (where `spawned` is set today): `LIVE`, or `DESTROY_QUEUED` if a destroy is queued.
   - `destroyNow`, after `onDestroy` (where `spawned` is cleared today): `UNSPAWNED`.
   - `reset()`/`dispose()`: every entity in the engine or in a queue ends `UNSPAWNED`.
3. **Derived reads:** `spawned` becomes a read-only value on `IGameEntity` (`LIVE` or `DESTROY_QUEUED`), and `dead`
   on `MegaGameEntity` becomes read-only (`UNSPAWNED` or `DESTROY_QUEUED`). Every reader keeps its code.
4. **Removals:** the `dead` writes and the `onQueueToSpawn`/`onQueueToDestroy` callbacks (no other user); the two
   test writes of `spawned`.
5. **Odd paths keep today's engine actions** (the same `onSpawn`/`onDestroy` calls in the same order). The engine
   logs them with `GameLogger.debug` under a new tag `GameEngine` (the engine has no tag today): a spawn call while a
   destroy is queued, a spawn call on a live entity, a destroy call on an unspawned entity. The log is the evidence
   for the guards in `062b`.
6. **Tests.** The engine's real behavior is proved by tests, not by guesses or by play only (owner's request,
   2026-10-26).
   - **Temporary contract suite** (engine test sources; deleted in the last commit before the merge):
     `TempOldGameEngine`, a frozen copy of today's `GameEngine` with the `dead` callbacks modelled as in
     `MegamanMaverickGame`, and `TempOldEngineContractTest`. It runs the old and the new engine side by side on the
     same seeded random sequences of calls over several entities: `spawn`, `destroy`, `update`, `reset`, `dispose`,
     a refused `canSpawn`, and `spawn`/`destroy` calls made from inside `onSpawn` and `onDestroy` (as entities that
     spawn children do). After each call it asserts:
     - the same ordered log of engine actions (`init`, component resets, `onSpawn`, `onDestroy`,
       `onSpawnRejected`, the system adds and removes) and the same return values;
     - the same `spawned` on every entity;
     - the same `dead`, except where the old `dead` is `false` for an entity that is out of the engine after the
       next update (the two intended changes). The test finds those cases by its own rule, not by a list, and checks
       that the new `dead` is the true value there.
   - **Permanent engine tests:** one test per transition in item 2; the odd paths (the engine actions are as today,
     the state is true, the log line is written); `reset()` and `dispose()` with live and queued entities; an
     invariant test on seeded random sequences: `spawned` is "in the engine", and `dead` is "not in the engine after
     the next update".
   - **Permanent core tests for the intended changes**, with a real `GameEngine`, written first and run on the
     baseline to show today's behavior (they must fail there, or the scope's reading is wrong):
     - an entity that was live is `dead` after `engine.reset()`;
     - the ice shield: an entity with a `FreezableEntityHandler` is unfrozen and frozen again in one frame; at the
       next freeze, the shield spawns.

New APIs and their first callers:

| API | First callers |
|---|---|
| `EntityExistenceState`, `IGameEntity.existenceState` | the derived `spawned` (engine) and `dead` (core) |

## Out of scope

- Moving the readers to named questions (`062c`; the helper names are item open question 2).
- Removing `spawned`, `dead` and `engine.contains(..., containedIfQueuedToSpawn)` (`062d`). The pool check at
  `C/MegamanMaverickGame.kt:933` stays as it is: `UNSPAWNED` also covers "spawn and destroy queued", where today's
  check does not free the entity.
- Changing what the engine does on the odd paths (item open questions 4 and 5): a spawn while a destroy is queued, a
  spawn of a live entity, a destroy of an unspawned entity. A new branch after this one adds the guards, with this
  branch's log as evidence (see "Decisions").
- The pooled-reuse (ABA) risk and a generation (item open question 6; after `019b`).
- The pool leak of entities queued to spawn at `reset()` (they are never freed to their pool).

## Intended changes

Each one makes `dead` the true value where today's value is wrong (see "Where the derived values differ from
today"):

1. After `engine.reset()`/`dispose()`, entities are `dead`. Megaman is `dead` between a level end and his next spawn
   at every level start, as at the first level start today.
   - Benefits: `dead` agrees with the engine for every entity after a level ends, so `dead` and `spawned` agree on
     every normal path. An object that lives longer than a level and holds an entity (Megaman, his handlers, a
     companion-object holder) sees an entity of the old level as `dead`, not as alive. Every level start is the same
     as the first one, so a level start needs only one check, not "the first" and "a later" one.
   - Consequences: in the window between the level end and Megaman's next queued spawn, `player.dead` is `true`, so
     `MegaLevelScreen` blocks the pause (`:927`), would force a resume if the game were paused (`:933`), does not
     call `playerDeathEventHandler.setInactive()` (`:962`) and draws the death fade (`:1058`, a fade set to its end).
     The first level start already runs with these values, so no new visible effect is expected [inferred]. The other
     entities of the old level are out of the engine; a reader that skips them when `dead` now skips them, and a
     reader that acts on them only when not `dead` no longer acts. The contract checks the second level start.
2. A spawn call while a destroy is queued leaves the entity `dead`; an ice shield destroyed and spawned in one frame
   no longer blocks later shields.
3. (Added 2026-10-26, found by the temporary suite in step 4.) If, in the destroy phase of an update, an entity gets a
   spawn call and then a destroy call, the destroy is applied in the same phase and the spawn at the next update, so
   the entity will be live. Today `dead` is `true` (the last call was a destroy); with the state it is `false`. A
   holder that checks `dead` no longer drops an entity that comes back at the next update. Rare: 61 times in 10,000
   random sequences; no real caller is known.

## Affected areas and risks

- **Files:** `E/GameEngine.kt`, `E/entities/IGameEntity.kt`, `GameEntity.kt`, a new `EntityExistenceState.kt`,
  `C/entities/contracts/MegaGameEntity.kt`, `C/MegamanMaverickGame.kt`, the engine tests, two test lines.
- **The derive rules:** a wrong rule changes `dead` for about 99 readers. Mitigation: the temporary contract suite
  compares old and new on random call sequences; the permanent tests hold the rules after the merge.
- **The level start window** (intended change 1): Megaman's `dead` is `true` for a short time at later level starts.
  Check: start a level, finish or leave it, start another, and compare with the first start.
- **Performance:** a read becomes a compare of one field (the same cost). A spawn or destroy call adds one set lookup
  (is a destroy queued?).

## Overlap with other branches

No other branch is active. `019b` (planned) changes the spawn and destroy paths too; this branch goes first, and
`019b` can use the state.

## Decisions

- 2026-10-26 (owner): the `reset`/`dispose` fix is an intended change of this branch, with its benefits and
  consequences written down (question 3); the pooled-reuse check waits for `019b`, and this branch goes before `019b`
  (question 5).
- 2026-10-26 (owner, "I prefer to be too verbose than not enough"): the type is `EntityExistenceState`
  (question 1). The property is `existenceState`, because it is read on an entity (`entity.existenceState`).
- 2026-10-26 (owner, "Yes, true value"): on a spawn call while a destroy is queued, `dead` stays `true`; an intended
  change (question 2).
- 2026-10-26 (owner, "Guards in new branch"): this branch keeps the engine's actions on the odd paths and logs them
  (question 4). The owner's play test runs with `--logTags` set to the engine's tag, so the log shows which callers
  hit each path. A new branch after this one adds the guards: reject a spawn of a live entity, apply a same-tick
  respawn after the destroy (the last call wins), reject a destroy of an unspawned entity. It becomes `062b`; the
  readers move to `062c` and the removals to `062d`.
- 2026-10-26 (owner): prove the engine's behavior with tests, temporary and permanent, not with guesses or play
  only: a temporary old-versus-new contract suite, permanent engine tests, and core tests for the intended changes
  that run on the baseline first ("In scope" item 6).
- 2026-10-26 (owner, "yes"): intended change 3 (a spawn and then a destroy in the destroy phase of an update leave
  `dead` `false`); the rule is "the new `dead` is always the true value", checked by the temporary suite at every
  update.

## Open questions

- None.
