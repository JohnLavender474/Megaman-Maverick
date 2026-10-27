# Scope: mega-refactor-019b-destroy-notification

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-27

## Goal

A spawner learns at once, from the engine, that its entity is destroyed. It no longer polls
`entity.dead`. This fixes the two rare respawn cases in the `TODO` at `MegaLevelScreen.kt:938-942`,
removes the pooled-reuse risk for spawners, and lets the bounds spawners use one camera rectangle
for each spawn pass.

## Item

`019` (one spawn phase per tick with destroy notifications). The parts that this branch takes, from
`ITEM.md` "Proposed scope":

- "Tell a spawner at once when its entity is destroyed, instead of polling `dead`. Do not drop
  events while a spawn is held. (This is what the second pass fixes today.)"
- "Compute the camera bounds once per phase (a quick win against pooled-rectangle churn)."
- "Check and fix the possible missed respawn after a room transition (`BEGIN_ROOM_TRANS` dropped)."

The first part, "one spawn phase at a fixed point in the tick", was done by `019a`.

## Current state

The full scan is in `docs/NOTES.md`. In short:

- `Spawner.test` (`core/.../spawns/Spawner.kt:26-32`) clears its held spawn when it sees
  `spawn.entity.dead`. It runs once per frame, in `spawnsMan.update(delta)` before `engine.update`
  (`MegaLevelScreen.kt:943`).
- `dead` (`MegaGameEntity.kt:29-31`, `UNSPAWNED` or `DESTROY_QUEUED`) becomes true when an
  accepted `destroy()` call (`GameEngine.kt:134-175`, after `062b`) ends the entity's "live after
  the next update": `LIVE` → `DESTROY_QUEUED`, `SPAWN_QUEUED` → `UNSPAWNED`, or a destroy that
  cancels a queued respawn (`RESPAWN_QUEUED` → `DESTROY_QUEUED`/`UNSPAWNED`). Since `062b` a
  destroy of an unspawned entity is refused, and a spawn while a destroy is queued is a respawn
  (`RESPAWN_QUEUED`, `dead` false again).
- Between the destroy call and the next spawn pass, the spawner still holds the spawn:
  - `SpawnerForEvent.onEvent` (`SpawnerForEvent.kt:26-28`) drops the events of that frame.
  - `SpawnerForBoundsEntered` keeps a stale `isEntered` (`SpawnerForBoundsEntered.kt:19-26`).
  - These are the two rare cases of the `019a` `TODO`.
- `runnablesOnDestroy` runs in `destroyNow`, inside the next `engine.update`, after that frame's
  spawn pass. A notification from there would be one frame later than the poll. So the
  notification must come at the time of the destroy call.
- Pooled reuse: the entity goes back to its pool in `destroyNow`. If another caller fetches and
  spawns the same instance before the spawner polls, the spawner holds it forever. (Item `062`,
  question 6.)
- Each `SpawnerForBoundsEntered` calls `camera.getRotatedBounds()` (`SpawnerFactory.kt:25`). Each
  call fetches one pooled `GameRectangle` (`Extensions.kt:112`), so one fetch per bounds spawner
  per frame. The camera does not move during the spawn pass, so all calls give the same rectangle.

## In scope

1. **Engine hook at destroy time.** `IGameEntity.onDestroyQueued()`, a lifecycle method with an
   empty default, next to `onSpawn` and `onDestroy`. `GameEngine.destroy` calls it when it accepts
   a call on an entity that would be live after the next update: `LIVE`, `SPAWN_QUEUED`, and a
   cancelled respawn (`RESPAWN_QUEUED`), also inside the entity's own `onSpawn`. It is not called for
   a refused or repeated destroy, nor by `reset()`, `dispose()` or the queue clearing (the level
   teardown resets the spawners).
2. **Game side.** `MegaGameEntity` overrides `onDestroyQueued()` and runs a new
   `runnablesOnDestroyQueued` map, in the same way as `runnablesOnSpawn` and `runnablesOnDestroy`.
3. **Spawner.** When a spawner makes a spawn, it puts a runnable under `ConstKeys.SPAWNER` in the
   entity's `runnablesOnDestroyQueued`. The runnable clears the held spawn and removes itself.
   `onSpawnRejected` and `reset()` also remove it. The `dead` poll in `Spawner.test` goes.
4. **Bounds spawner.** When it is notified, `SpawnerForBoundsEntered` refreshes `isEntered`, so that
   the edge trigger works as it did with the old second pass (the exact rule is a plan step, with a
   test).
5. **Camera bounds once per pass.** `MegaLevelScreen` writes the rotated camera bounds into one
   `GameRectangle` before `spawnsMan.update`. The bounds spawners read that rectangle. No pooled
   fetch per spawner.
