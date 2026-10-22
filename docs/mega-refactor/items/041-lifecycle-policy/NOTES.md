# Notes: 041 Lifecycle policy per entity, room inferred from geometry

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Data-driven design (2026-10-22)

Copied as written from the review section "ITEM 40: SPAWN AND CULL LIFECYCLE BY DEFAULT".

````text
(1) Current state

Spawning (`SpawnersLayerBuilder.kt:87-189`) has four modes:
- `spawn_now`: spawn at level load.
- `spawn_room`: an event spawner on `PLAYER_READY` / `BEGIN_ROOM_TRANS` / `SET_TO_ROOM_NO_TRANS`, if the current room is in the comma list. Events can be overridden with `events`.
- `spawn_event`.
- Default (no `spawn_type`): spawn when the spawn shape overlaps the camera (`SpawnerFactory.kt:13-29`).
- `respawnable` defaults to true.

Culling (`AbstractEnemy.kt:201-235`):
- `cull_out_of_bounds` defaults to true: cull 1 s after leaving the camera.
- `cull_events` defaults to true: `GAME_OVER`, `PLAYER_SPAWN`, `BEGIN_ROOM_TRANS`, `GATE_INIT_OPENING`.
- Other entities use their own variants: `getStandardEventCullingLogic` / `getGameCameraCullingLogic` (`GameEntityUtils.kt:38-60`, 168 uses) and `cull_room` on `Block` (`Block.kt:78-97`).

Data from the 18 live maps (my script):
- 998 objects with `spawn_type=spawn_room` and 213 with `spawn_now`.
- 1072 objects have a `spawn_room` value.
- 895 (84%) name exactly one room, and their center is inside that room. These are redundant.
- 37 list several rooms.
- 141 have a center that is in none of their named rooms. These need review before any inference.

X6 (engine.md "Spawning and despawning"):
- Each area has a list of records.
- An object spawns when its record point comes within 48 px of the view.
- A "spawned" bit makes one object per record.
- Going off screen deletes the object and clears the bit, so it comes back when it enters the band again.
- `DeleteObjectKeep` marks the record as single-use.
- Death and restart reset all records.
- There are no per-object room properties. The lifecycle is one engine rule for all objects.

(2) Options

A. Infer the room, and keep everything else.
- When `spawn_type=spawn_room` has no `spawn_room` value, use the room that contains the object center.
- A one-time script removes the 895 redundant values.
- Pro: small, with no change in behaviour.
- Con: the other lifecycle settings are still written by hand.

B. A lifecycle policy per entity in the schema, with explicit overrides (recommended). Each `EntityDef` (item 21) declares a default:
- `ROOM`: spawn on entering the inferred room; cull on leaving the room, on player spawn and on gate opening. Default for enemies.
- `CAMERA`: X6-style band; spawn when near the view; cull when off screen for t s; respawn on re-entry.
- `LEVEL`: spawn now; never culled. Default for blocks, decorations and specials.
- `EVENT`.
- `ONCE`: never respawn after a kill. The same as X6 `DeleteObjectKeep`, and the same as `respawnable=false`.
- The room is inferred from geometry. TMX properties override it only when they differ: `spawn_room` list, `events`, `cull_time`, `respawnable`.
- The validator reports objects with no room, and explicit values that equal the inferred value.

C. The X6 model for all objects: the camera band only, with no room concept.
- Pro: one rule, nothing to write.
- Con: it changes the behaviour of the 998 room spawns. Room transitions in Maverick depend on spawning at `BEGIN_ROOM_TRANS`.

Migration with no change in behaviour:
1. Add inference and the per-entity default, set to today's behaviour.
2. Delete the redundant properties with a script (895 objects). The validator proves that the resolved lifecycle for each object is equal before and after. This is a good golden snapshot: object → (mode, rooms, cull events, respawnable).
3. Review the 141 mismatches by hand.

(3) Recommendation

Option B, migrated through option A first.

(4) Open questions for the owner

- Are the 141 objects whose center is outside their named room intended? For example, an enemy that spawns in room A but stands on the border.
- If the camera backlog item adds X-style camera zones, should the lifecycle key on rooms, zones or both?
- Should a killed enemy respawn when the room is entered again (today: yes, `respawnable` defaults to true)?

(5) Backlog wording

Title: "Lifecycle policy per entity, room inferred from geometry"
- Each `EntityDef` declares a default lifecycle: `ROOM`, `CAMERA`, `LEVEL`, `EVENT` or `ONCE` (spawn rule, cull rule, respawn rule).
- Infer the room from the object center. `spawn_room`, `events`, `cull_*` and `respawnable` in TMX are overrides only.
- A script removes the redundant values (895 of 1072). A snapshot of the resolved lifecycle per object proves there is no change in behaviour. List the 141 mismatches for review.
- The validator reports objects outside every room and overrides that do nothing.

(6) Dependencies

Items 21, 20, 2, 18 (spawn phase / spatial index), 16 (culling moves into a system), 35/36 (respawn resets the spawners, as X6 `RecordsReset` does), and the camera backlog item.
````
