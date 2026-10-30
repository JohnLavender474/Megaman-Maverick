# Plan: mega-refactor-051b-camera-behaviors

How to do the work in `SCOPE.md` (approved 2026-10-30), checked by `CONTRACT.md` (approved 2026-10-30).
`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`MLS` = `C/screens/levels/MegaLevelScreen.kt`.

- Status: approved
- Approved: 2026-10-30

## Design facts from the old code

The old `update()` mixes room logic and follow in one order. The parts must keep that order exactly:

1. A pending reset wins over all else: clear the rooms and the transition, snap to the focus, find the room, call
   `onFirstUpdateAfterReset`, and do nothing more in that frame (no clamp).
2. Else, a transition holds the camera for the frame.
3. Else: track the room, then follow, then clamp, then look for a room change. The room change only sets the values;
   the transition starts in the next frame.
4. The follow has 3 modes in step 3: x and y (the focus in the current room), x only (the focus in no room, with a
   prior room), and none (no room and no prior room).

So the controller has 2 ordered lists:

- **Movers:** run in order, and a mover can hold the camera for the frame, which ends the list. The order is
  `CameraRoomBounds` (reset, tracking, sets the follow mode), `CameraRoomTransition`, `CameraFollow`, `CameraRoomClamp`
  (clamp and room-change scan).
- **Effects:** always run, after the movers: the rotation adapter (`camera.update(delta)`), `CameraShake`, the
  smooth-move stop rule. This is the old order at `MLS:985-994`.

A context omits what it does not need: with only `CameraFollow`, the mode stays "x and y".

Traps:

- There is no `CameraController.reset()` that forwards to all parts. `CameraShaker.reset()` restarts the timers, so it
  starts a shake. The level screen makes the same explicit resets as now, each at its old place.
- The old flag has 3 kinds of clear (`MLS:490,605,1140`, the rotation end, the stop rule). Each becomes
  `stopSmoothFollow()` at the same place.
- `transitionInterpolation` is put in event properties. `engine` has no pools, so the part has
  `getTransitionInterpolation(out)`, and `core` passes a pooled vector, as now. A shared vector could change before
  `events.run()`.
- `focus`'s setter moves the camera and sets the reset flag (`MLS:285`). This stays.
- `IFocusable` moves to `engine`, because `CameraFollow` needs it.

## New API and its first callers

| API | First production caller |
|---|---|
| `E/camera/ICameraPart`, `CameraController` (2 ordered lists, `getPart`, `update`) | `MLS` (build, frame), `MegamanMaverickGame.cameraController` |
| `E/camera/CameraFollow` (focus, x limits, mode, `startSmoothFollow`, `stopSmoothFollow`, stop rule) | the controller, `MLS`, the 7 entity requests |
| `E/camera/CameraRoomTransition` (the pan, `getTransitionInterpolation(out)`, callbacks) | the controller, `MLS` (`NEXT_ROOM_REQ`), `LevelRoomHandler` |
| `E/camera/CameraShake` | the controller, `MLS` (`SHAKE_CAM`, `PLAYER_SPAWN_INIT`) |
| `C/screens/levels/camera/CameraRoomBounds`, `CameraRoomClamp` | the controller, `levelState` suppliers, `MLS`, `LevelRoomHandler` |
| `C/screens/levels/LevelRoomHandler` | `MLS` |
| `MegaServices.cameraController`, core `startSmoothFollow()` and `stopSmoothFollow()` extensions | the 7 entity requests |

`CameraRoomClamp` is a small part next to `CameraRoomBounds`, added to the approved names because the clamp runs after
the follow. If `084a` is not on `master` at step 8, stop and tell the owner.

## Steps

### 1. Baseline

- Changes: none.
- Check: `./gradlew engine:test core:test lwjgl3:jar`. Write the test counts and the unique warnings in the process log.
- Done: yes

### 2. Test map

- Changes: a subagent builds `assets/tiled_maps/tmx/RefactorTest051.tmx` with the `level-builder` skill, with painted
  blocks and these rooms: `basic` (with horizontal and vertical neighbors), `gap_left` and its neighbor over a gap,
  `to_gravity` and a room with `megaman_direction`, `to_success` and a room with `event=success`. Add
  `REFACTOR_TEST_051` of type `TEST_LEVEL` to `LevelDefinition`.
- Check: a short launch of each room on the baseline code: no exception. `LevelCompletionScreensTest` updated if it
  needs the new level.
- Done: yes

### 3. Temporary equivalence suite on the old code

- Changes: core test sources: `Temp051bFrozenCameraManager` (the old manager, `coerceIntoBounds`, the shaker update, the
  flag clear, in the old order) and `Temp051bCameraEquivalenceTest` (the cases in `CONTRACT.md`). The "new" side runs
  the current classes for now.
- Check: the suite passes on the old code. It must reach each case: assert that the paths hit a transition, a gap and
  a reset.
- Done: yes

### 4. Engine parts

- Changes: `ICameraPart`, `CameraController`, `CameraFollow`, `CameraRoomTransition`, `CameraShake`, `IFocusable` moved
  to `engine`. Permanent engine tests: the parts and the order, and a controller with only a follow part.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes. Commit.
- Done: yes

### 5. Room parts in core

- Changes: `CameraRoomBounds`, `CameraRoomClamp` (with the logic of `coerceIntoBounds`, which needs the direction from
  the camera). Permanent core tests.
- Check: the temporary suite's "new" side runs the controller with the parts, on a plain test set-up. It must pass.
  Commit. If it fails, fix the part, not the frozen copy.
- Done: yes

### 6. Room handler

- Changes: `LevelRoomHandler` with the 5 callback bodies from `MLS:286-409`, not yet called.
- Check: the check script (the same lines, except receivers). `LevelRoomHandlerTest`. Commit.
- Done: yes

### 7. Merge `master`

- Changes: `git merge master` in the worktree, with `084a`.
- Check: the build and tests pass. If `084a` is not merged, stop and tell the owner.
- Done: yes

### 8. Switch the game to the controller

- Changes: `MegamanMaverickGame.cameraController`; `MegaServices.cameraController`; `TestServices`; the level screen
  builds the parts, uses the room handler, and calls `cameraController.update(delta)` at `MLS:985`; the 4 event cases,
  the level start and end, the suppliers; `startSmoothFollow`/`stopSmoothFollow` at all flag writes; the core extensions.
  Delete `CameraManagerForRooms.kt`, `CameraShaker.kt`, `coerceIntoBounds`, `GameCameraState.focusSnappedAway`.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes with the temporary suite; the contract grep is clean; a
  short launch of `INTRO_STAGE`. Commit.
- Done: yes

### 9. Docs

- Changes: the root `CLAUDE.md` (the `camera` line, the holder list, the room transitions line).
- Check: the grep of `CameraManagerForRooms` and `focusSnappedAway` in `CLAUDE.md` finds nothing. Commit.
- Done: yes

### 10. Automated contract checks

- Changes: none.
- Check: all items under "Automated checks" in `CONTRACT.md`, after `./gradlew clean`, with a warnings comparison.
- Done: yes

### 11. Owner play test

- Changes: none.
- Check: the owner runs the manual checks in `CONTRACT.md`. Write the result in the process log.
- Done: yes

### 12. Remove the temporary suite

- Changes: delete `Temp051bCameraEquivalenceTest` and `Temp051bFrozenCameraManager`, in the last commit before the merge.
- Check: `./gradlew engine:test core:test lwjgl3:jar` passes. Write the counts in the process log.
- Done: yes
