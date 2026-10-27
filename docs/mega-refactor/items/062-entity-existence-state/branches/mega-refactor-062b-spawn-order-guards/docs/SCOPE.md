# Scope: mega-refactor-062b-spawn-order-guards

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-27

## Goal

The engine keeps one rule for each entity: `onSpawn` and `onDestroy` alternate (no second `onSpawn` without an
`onDestroy` between, no `onDestroy` without an `onSpawn` before it), and the last accepted `spawn()`/`destroy()` call decides
whether the entity is live after the update. The engine refuses a call that cannot follow that rule and logs it.

## Item

`062`, decision of 2026-10-26 ("Guards in new branch"):

> A new branch `062b` adds the guards with that log as evidence: reject a spawn of a live entity, apply a same-tick
> respawn after the destroy (the last call wins), reject a destroy of an unspawned entity (questions 4 and 5).

And the owner's request of 2026-10-26: prove the engine's real behavior with tests (temporary and permanent), not
with guesses or play only.

## Current state

Based on `master` at `74b8c5a5c` (after `062a`). `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

- **The engine** (`E/GameEngine.kt`): `spawn()` queues unless the entity is already in the spawn queue; `destroy()`
  queues unless it is already in the kill queue. `update()` runs all queued spawns, then all queued destroys (new
  destroys during that phase run in the same phase; new spawns wait for the next update). Since `062a` the state is
  true on every path, and three odd paths are logged under the `GameEngine` tag.
- **The four odd paths** (what the engine does today):
  - A. `spawn()` of a live entity: `onSpawn` runs again with no `onDestroy` before it.
  - B. `spawn()` while the entity's destroy is queued: the update runs `onSpawn` (again), then `onDestroy`; the entity
    ends destroyed although the last call was a spawn.
  - C. `destroy()` of an unspawned entity (not queued to spawn): the update runs `onDestroy` again.
  - D. In the destroy phase, `spawn(X)` then `destroy(X)` of an unspawned `X`: `onDestroy` runs at once on an entity
    that has not spawned, then `onSpawn` at the next update; `X` ends live although the last call was a destroy
    (`062a` intended change 3 made `dead` tell the truth about it).
- **The play log of `062a`:** six contract rooms with `--logTags GameEngine`: no line (`062a` `docs/NOTES.md`).
- **The callers (static review of all 393 `spawn()` and 319 `destroy()` calls in `core`):**
  - Path A: no caller found. 333 spawns act on an entity fetched from a pool just before; the 52 spawns of a held
    reference also act on an entity fetched fresh in the same function (mostly in `onSpawn`: `block =
    MegaEntityFactory.fetch(...)`, then `block!!.spawn(...)`) or from a supplier that fetches. The level screen
    spawns what the spawners supply (fresh fetches). Megaman is spawned only after a destroy or a reset.
  - Path B: `FreezableEntityHandler.setFrozen` (`C/entities/utils/FreezableEntityHandler.kt:57-67`): a thaw and a
    freeze in one frame (`062a` core test). No other caller found.
  - Path C: 96 `destroy()` calls on held references with no `dead`/`spawned` check, mostly a parent's `onDestroy` that
    destroys all its children (`Bospider`, `PenguinMiniBoss`, `RodentMan`, `GutsTank`, `FireMetSpawner`,
    `TorikoPlundge`, `ElecDevil`, `BigAssMaverickRobot`, ...). The parents remove dead children in their own update,
    so a dead child is still in the list only when it and its parent die in the same frame. Then the child's
    `onDestroy` runs a second time, and that is not harmless: health is not reset on destroy (`PointsComponent` has
    no reset), so `AbstractEnemy`'s `ITEMS` runnable (`C/entities/contracts/AbstractEnemy.kt:140`) disintegrates the
    enemy again and rolls for an item drop again; `BigJumpingJoe` spawns another `SniperJoe`
    (`C/entities/enemies/BigJumpingJoe.kt:128`); `AbstractBoss.onDestroy` spawns the defeat orbs again; 20 other
    `onDestroy` bodies explode, spawn or play a sound.
  - Path D: no caller found; it needs a spawn and then a destroy of the same entity inside a destroy callback.
  - Two callers destroy an entity inside its own `onSpawn` (`EventTrigger`, `MegamanTrailSpriteV2`): the state is
    `SPAWN_QUEUED` there, so this is the normal "spawn then destroy" path, not path C. The guards must keep it.
- **Return values:** `MegaLevelScreen.kt:948,973` call `spawn.onRejected` when `engine.spawn` returns `false` (the
  spawner tries again later); spawners supply fresh entities, so path A does not reach it.

## In scope

The engine rules (`E/GameEngine.kt`), each with a log line under the `GameEngine` tag:

1. **Path A:** `spawn()` of a `LIVE` entity is refused: it returns `false`, no `onSpawn`, no `onSpawnRejected`
   (the entity is live, not rejected by `canSpawn`).
2. **Path B:** `spawn()` of a `DESTROY_QUEUED` entity is a respawn: the update runs `onDestroy`, then `onSpawn` with
   the new properties; the entity ends `LIVE`. Until the update the state is a new `RESPAWN_QUEUED` (`spawned` is
   `true`, because the entity is still in the engine; `dead` is `false`, because it is live after the update).
3. **Path C:** `destroy()` of an `UNSPAWNED` entity is refused: it returns `false`, no `onDestroy`.
4. **Path D:** a destroy call on an entity whose spawn is still queued waits for that spawn: the update runs `onSpawn`
   and then `onDestroy` (as today when both calls come before the update), also when the destroy call comes during
   the destroy phase. The entity ends `UNSPAWNED`.
5. **Tests**, as the owner asked:
   - A temporary old-versus-new suite (as in `062a`): a frozen copy of today's engine, the same seeded random call
     sequences with nested calls; the action logs, return values and states are the same except on the four paths,
     and on those the new result follows the rule (checked by its own oracle, not by a list of cases). Deleted before
     the merge.
   - Permanent engine tests: the alternation invariant (`onSpawn` and `onDestroy` alternate for every entity on random
     sequences), "the last accepted call wins", and one test per rule; the `062a` tests of the odd paths change to the new
     rules.
   - Core tests: the ice shield (a thaw and a freeze in one frame leave the shield live); a parent and its child enemy
     that die in the same frame drop at most one item roll (`onDestroy` of the child runs once).

New APIs and their first callers:

| API | First callers |
|---|---|
| `EntityExistenceState.RESPAWN_QUEUED` | `GameEngine` (rule 2), the derived `spawned` and `dead` |

## Out of scope

- Moving the `dead`/`spawned` readers to named questions (`062c`) and removing the old flags (`062d`).
- The pooled-reuse risk (`019b` and later): a stale reference to an entity that its pool gave to a new owner is live,
  so none of these rules sees it.
- Making the 96 unguarded `destroy()` callers check `dead` (the engine rule makes the check unneeded).
- Resetting health on destroy.

## Intended changes

1. A thaw and a freeze of an enemy in one frame leave its ice shield live (today: destroyed until the next freeze).
2. When a child enemy and its parent die in the same frame, the child's `onDestroy` runs once: no second
   disintegration, no second item roll, no second `SniperJoe` from `BigJumpingJoe`, no second boss defeat orbs.
3. Path A and path D: no caller found; if one exists, `onSpawn` no longer runs twice (A), and an entity spawned and
   destroyed in the destroy phase ends destroyed (D).

## Affected areas and risks

- **Files:** `E/GameEngine.kt`, `E/entities/EntityExistenceState.kt` and `IGameEntity.kt` (`spawned` includes
  `RESPAWN_QUEUED`) and their tests; core tests. No core production file (`dead` is already false for the new state).
- **A caller that depends on an odd path** that the scan missed (for example a spawn that "resets" a live entity).
  Mitigation: the log line stays, so a refused call shows in the log; the owner's play test runs with
  `--logTags GameEngine` over a wider set of rooms and bosses (the contract lists them).
- **Order of `onSpawn` calls in an update** (rule 2): a respawn runs after the destroy phase, so its `onSpawn` comes
  after the other spawns of that update. Only path B entities move.
- **Performance:** a set lookup in `spawn()`/`destroy()` (already there since `062a`); one short list for
  respawns.

## Overlap with other branches

No other branch is active. `019b` (planned) will add destroy notifications on the same paths; it can rely on these
rules.

## Decisions

- 2026-10-27 (owner): a queued respawn has its own state `RESPAWN_QUEUED` (question 1); refused calls log at `debug`
  (question 2); on path D the destroy waits for the queued spawn (question 3).
- 2026-10-27 (owner, "Approved"): the rule is "the last *accepted* call wins": a refused call (returns `false`)
  changes nothing. Found by the temporary suite in step 4: `spawn`, `destroy`, `spawn` before one update refuses the
  second spawn (a spawn is already queued, as today), and the entity ends destroyed, as today.

## Open questions

- None.
