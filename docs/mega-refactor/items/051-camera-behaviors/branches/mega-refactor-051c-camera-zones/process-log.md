# Process log: mega-refactor-051c-camera-zones

## Status

- State: merged into `mega-refactor-051-master`
- Goal: Tiled camera zones, players-only edge blocks and `CrusherComponent`; levels with no zones play the same
- Done: `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` approved; branch start.
- Next: none. `051d` starts from the parent.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- No branch exists yet. The folder and a draft `docs/SCOPE.md` were prepared on `master`.
- Owner approved `docs/SCOPE.md` and accepted the recommendations on its open questions.
- Owner approved `docs/CONTRACT.md`.
- Owner approved `docs/PLAN.md`.
- Plan change (owner question): step 9 gets a temporary side-by-side suite for `GravityBlock`, so that the move is
  proved 1:1. The contract lists it.
- Step 1 (at `dde141861`, after `clean`): engine 585, core 337, 0 failures, 96 unique warnings.
- Step 3: `Temp051cCameraEquivalenceTest` (6 tests) and `Temp051cFrozenCameraManager`, from the `051b` suite. New cases:
  a cross-axis snap with `DesertMan` geometry and an offset chain in 4 directions; rooms smaller than the view; a reset
  every 37 frames. Mutation check: max wins, no reset, tracking during a pan, no viewport swap. All 4 fail the suite.
- Step 4: `CameraRoomScan`. Step 5: `CameraController.reset()`, 3 call sites.
- Step 6: `CameraLimits`, `CameraLimitStage`. Mutation check: no prior room in the cut, and the clamp order; both fail
  the suite. Short launch of `INTRO_STAGE`: no exception.
- Plan changes in step 6: (1) the stage runs after the follow, because the snap rule needs the follow's result. (2) No
  `CameraRoomLimitRule`: the room is the base and the cut of the stage, so `ICameraLimitRule` starts with `CameraZones`.
  (3) A pin resolves to a view-sized range in the stage, so the ease and the clamp handle only ranges.
- Step 2: a subagent added the rooms and `camera_sections`. The map grew at the bottom, so old rooms have a game y that
  is 98 tiles higher; old objects keep their Tiled coordinates. `zone_shake_entry` and `edge_rotated_entry` are extra rooms,
  because `RoomShaker` and `megaman_direction` act only after a room change. Check 14 starts at `zone_shake_entry`.
- Step 7: the ease, the snap rule, the hold predicate and `CameraController.settledPosition`. `ICameraLimitRule` starts
  here, with a fake rule in the test, one step before `CameraZones`. Default ease speed: 6 tiles per second, from the X6
  2 px/frame in a 320-wide view. The suite passes.
- Step 2 launches (the step 6 code, from a saved jar): 11 new-room starts and `basic`, 18 s each, no exception.
- Step 8: `CameraZones`, `CameraZone`, `CameraSectionsLayerBuilder`, 7 tests. Levels with no layer get an empty list, so a
  new level clears the zones.
- Step 9: `CrusherComponent` in `entities/state/`, and `GravityBlock` moved to it. Plan change: no `instant` setting,
  because both callers kill at once and a setting would have no caller. Sides are relative to the body, because the
  fixtures and senses rotate with it; `worldToBodySide` maps a world edge. `Temp051cGravityBlockCrushTest` passes on 5000
  seeded states, with more than 500 kills and 500 misses. A mutation (feet for head) fails it.
- Step 10: `CameraEdgeBlock`, `CameraEdgeBlocks`, and `blockedEdges` on the limits. Plan change: the manager is an effect,
  not a mover, because a mover does not run during a pan. It removes the blocks during a pan and until the player is in
  play, because a pan moves Megaman directly. A crush skips a body with `collisionOn` off, for a teleport.
- Step 11: root `CLAUDE.md`.
- Step 12 (after `./gradlew clean`): `engine:test core:test lwjgl3:jar` passes. engine 594, core 372 (7 temporary), 0
  failures. 96 unique warnings, no new one. The contract greps find nothing. One new unchecked cast warning in
  `MegaLevelScreen` was fixed before this result.

### 2026-10-30 (play test start)

- The owner's first check crashed at `Megaman.kt:572`. Two faults: spawn `23` was inside the map's start area and replaced
  each launch spawn, and the room-transition velocity read failed when `edge_rotated` changed the gravity axis. See the bug
  candidates in `docs/TODO.md`.
