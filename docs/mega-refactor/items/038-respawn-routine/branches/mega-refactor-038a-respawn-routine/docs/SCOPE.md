# Scope: mega-refactor-038a-respawn-routine

What this branch changes and why. Write this file after the scan, with `CONTRACT.md` and before
`BRIEF.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-06

## Goal

One `RespawnRoutine` runs the death and the respawn of the player as fixed steps in a fixed order, in place of the chain of 5 events and 2 screen handlers. The routine reuses `Routine` and `Step` from `engine/.../state/`.

## Item

`038`, "One ordered respawn routine instead of an event chain" (`ITEM.md`):

- A `RespawnRoutine` with fixed steps: reset world, place player, reset camera and room, apply checkpoint data, music, beam-in, ready.
- Keep the public events for entities, but submit them from one place in a set order.
- Remove the stale room-music block in `PLAYER_SPAWN_INIT`.
- Split `PLAYER_SPAWN` into a spawn event for each player and a level reset that the routine owns. Move the 24 listeners that use `PLAYER_SPAWN` as a level reset.
- Define the routine behind an interface. The single-player routine copies today's behavior. `047` adds a co-op routine.

Item decisions: `PLAYER_SPAWN` splits (2026-10-23, from `029`). The entity keeps `PLAYER_DESTROYED` and the routine sends `LEVEL_FAILED` (2026-11-02, from `119`). The routine can reuse the `118a` `Step` and `Routine` (2026-11-02).

The proposed branch split (decision D1 of the brief): this branch, `038a`, makes the routine and keeps every public event and its frame. `038b` splits `PLAYER_SPAWN` into a spawn event for each slot and `LEVEL_RESET`, and moves the 24 listeners.

## Current state

Scan of `master` at `73fdb0f8c`.

### The chain

| # | Event | Sender | Handled by | What happens |
|---|---|---|---|---|
| 1 | `PLAYER_JUST_DIED` | `Megaman.onDestroy` (`Megaman.kt:374`), every slot, every destroy route | `MegaLevelScreen.kt:562-590` | slot 0 only: destroy the other players, stop music and loops, remove a life, `playerDeathEventHandler.init()`, force resume |
| 2 | `PLAYER_DONE_DYIN` | `PlayerDeathEventHandler.kt:84`, after 4 s and a 0.25 s fade out | `MegaLevelScreen.kt:592-607` | play level music; game over if no lives; else `playerSpawnEventHandler.init()` |
| 3 | `PLAYER_SPAWN_INIT` | `PlayerSpawnEventHandler.kt:133` (with `TURN_CONTROLLER_OFF`) | `MegaLevelScreen.kt:514-555` | reset backgrounds, camera, fights, boss bar, systems on, enemies destroyed, spawners rearmed, `engine.spawn` for each slot, stop shake, room-music block |
| 4 | `PLAYER_SPAWN` | `Megaman.onSpawn` (`Megaman.kt:364-365`), slot 0 only, the frame after 3 | 24 entity classes | level reset: cull or reset state |
| 5 | `PLAYER_READY` | `PlayerSpawnEventHandler.kt:218` (with `TURN_CONTROLLER_ON`), after fade in 0.25 s, pre-beam 1 s, beam down 0.5 s, beam land 0.35 s | `MegaLevelScreen.kt:557-560` (sends `TURN_CONTROLLER_ON` again), `DarknessV2`, `DarknessV4`, `RisingLavaRiver`, `SpawnersLayerBuilder` | play starts |

- The level start runs only the respawn half: `MegaLevelScreen.kt:401-402`.
- The handlers run in a `when` in the screen render (`MegaLevelScreen.kt:812-816`), with a comment that the order must not change. `:818` makes the death handler inactive when the player is live.
- `PLAYER_SPAWN_INIT` has no listener outside the screen. `PLAYER_DONE_DYIN` has 4 entity listeners: `DrWily`, `QuestionBlock`, `RisingLavaRiver`, `LaserBeamer`. `PLAYER_JUST_DIED` has 4 entity listeners: `GauntletMaster`, `PreciousGem`, `PreciousGemCluster`, `SpiderWeb`.

### The room-music block

`MegaLevelScreen.kt:545-554` reads `nextGameRoom()`, which falls back to `currentGameRoom`. At that moment the camera reset is only a flag (`CameraRoomBounds.kt:47-58`), so both are the room of the death. `LevelRoomHandler.onSetToRoomNoTrans` (`LevelRoomHandler.kt:157-180`) plays the music of the respawn room on the next camera update, if the room has `music` and it is not already playing. Among the live maps, only `SMB3_1.tmx` has rooms with `music`, and every one of its 5 `game_rooms` objects has it; `SMB3_1` has no level music. So in every case the respawn room's music plays after the camera update, with or without the block. The block only plays the death room's track for the frames before that. With the block gone, nothing that a player can hear changes. (Correction, 2026-11-06: the brief first said that the level music would play in one `SMB3_1` case. That case does not exist.) The issue `docs/issues/2026-10-22-respawn-stale-room-music.md` says no live room has `music`; that is also wrong. Close the issue at the merge and correct it.

## In scope

- `RespawnRoutine` in `screens/levels/respawn/`: a `Routine` of steps for the death half and the respawn half. Its first caller is `MegaLevelScreen` (on a slot 0 `PLAYER_JUST_DIED`, and at level start).
- The steps take the work of `PlayerDeathEventHandler`, `PlayerSpawnEventHandler` and the 3 screen `onEvent` branches. Each step submits the public event that it replaces, in the same frame as today.
- `PLAYER_SPAWN_INIT` goes (no outside listener). The screen stops listening to `PLAYER_JUST_DIED`'s follow-ups `PLAYER_DONE_DYIN` and `PLAYER_READY`.
- The duplicate `TURN_CONTROLLER_ON` (`MegaLevelScreen.kt:559`) goes.
- The room-music block goes.

## Out of scope

- The `PLAYER_SPAWN` split and the 24 listeners (`038b`).
- `PLAYER_DESTROYED` and `LEVEL_FAILED` (`119`).
- An interface for the routine, and co-op respawn (`047`).
- Checkpoint data (`036`, `037`). Freezes (`012`). The level flow state machine (`013`).

## New API and first caller

| API | First caller |
|---|---|
| `RespawnRoutine` | `MegaLevelScreen.onEvent(PLAYER_JUST_DIED)` and the level start |
| One `Step<RespawnContext>` per step | `RespawnRoutine` |
| `RespawnContext` | the steps |
| `EventsManager.runCount` (engine) | `AwaitEventsRun` and `BeamUntilEventsRun`: the old chain did its next part in the next `events.run()`, so the steps wait for that run (added after the review, 2026-11-06) |

## Affected areas and risks

- `MegaLevelScreen` (`onEvent`, render, `reset`), `PlayerSpawnEventHandler`, `PlayerDeathEventHandler`, `EventType`.
- Frame order: today `PLAYER_SPAWN` arrives one frame after `PLAYER_SPAWN_INIT`, because `engine.spawn` is queued. Entities that spawners make in that frame can get culled by it. The routine must keep these frames.
- Pause: the death forces a resume, and the handlers do not run while paused.
- Game over: no respawn steps run after the lives check fails.

## Overlap with other branches

- `118j` (active) changes `Gate.kt` and the fight events. `Gate` listens to `PLAYER_SPAWN`, which this branch does not change.
- `081a` (active) moves the player lifecycle out of `Megaman`, including `onSpawn` and `onDestroy`. `Megaman.kt:364-374` sends events 1 and 4. Merge order matters; this branch does not change those lines.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` Components, not new inheritance | no effect | no new entity class line |
| `O1-R3` No new fragile order | helps | the cross-frame event chain becomes one routine; this is the rule's own example |
| `O1-R4` Typed state | no effect | `RespawnContext` is typed; no new `ConstKeys` |
| `O1-R5` Code only with a caller | no effect | no interface (decision D2); each new type has a caller above |
| `O2-R4` Stable order | no effect | players still iterate in slot order |
| `O2-R5` No logic assumes one player | waiting | the routine acts on slot 0, as the handlers do today; the `TODO` comments move with the code (`047`) |
| `O2-R7` Gameplay does not depend on rendering or audio | no effect | the fade and beam draws stay draws; the steps finish on timers, as today |
| `O2-R9` Events and progress name a slot | no effect | each submitted event keeps its `SLOT` |
| `O5-R1` No per-frame allocation | no effect | the routine and its steps are made once per screen |
| `O6-R1` CI passes, `O6-R2` Named checks | helps | `CONTRACT.md` lists the checks |

## Owner decisions (2026-11-06, through the brief)

- D1: two branches, each merged directly into `master`. `038a` makes the routine with the same public events. `038b` splits `PLAYER_SPAWN` into a spawn event for each slot and `LEVEL_RESET`, and moves the 24 listeners.
- D2: no interface for the routine yet. `047` designs the co-op shape when it has a second caller.
- D3: the routine owns the death half and the respawn half (steps 1 to 6). The level start enters at step 4.

## Open questions

None.
