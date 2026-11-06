# Scope: mega-refactor-038b-level-reset

What this branch changes and why. Write this file after the scan, with `CONTRACT.md` and before
`BRIEF.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-06

## Goal

The respawn routine sends a new `LEVEL_RESET` event when it resets the level. The 24 entity classes that use `PLAYER_SPAWN` as a level reset listen to `LEVEL_RESET` in its place. The event arrives in the same `events.run()` as `PLAYER_SPAWN` does today, so nothing that a player can see changes.

## Item

`038`, "One ordered respawn routine instead of an event chain" (`ITEM.md`), the part that this branch takes:

- Split `PLAYER_SPAWN` into a spawn event for each player and a level reset that the routine owns. Move the 24 listeners that use `PLAYER_SPAWN` as a level reset (`029` `NOTES.md`, review 2, section 4).

Item decisions:

- 2026-10-23 (from `029`): `PLAYER_SPAWN` must split into a spawn event for each player and a separate level reset. Motivation: with respawn per player (co-op), one player's respawn must not reset the room for everyone.
- 2026-11-06 (`038a` brief): `038b` splits `PLAYER_SPAWN` into a spawn event for each slot and `LEVEL_RESET`, and moves the 24 listeners.

## Current state

Scan of `master` at `db3d11cdc` (after the `038a` merge).

- Sender: `playerSpawnEnd()` (`players/PlayerLifecycle.kt:64-71`) submits `PLAYER_SPAWN` with the slot, for slot 0 only. A `TODO` there names this item. `Megaman.onSpawn` calls it (`Megaman.kt:320`).
- The only place that spawns a player is `MegaLevelScreen.resetLevelForRespawn()` (`MegaLevelScreen.kt:517,520`), which the routine step `ResetLevel` calls (`respawn/RespawnSteps.kt:127-132`). So every `PLAYER_SPAWN` today comes from a level reset: at the level start and after each death.
- Timing today: `ResetLevel` runs at the top of `render` (frame K). It queues the spawns. `engine.update` in the same `render` runs `onSpawn`, which submits `PLAYER_SPAWN`. `events.run()` at the end of frame K delivers it.
- `EventsManager.run()` (`EventsManager.kt:60-80`) delivers by listener, in registration order, and for each listener in the order of its own `eventKeyMask`. Inside a run, the submit time of an event does not change the order. So an event submitted at the top of `render` K arrives in the same run, to the same listeners, in the same order as one submitted during `engine.update` K.
- Listeners (24 files, all in `core/src/main`). None of them reads the slot. Each one uses the event as "the level was reset":
  - destroy: `AbstractBoss`, `BigAssMaverickRobotBody`, `Rain`, `Togglee`;
  - cull through a `CullableOnEvent` key list: `AbstractEnemy`, `FireMetSpawner`, `LavaBeamer`, `MuzzleFlash`, `SeaMine`, `ExplosionOrb`, `FloatingEmber`, `Cart`, `SwingingPlatform`, `IProjectileEntity`, and the default list of `getStandardEventCullingLogic` (`GameEntityUtils.kt:39-43`, 31 callers);
  - cull through a predicate: `FireballBar`, `AsteroidsSpawner`, `InfernoMeteorShower`, `PushableBlock`, `Lava`;
  - reset: `Gate` (`resettable` gates lock again), `GearTrolley`, `PropellerPlatform` (motion reset), `RoomShaker` (stops).
- No live map names `PLAYER_SPAWN` in a property, so the `EventType.valueOf` readers (`Spike`, `SpawnersLayerBuilder`, `EventTrigger`) need no change.
- Tests that name it: `GateTest` (the lock-again case), `PlayerLifecycleTest` (slot 0 submits, slot 1 does not), `MegaLevelScreenEventsTest` (the screen mask does not hold it).
- Docs that name it: `players/README.md` (the spawn sequence), `screens/levels/respawn/README.md` (status and frame diagram), `O2-R9` current state on `master`.

## In scope

- New `EventType.LEVEL_RESET`, with no properties. The `ResetLevel` step submits it right after `context.resetLevel()`.
- The 24 listeners use `LEVEL_RESET` in place of `PLAYER_SPAWN`: the key is replaced, and no branch or predicate changes. (Correction after the review: a key mask is an `ObjectSet`, which orders keys by hash, and an enum hash is an identity hash. So the order of keys inside one listener was never fixed between JVM runs, also on `master`. What stays the same is the listener order and the run.)
- `LEVEL_RESET` means "the level reset", not "a player spawned". If `engine.spawn` refused a player spawn (the player is still live, or `canSpawn` rejects it), the old code sent no event and the new code sends it. No current path refuses a player spawn at a reset: the player is destroyed before each reset, and the one live `levelOverride.canSpawn` filters only `Life` and `WeaponEnergyBulb`.
- `PLAYER_SPAWN`: remove it (decision D1, 2026-11-06) and its submit in `playerSpawnEnd()`, with the `TODO`. A spawn event for each slot comes with `047`, when a listener needs it.
- Tests: `RespawnRoutineTest` checks that `LEVEL_RESET` is submitted once in the frame of the level reset, for the level start and for a respawn, and not on game over or cancel. `GateTest` uses `LEVEL_RESET`. `PlayerLifecycleTest` and `MegaLevelScreenEventsTest` follow D1.
- The two package READMEs.
- `Togglee`: its comment "this assumes that a player spawn is never in the same room" names the old event; reword it for the level reset.

## API and first caller

| API | First caller in the branch |
|-----|----------------------------|
| `EventType.LEVEL_RESET` | `ResetLevel.begin` (submit); the 24 listeners (receive) |

No other new API.

## Out of scope

- A spawn event for each slot (`047`, see D1).
- `PLAYER_DESTROYED` and `LEVEL_FAILED` (the `119` decision of 2026-11-02).
- Any change to what a listener does on the reset.
- `BEGIN_ROOM_TRANS`, `GATE_INIT_OPENING` and the other cull keys in the same lists.

## Affected areas and risks

- 24 entity files, `EventType`, `PlayerLifecycle`, `RespawnSteps`, 3 tests, 2 READMEs.
- A missed listener would stop culling or resetting on a respawn. The grep for `PLAYER_SPAWN` must give 0 lines in `core/src/main` (with D1 as recommended).
- Frame: the event moves from `onSpawn` to the step before `engine.update`, in the same frame. Delivery is the same run (see "Current state"). The only case that differs: a reset whose player spawn does not happen in that `engine.update`. The engine always processes a queued spawn at the start of the next `update`, so this case has no trigger.

## Overlap with other branches

- `081b` (`mega-refactor-081b-x-run-jump-shoot`): adds a second player class that calls `playerSpawnEnd()`. If `081b` merges first, its X class gets the change through the shared function. If this branch merges first, nothing in `081b` names `PLAYER_SPAWN` today. Check again before the merge.
- `x6-intro-extra-level` merged; its new entities (`D1000` and others) do not name `PLAYER_SPAWN`.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R3` No new fragile order | helps | The level reset event comes from the routine step, not from a side effect of a player `onSpawn`. |
| `O1-R5` Code only with a production caller | helps | `LEVEL_RESET` has a sender and 24 listeners. D1 as recommended drops an event with no listener in place of keeping one. |
| `O2-R5` No logic assumes one player | helps | The level reset no longer depends on which slot spawns. |
| `O2-R9` Events and progress name a slot | no effect | `LEVEL_RESET` is a level fact, not a player fact, so it has no slot. The accepted gap "only slot 0 submits `PLAYER_SPAWN`" (2026-11-01) goes, because the event goes (D1). |
| `O6-R2` Named checks | helps | Frame test for the new event. |
| `O2-R4`, `O5-R1` | no effect | No new iteration or allocation. |

## Decisions

- D1 (owner, 2026-11-06): remove `PLAYER_SPAWN`, its submit in `playerSpawnEnd()` and the slot-0 `TODO`. `047` adds a spawn event for each slot when a listener needs one.

## Open questions

None.
