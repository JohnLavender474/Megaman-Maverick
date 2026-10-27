# Notes: mega-refactor-019b-destroy-notification

## Decisions

- 2026-10-27 (owner, later the same day): add `onSpawnQueued()` after all, "for the sake of posterity", with unit
  tests and no manual test. An owner exception to "write code only when production code calls it": the engine calls
  it, no game code overrides it. This replaces the decision below.
- 2026-10-27 (owner, "Yes", replaced by the decision above): no `onSpawnQueued()` hook in this branch; it has no caller. The destroy hook is a
  lifecycle method on the entity (`IGameEntity.onDestroyQueued()`, run by `MegaGameEntity` through
  `runnablesOnDestroyQueued`), not a callback that callers of `destroy()` pass, and not an engine-wide callback like
  the `onQueueToSpawn`/`onQueueToDestroy` that `062a` removed.

- 2026-10-27 (owner): start `019b` now, in parallel with `062b`, although the item suggestion was to do it after
  `062b`.
- 2026-10-27 (owner): do the planning now; the engine change waits until `062b` merges.

## Scan (2026-10-27, `master` at `69b2297d6`)

- `Spawner.test` (`core/.../spawns/Spawner.kt:26-32`) polls `spawn.entity.dead` and clears the held spawn. It runs
  only in the one `spawnsMan.update(delta)` before `engine.update` (`MegaLevelScreen.kt:943`, with the `019a` `TODO`
  at `:938-942`).
- `MegaGameEntity.dead` (`MegaGameEntity.kt:29-31`) is `UNSPAWNED || DESTROY_QUEUED`. The engine sets
  `DESTROY_QUEUED` (or `UNSPAWNED` for a `SPAWN_QUEUED` entity) in `GameEngine.onDestroyQueued`
  (`GameEngine.kt:121-128`), at the time of the `destroy` call. So the poll sees the destroy at the next spawn pass.
- `runnablesOnDestroy` runs in `onDestroy`, inside `destroyNow`, inside the next `engine.update` (after that frame's
  spawn pass). A notification there is one frame later than the poll for a bounds spawner. Only a notification at
  queue time keeps today's timing.
- `SpawnerForEvent.onEvent` (`SpawnerForEvent.kt:26-28`) keeps events only while `!spawned`. `eventsMan.run()` runs
  after the screen render, so an entity destroyed after the spawn pass makes its spawner drop that frame's events.
- `SpawnerForBoundsEntered` (`SpawnerForBoundsEntered.kt:21-26`) keeps a stale `isEntered` while it holds a spawn.
- Pooled reuse (ABA): the pool runnable (`ConstKeys.POOL` in `runnablesOnDestroy`) frees the entity in
  `destroyNow`. If the spawner has not polled before another caller fetches and spawns the same instance, `dead` is
  false again and the spawner holds it forever. Item `062` question 6 waits for `019b` on this.
- Rejected spawns: `SpawnsManager` sets `Spawn.onRejected = spawner::onSpawnRejected`
  (`SpawnsManager.kt:34`); the engine `onSpawnRejected` hook returns the entity to its pool
  (`MegamanMaverickGame.kt:927-933`). This path already clears the spawner.
- Room transition: `beginTransition` (`MegaLevelScreen.kt:283-317`) submits `BEGIN_ROOM_TRANS` and then destroys all
  enemies, after the spawn pass. Live `spawn_room` enemies (verified by parsing the live TMX files): `BulbBlaster`
  in `WilyStage1_v2.tmx` (four in `room8`, two in `desert_man_room`) and `TorikoPlundge` in
  `DesertMan_16x14_v4.tmx` (`room14`). Each lists one room, and the spawn predicate checks the new room, so the
  dropped `BEGIN_ROOM_TRANS` gives no visible change today (inferred).
- Spawn queues still present: `spawns` (drained twice per frame, `MegaLevelScreen.kt:945-950` and `:970-975`), the
  engine queue, `playerSpawnsMan`, and `SPAWN_NOW` (`SpawnersLayerBuilder.kt:86-94`).
- Camera bounds: each `SpawnerForBoundsEntered` calls `camera.getRotatedBounds()` (`SpawnerFactory.kt:25`), one
  pooled rectangle fetch per bounds spawner per frame.

## Overlap with other branches

- `062b` (`mega-refactor-062b-spawn-order-guards`, active, at the scan step on 2026-10-27): it changes
  `GameEngine.spawn`, `onSpawnQueued`, `destroy` and `onDestroyQueued`, the place where a queue-time notification
  would be called. Its "the last call wins" rule (a spawn after a destroy in the same tick respawns the entity)
  decides what a notified spawner must do when its entity is spawned again after the notification.

## `onSpawnQueued()`: added with no caller (owner's decision)

The case that would use it later: a spawner that takes back an entity that was respawned after its destroy
notification (scope open question 1, answered "no" for now).
