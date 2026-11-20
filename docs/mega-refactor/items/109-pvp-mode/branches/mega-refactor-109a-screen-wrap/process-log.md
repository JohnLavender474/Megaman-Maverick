# Process log: mega-refactor-109a-screen-wrap

## Status

- State: active
- Goal: horizontal screen wrap in a room with the `wrap` flag (players and shots), and the test map `REFACTOR_TEST_109`
- Done: steps 1 to 10 (play test open)
- Brief: https://claude.ai/artifact/68FR4oFMUSn7UbZTCSEmQb (approved 2026-11-19)
- Next: owner play test of the manual checks in `SCOPE.md`
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-19

- Branch start. Research and owner decisions are in the item `NOTES.md` ("Horizontal screen wrap", "Wrap filter and counter") and `ITEM.md`.
- Planning: scan, `SCOPE.md`, brief, owner answers (D1 fail fast on width, D2 `TODO`s), `PLAN.md`. Merged `master` (`bfd12356d`) into the branch first.
- Scope change (owner): move the draw offsets out of `WorldSystem` into `BodyInterpolation` as step 2b. `SCOPE.md`, `PLAN.md`, the brief updated; brief published again to the same link.

## 2026-11-19 (implementation)

- Step 1 (subagent): `RefactorTest109.tmx`, room `arena` at x=32, y=64, 512x448 px, `wrap=true`. Blocks: full-width floor, ceiling, low seam pair (3 tiles each side), high seam pair (2 tiles each side), inner platform (6 tiles). Player spawn at x=128 on the floor; `player_2` to a rect at x=416 in `player_slot_spawns`. `tiles` painted on 112 cells. Helper `temp/109a-test-map/finish_map.py`. `LevelCompletionScreensTest` passes.
- Step 2 baseline (before any engine change), `engine:allocationTest`: `WORLD_ALLOC` 16 bytes per frame, `SNAPSHOT_ALLOC` 0 bytes per fill. Timing of the same 200-body world (temporary spec `temp/109a-baseline/TempWorldTimingTest.kt`, copied into `engine/src/allocationTest/` only for the run, not committed): median 4.95 ms per frame over 7 runs of 1000 frames (4.92 to 5.03 ms).
- Step 2b (subagent): new `engine/.../drawables/BodyInterpolation.kt` (`IDrawOffsets`, `interpolation` flag, 1-tile guard and its `TODO`). It reads `stepSerial`, `alpha` and `ppm` from `WorldSystem` directly: no step-clock interface, because `WorldSystem` is its only source. `WorldSystem` drops `IDrawOffsets`, `getDrawOffset` and `interpolation`. The game holds `bodyInterpolation`, which `SpritesSystem`, `CameraDrawOffset` and the `I` toggle use. `WorldDrawOffsetsTest` became `BodyInterpolationTest` (same 17 cases). `engine:test` 718, `core:test` 2620, 0 failures.
- Steps 3-6 (subagent): `WorldWrap` (`region`, `wraps`, `maxWraps` lambdas, `UNLIMITED_WRAPS`); `IBody.wrapCount` and `wrapBy(dx)` (moves the bounds and the step start, adds 1 to the count); `WorldSystem(wrap = null)`. New spec `WorldWrapTest`, 21 cases. `engine:test` 739, `core:test` 2620, 0 failures. `TODO` at the `COLLIDE_DOWN_ONLY` friction add in `MegaCollisionHandler`.
  - Plan change: the wrap runs after the whole `postProcess` + `endStep` loop, not between them per body. Every `postProcess` (also one that reads another body) sees the position before the wrap, and the no-region order of calls is unchanged. `wrapBy` moves the step start, so the step move and the draw offset are the same.
  - Plan change: the seam contacts and the seam collisions are separate loops over the wrapping bodies, after the normal loops (no lookup per fixture; order stays deterministic).
  - Accepted gap (Claude): a body at its cap gets no seam contacts, also just after its last wrap. `TODO` in `WorldSystem`, backlog candidate in `docs/TODO.md`.
  - Measurement after step 6 (`engine:allocationTest`): `WORLD_ALLOC` 16 bytes per frame (same as before), new permanent case `WORLD_WRAP_ALLOC` 16 bytes per frame, `SNAPSHOT_ALLOC` 0. Timing, no wrap: median 4.98 ms per frame (before 4.95). Timing with a wrap region: 7.62 ms per frame, a worst case (a region narrower than the 200-body grid, every fixture a filter key); temp specs and results in `temp/109a-baseline/`.

## 2026-11-20