- Correction: the 13 room launches at step 10 did not prove the rooms, because the launches started at spawn `23`.
- Correction: the "Permanent tests" box was ticked with 3 tests missing: a pin beats a range, a room change snaps, and no
  zone part gives the same result. They now exist. A mutation that removes the room-change snap fails the suite.
- Map: spawn `23` moved to Tiled (192, 3664), in the same room and above the start area. x 528 failed: Megaman entered
  `edge_rotated` with no input and crashed. New `sensors` layer with a `Death` sensor above the `zone_overlap` overlap, and
  `speed=2` on `zone_overlap_a`. A zone's `speed` acts only while the focus is in that zone, so it sets the ease into the
  zone, not out of it.
- Contract: checks 9, 10, 14 and 18 reworded, after owner approval of the coverage review. Check 18 now starts at
  `edge_rotated_entry` and needs the `084c` fix, so `084c` merges first (owner decision).
- Launches: 12 rooms and a plain start, 13 s each, no exception. `basic` and `zone_overlap` logged the correct spawn and no
  spawn change. The map scan finds no non-first spawn in the start area of any live map.
- `engine:test core:test lwjgl3:jar` passes: engine 594, core 377 (7 temporary), 0 failures.

### 2026-10-30 (`084c` merged)

- Merged `master` into this branch (`1aec24a16`), with no conflicts. It holds `084c` (`1d6a1ee52`) with the room-carry fix,
  so check 18 is no longer blocked. The carry bug candidate is closed in `docs/TODO.md`.
- After `./gradlew clean`, `engine:test core:test lwjgl3:jar` passes: engine 594, core 398 (7 temporary), 0 failures, 96
  unique warnings.

### 2026-10-30 (basics first)

- The owner found `zone_overlap` hard to judge: it is an edge case, and the checks had no step up to it. Owner decision: test
  rooms start from the basics.
- New room `zone_hold`: one zone in a long room, spawn `25`. A subagent built it. `min_x` removed from `zone_overlap_b`, so
  the overlap takes its left stop from `zone_overlap_a` and its right stop from `zone_overlap_b`, and crossing tile 23 no
  longer moves the camera.
- Contract: the manual checks are reordered into basics, zones with other camera features, and edge cases, so there are
  now 19 checks.
- `engine:test core:test lwjgl3:jar` passes. Launches of `zone_hold`, `zone_overlap` and `edge_rotated_entry`, 13 s each,
  with no input: no exception, the correct spawn in the log, no spawn change. A map scan finds no non-first spawn in the
  start area.

### 2026-10-31 (limit objects)

- Scope change (owner approved): zone limits are references to Tiled objects. A `min_*` property reads the low edge and a
  `max_*` property reads the high edge. Numbers are gone, with no numeric form, because world tiles with y up were hard to
  read in Tiled, and because no real level uses zones.
- The pin had no caller after this change, so `setPin` and the pin state in `CameraAxisLimit` are removed. A locked axis is a
  range of one view size. `resolve` no longer takes a view size. In a larger rotated view, "min wins" holds the low edge,
  where the pin held the center. `051d` adds its own form.
- Map: a script converted the 14 zones and added 12 limit objects (a loop over map data, not a new room). A readback of
  the file gives the same limits as the old numbers. `zone_lock_y` now covers the full room height from tile x 78, and its
  view object is `zone_lock_view`, because a focus above the old zone top left the zone and the view at the same moment. The
  owner found this in play.
- `engine:test core:test lwjgl3:jar` passes: engine 594, core 398 (7 temporary), 0 failures. Launches of `zone_lock`,
  `zone_hold` and `edge_block`: no exception, the correct spawn, 14 zones. A launch does not prove the limit values; the play
  test does.

### 2026-10-31 (play test passed; merge)

- Owner play test: "tests pass" (plan step 13).
- Plan step 14: the 3 temporary suites are deleted. After `./gradlew clean`, `engine:test core:test lwjgl3:jar` passes:
  engine 594, core 391, 0 failures, 96 unique warnings, the baseline count.
- Owner decision: this branch merges into a new parent `mega-refactor-051-master`, not into `master`, and `051d` merges
  there too, so that the owner can play the 2 branches together before `master`.
- Overlap at merge: `031e` also changes `Constants.kt` and `MegaLevelScreen.kt`. `084b` shares no code file. `master` changed
  only in docs since this branch took it in.
