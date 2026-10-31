# Scope: mega-refactor-051c-camera-zones

- Status: approved
- Approved: 2026-10-30

## Goal

A level can change the camera limits inside a room with zones in Tiled, with no room transition. Players-only blocks
can hold the player at a screen edge, and a player pinned against a solid dies at once. Levels with no zones play the
same.

## Item

`051`, from the `ITEM.md`, "Decisions" of 2026-10-30: the rule pipeline, the ranges, the fold in Tiled order, the
snap and ease rule, the hold test, `CrusherComponent`, and the edge blocks. The research is in the item `NOTES.md`,
"Camera rules research for `051c` and `051d`".

## Current state

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`. `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`.
`MLS` = `C/screens/levels/MegaLevelScreen.kt`. Scan at `ba4b3ca1c`.

- Part order (`MLS:287-293`): `CameraRoomBounds`, `CameraRoomTransition`, `CameraFollow`, `CameraRoomClamp`; effects
  `CameraRotationStep`, `CameraShake`, `follow.stopRule`. The controller runs in each unpaused frame (`MLS:863-874`).
- `CameraRoomClamp` (50 lines) has 2 jobs: the clamp and the call to `scanForNextRoom`. It reads a room rectangle
  directly and swaps the viewport by `camera.direction` (`:33-39`). In a room smaller than the view, min wins (`:41-48`).
- `CameraRoomBounds.reset()` has 3 call sites (`MLS:296, 488, 1011`). The reset frame has no clamp.
- `CameraController` has no `reset()`, and it has no state for limits.
- No Tiled layer or data exists for camera limits. `GameRoomsLayerBuilder` is the pattern, and
  `MegaMapLayerBuilders.kt:37` lists the layers.
- `AbstractBlock` is a players-only block for contacts. Body collision does not read its filter, and its `onSpawn`
  replaces `BLOCK_FILTERS` (see the item `NOTES.md`, "Players-only blocks").
- `GravityBlock.kt:213-218` is the one crush test. `CeilingCrusher` and `PlayerDeath` are kill areas.
- Item `051e` will change the clamp's read of `camera.direction`. This branch keeps that read.

## In scope

1. **The limits stage, with no feature.** The first commit group. Levels play the same.
   - Split the room scan from `CameraRoomClamp`. Tracking stays first.
   - A limits result: per axis a range. A zone locks an axis with a range of one view size (item 3).
   - The room supplies the target. The cut uses `currentGameRoom ?: priorGameRoom`, as the clamp does now. No limits
     stage in `CameraFollowMode.OFF`.
   - `CameraController.reset()` resets each `Resettable` part. The 3 call sites become 1 call.
   - The clamp keeps "min wins" and does not use `coerceIn`.
2. **An equivalence check for step 1.** A temporary suite. The frozen copy comes from commit `b7fb77bb8` (no branch holds
   it) or from `CameraManagerForRooms` at `7fe00b8b8^`. It adds cases: the pan-end snap of the cross axis, the reset frame
   with no clamp, min wins in a short room, a rotated pan.
3. **Zones.**
   - A layer `camera_sections` and a builder. A zone is data, not an entity, with `type=zone`. Properties `min_x`, `max_x`,
     `min_y`, `max_y` are references to Tiled objects and name each edge that it sets; other edges keep. A `min_*`
     property reads the low edge of its object, and a `max_*` property reads the high edge. The numbers and the pin went
     away on 2026-10-30 (owner decision), because world tiles with y up were hard to read in Tiled. No numeric form.
   - A part that folds the active zones in Tiled object order each frame. A zone is active while
     `getFocusPosition()` is inside it.
   - Zones never change `currentRoom`. The 25 reads stay the same.
4. **The ease.** The limits move to the target, relative to the camera, at a speed from the zone. Snap when the room
   changes, on a reset, or when the focus is out of view after the move. The ease reads the position after the movers,
   not the position with shake. An injected hold test stops the ease; `051d` is its second reader.
5. **Edge blocks.** A new class on `AbstractBlock`, for example `CameraEdgeBlock`, so that callers do not repeat the
   flags:
   - body collision with players only, through a block filter added after `super.onSpawn`;
   - no culling, no drawing;
   - it follows the current limit, and it acts only on an edge that a rule sets and that differs from the room edge.
   `edge_blocks=left,right` on a zone enables it per edge. Zones default to off. Code spawns it, not Tiled.
6. **`CrusherComponent`** on the crushing entity, with one test per player in `body.preProcess` or an updatable. It has
   the player side that it hits, an active predicate, and "needs a solid on the opposite side". `GravityBlock` moves to
   it with no behavior change, and the edge block is its second caller. It kills through one shared path that keeps the
   `instant` meaning. World sides map to the player's fixtures when gravity is rotated.
7. **Test rooms** in `RefactorTest051.tmx` (`REFACTOR_TEST_051`): overlapping zones reached from both sides, with a
   spawn in the overlap; a pipe into a far zone; a zone over a gap; a pan with a zone-limited cross axis; a shake at a
   clamped edge during an ease; an edge block against a wall; a rotated player against an edge block.
8. **Permanent tests** for the fold, the snap and ease rule, the limits result, and `CrusherComponent`.

## Out of scope

- Auto-scroll, moving pins, `next` and stop modes: `051d`.
- Rotation as a part, and the view dimensions in the clamp: `051e`.
- A view that goes past the current room. A later branch can add it if a level needs it.
- A level validator (owner decision).
- A one-way zone, which stores state. `051d` adds the permanent limit, and a zone can use it later.
- `CameraRoomTransition`: its pan target stays as it is.
- `CeilingCrusher` and `PlayerDeath`: they stay kill areas.
- A camera target that is not a player, and co-op focus (`047`).
- Limits in checkpoint data (`036`).

## Affected areas and risks

- Files: `CameraController.kt`, `CameraRoomBounds.kt`, `CameraRoomClamp.kt`, `MegaLevelScreen.kt`,
  `MegaMapLayerBuilders.kt`, `GravityBlock.kt`, new parts, a new layer builder, a new block class, a new component,
  tests, the test map, root `CLAUDE.md`.
- Risk: step 1 is the one change that can alter live levels. The equivalence check covers it, and it is a separate
  commit group.
- Risk: the snap rule. With no zones, the target changes only with the room, so a one-frame snap stays. The cases at up
  to 23 live room pairs are in the equivalence check.
- Risk: `GravityBlock` must kill in the same cases. Check in `RefactorTest029.tmx` and `MOON_MAN`.
- Risk: a block filter on a moving block. The block moves after `engine.update`, so a push acts in the next frame.
- Risk: the collision handler can push a pinned body through a wall. The crush test runs before collision.

## Overlap with other branches

- `031e-bindings`: changes `MegaLevelScreen.kt` in other places (the select button read), not the camera code.
- `084c-movement`: post-action momentum and the room-transition carry. No shared file now. Its room carry reads the
  pan. This branch does not change the pan. Check again at branch start.
- `084` splits `Megaman`. `CrusherComponent` reads player fixtures through `EntityQueries.kt` and does not change
  `Megaman`.

## Open questions

None. The owner accepted the 4 recommendations on 2026-10-30: a zone can lock an axis; the hold test is in this branch; one
layer `camera_sections` with `type`, and the property names in item 3; one default ease speed in `ConstVals` with a zone
property that overrides it, with the value chosen in the first test room.
