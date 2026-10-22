# Notes: 019 One spawn phase per tick with destroy notifications

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Game loop and engine core (2026-10-22)

Copied as written from the review section "ITEM 18. One spawn phase; spatial index for spawners".

````text
(1) Current state

Order of one frame (`MegaLevelScreen.kt:876-940`):
- pass 1 `spawnsMan.update(delta/2)`
- drain `spawns` into `engine.spawn` (this only queues)
- event handlers
- `engine.update`: it first adds all queued spawns (`GameEngine.kt:111-114`), then runs the systems
- pass 2 `spawnsMan.update(delta/2)`
- `playerSpawnsMan.run()`
- drain into `engine.spawn` again
- backgrounds, `cameraManagerForRooms.update`, `gameCamera.update`
- after `screen.render`: `eventsMan.run()` delivers all events (`MegamanMaverickGame.kt:530`)

Spawners:
- `SpawnerForBoundsEntered`: camera-overlap edge trigger, `!wasEntered && isEntered`.
- `SpawnerForEvent`: collects events in `onEvent`, but only while `!spawned`. It tests them on the next `test`.
- Both use `Spawner.test`, which clears the held spawn only when it polls `spawn.entity.dead` (`Spawner.kt`).
- `dead` is set at once by `engine.destroy` (the `onQueueToDestroy` hook, `MegamanMaverickGame.kt:864-865`).

Is the second pass "same frame"? No.
- Spawns queued in pass 2 go to `entitiesToSpawn` and are added at the start of the NEXT `engine.update`. Pass 1 of the next frame would give exactly the same timing.
- The camera has not moved between pass 1 and pass 2. The camera update comes after pass 2. So both passes test the same camera bounds.
- Events are delivered only after both passes.
- An entity queued during `engine.update` (for example by a system) can never be added in the same frame, because the queue is drained only at the start of the update.

What pass 2 really does (verified by reading):
- It refreshes the spawner state early. It clears `spawn` for entities that died during `engine.update`, and it re-samples the edge trigger.
- Effect: an event spawner whose entity died in this update accepts this frame's events. Without pass 2, `onEvent` drops them because `spawned` is still true.
- So pass 2 fixes a dropped-event problem. It does not give a frame of latency.
- The same problem still exists for entities destroyed after pass 2. Possible bug, unverified, needs a runtime check: `beginTransition` destroys all enemies inside `cameraManagerForRooms.update` (`MegaLevelScreen.kt:296`) and submits `BEGIN_ROOM_TRANS`. Their `SpawnerForEvent` still holds `spawn != null` when the event is delivered, so it drops the event. `END_ROOM_TRANS` is in the key mask but not in the default accept list (`SpawnersLayerBuilder.kt:113-121`). Result: a `spawn_room` enemy whose room list contains both the old and the new room may not respawn when you enter the new room.

`delta / 2f`: no spawner uses `delta` today. `shouldTestPred` is `ALWAYS_TRUE` or a room-transition check (`SpawnersLayerBuilder.kt:33-35`, `:60-63`). `trySpawn` ignores `delta`. So the value is harmless but misleading. The two halves add up to one delta, so a future timer would still be correct.

Cost:
- 2 passes x all spawners (about 250-500 for each level) on every frame.
- Each camera spawner calls `camera.getRotatedBounds()` (`RotatableCamera.kt:71-75`). That fetches a pooled `GameRectangle` on every call (`Extensions.kt:112`). So there are about 500-1000 pool fetches for each frame, only for spawner tests.