6. Remove the `TODO` at `MegaLevelScreen.kt:938-942`.
7. **Engine hook at spawn time (owner's decision, 2026-10-27: add it for posterity, with unit
   tests and no manual test).** `IGameEntity.onSpawnQueued()`, an empty default method.
   `GameEngine.spawn` calls it when it accepts a call on an entity that would not be live after the
   next update: `UNSPAWNED` → `SPAWN_QUEUED`, a respawn (`DESTROY_QUEUED` with the destroy queued →
   `RESPAWN_QUEUED`), and a spawn inside the entity's own `onDestroy` (queued for the next update).
   It is not called for a spawn refused by `canSpawn` (that is `onSpawnRejected`), refused by the
   `062b` guards, or already queued, nor by `reset()`/`dispose()`. Engine only: no game code
   overrides it, and `MegaGameEntity` gets no map for it.
8. Both hooks run inside the `spawn()`/`destroy()` call, after the engine writes the new state, so
   the entity reads its new state in the hook.
9. Tests:
   - engine, `onDestroyQueued`: called once for each accepting case of item 1 (`LIVE`,
     `SPAWN_QUEUED`, a cancelled respawn, a destroy inside the entity's own `onSpawn`), with the new
     state; not called for a refused destroy (unspawned, inside its own `onDestroy`), a repeated
     destroy, or `reset()`/`dispose()`;
   - engine, `onSpawnQueued`: called once for each accepting case of item 7, with the new state;
     not called for a spawn refused by `canSpawn` (which still calls `onSpawnRejected`), a spawn of
     a live entity, a spawn inside the entity's own `onSpawn`, a repeated spawn, or
     `reset()`/`dispose()`;
   - engine, both hooks, on seeded random call sequences with nested calls: after each call, a hook
     ran exactly when the call changed "live after the next update" (`onSpawnQueued` from no to
     yes, `onDestroyQueued` from yes to no), once, and never otherwise;
   - engine: a temporary old-versus-new suite (as in `062b`) shows that the hooks change no engine
     action, return value or state;
   - core: a spawner frees its spawn on the notification, it accepts events in the same frame, and
     it does not keep a pooled entity that was reused.

New APIs and their first callers:

| API | First caller |
|-----|--------------|
| `IGameEntity.onDestroyQueued()` | `GameEngine.destroy` (calls it); `MegaGameEntity` (overrides it) |
| `IGameEntity.onSpawnQueued()` | `GameEngine.spawn` (calls it); no override (owner's exception to "no API without a caller", 2026-10-27) |
| `MegaGameEntity.runnablesOnDestroyQueued` | `Spawner` (adds its release runnable) |
| The shared spawn camera rectangle (a field, not a type) | `MegaLevelScreen.render` (writes it); `SpawnersLayerBuilder` bounds spawners (read it) |

## Out of scope

- Merging the four spawn queues (`spawns`, the engine queue, `playerSpawnsMan`, `SPAWN_NOW`).
  Backlog for a later branch `019c`.
- A spatial or room index for spawners.
- Moving other readers of `dead` to the notification.
- The engine guards on the call order (`062b`).

## Affected areas and risks

- `engine/`: `IGameEntity`, `GameEngine` (the same functions that `062b` changes).
- `core/`: `MegaGameEntity`, `Spawner`, `SpawnerForBoundsEntered`, `SpawnerForEvent` (maybe),
  `SpawnerFactory`, `SpawnersLayerBuilder`, `MegaMapLayerBuilders` (params), `MegaLevelScreen`.
- Timing: the notification comes at the same moment that `dead` becomes true, so a spawner that
  polled before sees the same thing at the next spawn pass. What changes is between the destroy and
  the next spawn pass. Changes compared with `master`:
  - The two rare cases of the `019a` `TODO` act as they did before `019a` (and better: they also
    cover destroys after the spawn pass).
  - A `spawn_room` spawner whose entity is destroyed in `beginTransition` accepts that frame's
    `BEGIN_ROOM_TRANS`. In the live maps each `spawn_room` enemy lists one room, so the predicate
    still says no. No visible change is expected (inferred; see `docs/NOTES.md`).
  - The contract lists these under "Intended changes".
- Risk: a spawn call on an entity whose destroy is queued (a respawn under `062b`). The spawner was
  already notified, so it no longer holds the entity, and it can make a second instance. See
  "Open questions".
- Risk: a spawner removed by `SpawnsManager` (culled, or not respawnable) while its entity lives.
  Its runnable must be removed, or the entity calls into a dead spawner (harmless, but it keeps a
  reference).

## Overlap with other branches

`062b` merged on 2026-10-27 (`bcbc42c48`); `master` is merged into this branch. Its engine functions
`onSpawnQueued` and `onDestroyQueued` are gone; the logic is in `spawn()` and `destroy()`, so item 1
adds the call in `destroy()`. No other branch is active.

## Decisions

- 2026-10-27 (owner, "yes"): scope approved. A spawner does not take back an entity that is respawned after its
  notification (question 1). The hooks are named `onDestroyQueued` and `onSpawnQueued` (question 2). Tests: a
  temporary old-versus-new engine suite, permanent engine tests for both hooks, and spawner tests written first and
  run on the baseline, where the two `TODO` cases must fail.

## Open questions

- None.
