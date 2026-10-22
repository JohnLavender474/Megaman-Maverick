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