(2) Options
- (a) Keep the two passes, but document them, and fix the `spawned` drop: clear `spawn` in a destroy callback instead of polling `dead`. Compute the camera bounds once per pass. Pros: small. Cons: the order stays implicit.
- (b) One explicit spawn phase in each tick, at a fixed point: after the events are delivered and after the camera update of the previous tick, and before `engine.update`. Spawners are told about a destroy at once (the entity's `runnablesOnDestroy` notifies its spawner), so no second pass is needed. Then the spawn appears in the same tick's `engine.update`.
- (c) Event-driven spawn requests: room enter/exit, the camera entering a cell, and the entity's destroy push "spawn requests" into one queue that the spawn phase drains. Pros: no scan. Cons: more machinery.
- Spatial index (with any of a-c): put spawners in a uniform grid (cell = one screen, or bucket them by room, see item 40). Each phase tests only the cells near the camera. To keep the edge-trigger semantics, keep the set of spawners that overlapped the camera in the last phase, and set `isEntered = false` for spawners that leave the set.

(3) Recommendation
- Do (b) with the destroy notification. Fold in the 4 spawn queues: `spawns`, the engine queue, `playerSpawnsMan`, `SPAWN_NOW`.
- Compute the camera bounds once per phase now (a quick win).
- Add the spatial index only if the profiling baseline (item 8) shows it. At 250-500 spawners the scan itself is cheap; the pooled-rectangle churn is the bigger cost, and the quick win removes it.
- Check the suspected room-transition drop with a runtime test before you change anything.

(4) Open questions for the owner
- Which concrete entity needed the second pass? A name would let us confirm that the destroy notification covers it.
- Must a destroyed entity be able to respawn in the same tick (for example blocks that reappear)? That decides whether the engine also needs an end-of-update spawn flush.

(5) Proposed backlog wording

Title: "One spawn phase per tick with destroy notifications"
- Replace the two `spawnsMan.update(delta/2)` passes with one spawn phase at a fixed point in the tick.
- Notify a spawner at once when its entity is destroyed, instead of polling `dead`. Do not drop events while a spawn is held.
- Merge the spawn queues. Compute the camera bounds once for each phase.
- Optional: a spatial or room index for the spawners, if profiling shows a cost.
- Check and fix the possible missed respawn after a room transition (`BEGIN_ROOM_TRANS` dropped).

(6) Dependencies
- Item 9 (the tick).
- Item 12 (level flow).
- Item 40 (infer `spawn_room`, room buckets).
- Item 44 (pooled camera rectangles).
- Item 37 (respawn routine).
````

## Research: origin of the second spawn pass (2026-10-22)

Decision K. Copied as written from the research agent's report.

````text
## Summary

Git history does not name an entity or a bug for the "second pass". The premise also needs a correction. The post-engine `spawnsMan.update` is the original pass. Commit `aea69ae3a` added the pre-engine pass and kept the old pass. In the current code, the post-engine pass only refreshes spawner state one frame early. I found no entity in a live level whose behaviour depends on it. The "same-frame spawn" rationale is not true for the current engine.

## Commits

- **`aea69ae3a`**, 2024-09-12, "Finish draft 1 of TimberWoman.tmx, start on PolarityMan.tmx draft 1". This is a large mixed commit (39 files, mostly entities and TMX). In the file then at `core/src/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt`, it did three things:
  - Before the commit, there was one `spawnsMan.update(delta)` after `engine.update(delta)`, and it ran after `cameraManagerForRooms.update`.
  - The commit added a new `spawnsMan.update(delta / 2f)` plus a spawn drain before `engine.update`.
  - It changed the old pass to `delta / 2f` and moved `backgrounds` and `cameraManagerForRooms.update` to after it.
  - The commit message says nothing about spawning. (Verified: `git show aea69ae3a`.)
- **`7309ed841`**, 2024-09-10, two days earlier, "Continue working on TimberWoman.tmx". It changed `Spawner.test` from `!spawn.entity.spawned` to `spawn.entity.dead`.
  - The old check used the engine's `spawned` state, which becomes false only inside `engine.update` (`destroyNow`).
  - The new `dead` flag is set at once by `onQueueToDestroy`.
  - It also added a `debugFilterByEntityTag` spawner debug log, so the owner was debugging spawner respawns at that time (inferred).
  - "Won't be spawnable until after the engine has run" matches the old `spawned` check, not the `dead` check that was already in place when `aea69ae3a` landed (inferred).
- **`a8f7706db`**, 2024-11-26. This changed `spawn_room` from a camera-bounds spawner on the room rectangle to a `SpawnerForEvent`. So at the time of `aea69ae3a`, no map used an event spawner. No TMX file in the full history has ever used `spawn_event` (verified: `git log -S spawn_event -- assets` is empty).
- **`4511bef40`**, 2026-10-17. It only added `onRejected` handling to both drain loops.
- I checked the engine at `aea69ae3a` (`core/libs/Mega-2D-Game-Engine-1.0.jar`, with `javap`). `spawn()` already queued the entity (`EntitiesToSpawn.add`), and `update()` drained the queue first. So no version of pass 2 ever gave a same-frame spawn (verified).

## The earlier review, checked

1. **Pass 2 spawns are added on the next frame: confirmed.** See `engine/.../GameEngine.kt:63-66` and `:106-113`, and `MegaLevelScreen.kt:897` (pass 1), `:919` (engine), `:922` (pass 2).
2. **Spawner state is refreshed early: confirmed.**
   - `Spawner.kt:26-32` clears the held spawn only when it polls `dead`.
   - `SpawnerForEvent.kt:26-28` keeps events only while `!spawned`.
   - `eventsMan.run()` runs after `screen.render` (`MegamanMaverickGame.kt:497`, `:530`).
   - `dead` is set at queue time (`MegamanMaverickGame.kt:864-865`).
3. **Bounds spawner uses an edge trigger and the camera updates after pass 2: confirmed** (`SpawnerForBoundsEntered.kt:21-26`, `MegaLevelScreen.kt:933-934`). In practice, pass 2 cannot fire a bounds spawn (inferred):
   - It sees the same static spawn shape and the same camera as pass 1 of the same frame.
   - The `ROOM_TRANSITION` gate is also unchanged, because it is set in camera callbacks after pass 2.
   - A spawner freed in pass 2 has a stale `isEntered == true` from when it spawned.
   - Pass 2's only bounds effect: it refreshes `isEntered` with the pre-update camera, one frame earlier.
4. **`delta / 2f` does nothing: verified.** No spawner predicate or `trySpawn` reads `delta`.

## Entities in live levels (verified by parsing the 18 TMX files named in `LevelDefinition.kt`)

- **`spawn_event`:** none.
- **`spawn_room` (all `SpawnerForEvent`):**
  - Blocks: `Block`, `GoldBlock`, `GravityBlock`, `PushableBlock`, `RocketPlatform`, `InfernoChainPlatform`, `PropellerPlatform`, `ConveyorBelt`, `PinkBlock`, `PreciousBlock`, `WoodCrate`, `AbstractBlock`.
  - Hazards: `Saw`, `FireballBar`, `InfernoOven`, `LavaRiver`, `LaserBeamer`, `TubeBeamerV2`, `PipePuff`, `UnderwaterFan`, `IceCubeMaker`, `CeilingCrusher`, `AsteroidsSpawner`, `InfernoMeteorShower`.
  - Decorations: `Lantern`, `Snowfall`, `FallingLeaves`, `MockingByteNest`.
  - Enemies: `TorikoPlundge` (DesertMan `room14`), `BulbBlaster` (WilyStage1 `room8`, `desert_man_room`).
  - Items: `HeartTank`, `HealthBulb`, `Life`.
  - Specials: `FloorButton`.
  - Only one object sets `events`: an `AbstractBlock` in `WilyStage3_v4.tmx:3701` (`wily_room`, `events=end_room_trans`). All others use the defaults `PLAYER_READY`, `BEGIN_ROOM_TRANS` and `SET_TO_ROOM_NO_TRANS` (`SpawnersLayerBuilder.kt:114-121`).

For pass 2 to matter, an entity must die inside `engine.update` in the same frame in which an accepted event for its own room is delivered. The normal flows do not do this (inferred):

- **Room exit.** `BEGIN_ROOM_TRANS` is submitted in the camera callback after pass 2 (`MegaLevelScreen.kt:286`). Cull-on-event entities only set a flag in `CullableOnEvent` and are destroyed in the next `engine.update` (`GameEntityUtils.kt:38-54`). Enemies are destroyed at `:296`, also after pass 2. The spawner then polls many frames before the player can come back.
- **Player respawn.** `PLAYER_SPAWN` culls entities on the frame after Megaman spawns (`Megaman.kt:507`). `PLAYER_READY` comes after the beam timers (`PlayerSpawnEventHandler.kt:196`).
- **`LavaRiver`** stays alive across `room4`, `room4a` and `room4b` by design (`LavaRiver.kt:150-157`).

## What would break without pass 2 (inferred, not tested in game)

- **Normal play:** no visible change is expected in any live level.
- **Rare coincidence 1:** a `spawn_room` entity dies in `engine.update` in the same frame that an accepted event for its own room is submitted. Without pass 2, the spawner drops the event. The entity does not come back until the next accepted event (the next room entry or `PLAYER_READY`). The player sees a missing block, hazard or decoration in that room.
- **Rare coincidence 2:** a bounds-spawned entity dies off-screen in the frame just before its spawn point scrolls into view. Without pass 2, it does not respawn until the camera leaves and re-enters its spawn area.
- **Same-frame spawns:** removing pass 2 does not delay any spawn. Pass 1 of the next frame queues before that frame's `engine.update`, which is the same frame in which pass 2's queued spawns would be added.
- **Keep the rest of the block:** `playerSpawnsMan.run()` and the second drain loop (`:924-930`) sit in the same block. Do not remove them with the pass.

## How a destroy notification covers this

The spawner clears `spawn` the moment its entity is queued to be destroyed: the same moment that `dead` is set (`onQueueToDestroy`), for example through `runnablesOnDestroy` or an engine hook. Then:

- **Events:** `spawned` is correct when `eventsMan.run()` delivers events, whatever the position of the spawns-manager pass. This covers everything pass 2 does for event spawners.
- **More than pass 2:** it also covers destroys that happen after pass 2, for example enemies destroyed in the begin-transition callback. This can accept events that today are dropped. Check this against the "gameplay is the contract" rule, though with the current room predicates I found no case that changes (inferred).
- **Pooled reuse:** it removes the risk that `4511bef40` fixed, because the reference is dropped before the entity goes back to the pool.
- **Bounds spawners:** on notification, also refresh or reset `isEntered`. The stale `true` value is the only bounds state that pass 2 refreshes.
- **Result:** with this in place, one `spawnsMan.update(delta)` before `engine.update` is enough. Keep `playerSpawnsMan.run()` and its drain where they are.
````