- Step 7 (subagent): `GameSprite.wrapOffsetX` (0 = none); `draw` draws the vertices a second time at `drawOffsetX + wrapOffsetX`. The zero-offset draw calls `drawer.draw(texture, getVertices(), 0, VERTICES_SIZE)` in place of `super.draw` (same call as `Sprite.draw`; `super.draw` from a helper looped under MockK `spyk`). `SpritesSystem(wrap = null)`: region once per frame, filter once per entity; the wrap count comes from the entity's own `BodyComponent` (0 with no body). New spec `SpriteWrapOffsetTest`.
- Fix (Claude): the cap gap of steps 3-6 also showed on the draw side: at its last wrap a shot lost the half of its sprite at the edge it came in through. New `IBody.lastWrapDx` (set by `wrapBy`, cleared by `reset()`) and one shared rule `WorldWrap.seamShift(...)`, used by the seam contacts, the seam collisions and the far-half draw: under the cap, the far side of the edge that the box crosses; at the cap, only the edge that the last wrap came in through. The `TODO` in `WorldSystem` and the backlog candidate in `docs/TODO.md` are removed. Tests: 2 seam rule cases, 2 seam contact cases at the cap, 1 sprite case. `engine:test` 761, `core:test` 2620, 0 failures; `WORLD_ALLOC` and `WORLD_WRAP_ALLOC` 16 bytes per frame.
- Steps 8-9 (subagent): new package `core/.../world/wrap/`: `WrapPolicy` (players by `isPlayer`, `IProjectileEntity` with `SHOT_MAX_WRAPS = 1`, 3 phase-2 `TODO`s), `WrapRooms.kt` (`isWrapRoom()` as the one read of `ConstKeys.WRAP`, `LevelState.wrapRegion(out)`, `checkWrapRoomWidths` called from `GameRoomsLayerBuilder`), `InvalidWrapRoomWidthException`. One `WorldWrap` in `createGameEngine()` for `WorldSystem` and `SpritesSystem`. Guards with `TODO`s for 111 in `CameraRoomScan`, `CameraRoomExits` and the `NEXT_ROOM_REQ` handler (refused with an error log; no test, the handler needs the full screen). 6 D2 `TODO`s in `world/contacts/rules/`. 10 new core cases (`WrapPolicyTest`, `WrapRoomsTest`, 1 in `CameraRoomPartsTest`, 1 in `CameraRoomExitsTest`). `engine:test` 761, `core:test` 2630, `core:smokeTest` 475, 0 failures.
  - Mutations (1 batched `core:test` run): scan guard removed, exits guard removed, `SHOT_MAX_WRAPS = 2`, `isPlayer` replaced by `is IPlayer`, width check `!=` to `<`. Each failed its own case; code restored.
  - Decision (subagent): the `CameraRoomExits` guard also removes the 2-player exit walls in a wrap room, because a wall at a side exit would stop the wrap.
  - Accepted edge case (Claude): a transition into a wrap room sets the region at once, so a player whose center is still past the edge wraps during the pan. `TODO` for 111 at `wrapRegion`. The test map has one room.
  - Checked in the code: a pooled shot starts each spawn with `wrapCount` 0 (`destroyNow` resets `BodyComponent`, which resets the body).

## 2026-11-20 — step 10 (automated checks, review before the play test)

- Automated checks: `engine:test` 764, `core:test` + `core:smokeTest` 3105, `lwjgl3:jar`, `detektMain`, `core:verifyRandomRule`: all pass. `WorldAllocationMeasureTest`: `WORLD_ALLOC` 16, `WORLD_WRAP_ALLOC` 16, `SNAPSHOT_ALLOC` 0 bytes, same as the baseline.
- Review (1 Sonnet subagent, no game launch). Findings and actions:
  - Contract: the arena was mirror-symmetric, so the check "stand across an edge" could not fail (the real-side half always stood on a block). Fixed: removed the right high ledge (block 7, x=480 y=192) and its tiles. A player across the right edge at that height now stands only on the left ledge through the seam.
  - Doc: `SCOPE.md`, `PLAN.md` step 4 and `NOTES.md` said the wrap runs before `endStep`; the code wraps after it (plan change of step 4). Wording fixed.
  - Style: `WorldSystem` had a function and a property both named `wrapBodies`. Function renamed to `applyWraps`.
  - Not changed: `NEXT_ROOM_REQ` guard has no test (safe by reading: last branch of the `when`); `SpritesSystem.getEntityWrapRegion` writes fields read by `getWrapOffsetX` (works); the room supplier stays set after the level screen (no effect); in pvp a dead slot 1 stays dead until slot 0 dies (`slotRespawn=false`, out of scope).
  - Root `CLAUDE.md` example `--level DESERT_MAN --room room4`: `room4` has no player spawn. A master doc fix, not done in this branch.
- Concrete manual check rooms: `REFACTOR_TEST_047` room `coop_wide`; real stage: `DESERT_MAN` room `room6` (horizontal neighbour `room7`).

## 2026-11-20 — play test round 1

- Owner (1-player check): with Mega Man across an edge, facing out, every shot vanished after a few frames. Cause: the shot (`ABSTRACT`, velocity set only in `preProcess`) spawned fully past the edge, so `defaultDoUpdate()` saw it outside the camera; the world skipped it, it never wrapped, and the off-camera cull took it after 0.05 s. The far half drew for those frames. `ChargedShot` has the same pattern.
- Fix (owner chose it over a wrap case in `defaultDoUpdate()` or a velocity set in each shot's `onSpawn`): `IBody.overlapsGameCam()` is wrap-aware. When the body does not overlap the view, it also tests the body shifted by the seam rule (`seamShiftOf` in `WrapRooms.kt`, which calls `WorldWrap.seamShift` with `WrapPolicy`). No wrap region or no wrap filter gives a shift of 0, so the no-wrap path does not change.
- Test: `WrapCameraOverlapTest` (12 cases): a wrap room, rooms with `wrap=false` and no `wrap`, and no room; shots and an entity that does not wrap, inside and past each edge; the cap case. `core:test` passes.
  - Mutation: the seam view test removed: 2 cases fail. The `WrapPolicy.wraps` check removed: no case fails, because `maxWraps` gives 0 for such an entity and `seamShift` then gives 0 (equivalent mutation; the check stays for the same filter as `WorldSystem`). Code restored.
- Checks to run again: 1-player arena, and the shot checks of the pvp run.
- Play test (owner, 2026-11-20): all manual checks pass, with the shot fix. Diff review: 0 threads; owner says the review is done.
