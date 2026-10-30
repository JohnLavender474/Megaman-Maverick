# Process log: mega-refactor-051b-camera-behaviors

## Status

- State: merged
- Goal: The camera is built from behavior components (room bounds, focus follow, rotation); `MegaLevelScreen` talks
  to a camera control interface, not to `CameraManagerForRooms`; `GameCameraState` goes. The game plays the same.
- Done: all plan steps; merged into `master` on 2026-10-30 (squash `7fe00b8b8`).
- Next: none.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- Branch start, from `master` at the `051a` merge. Created the branch and its worktree
  `.worktrees/mega-refactor-051b-camera-behaviors/`.
- Scan (`docs/NOTES.md`, "Scan"), research by a subagent on X6 and other engines (`docs/NOTES.md`, "Research";
  X6 facts spot-checked), overlap with `084a` and `031d`. Wrote the draft `docs/SCOPE.md`. 1 backlog candidate.
- Owner approved `docs/SCOPE.md` and the 4 answers (`docs/NOTES.md`, "Decisions"). Bounds move to the controller
  (`coerceIntoBounds` leaves `RotatableCamera`). Room scan of the live TMX files. Wrote the draft `docs/CONTRACT.md`.
- Owner decision: the view is separate from the controller. `docs/SCOPE.md` item 5 changes: new
  `services.cameraController` for the smooth-move requests. `CameraView` waits for `051e`. The contract is not affected.
- Owner approved `docs/CONTRACT.md`. Wrote the draft `docs/PLAN.md`.
- Owner approved `docs/PLAN.md`.
- Step 1 (baseline, at `2896f0424`, after `clean`): engine 575, core 274, 0 failures, 103 unique warnings.
- Step 2: a subagent built `RefactorTest051.tmx` and `REFACTOR_TEST_051`. `--room` works for `basic`, `gap_left`,
  `to_gravity`, `to_success`. Short launch of each on the baseline code: no exception. `gravity_room` uses
  `megaman_direction=down`; the default gravity is `UP`, so this is a real change. `basic_up` has a 3-row floor. Entering
  `gap_right` from the gap gives `SET_TO_ROOM_NO_TRANS`, not a transition, as the old code does.
- Step 3: `Temp051bFrozenCameraManager`, `Temp051bCameraEquivalenceTest`. The first run stalled: `GameObjectPools` frees
  only when the game loop reclaims, so the test now calls `performNextReclaim()` each frame.
- Step 4: engine parts and 10 tests. Plan detail: the pan starts in the frame in which the delay ends (the old
  behavior).
- Step 5: `CameraRoomBounds`, `CameraRoomClamp`, `CameraRotationStep` (the one-line adapter), 5 tests. The suite passes
  against the frozen manager. Mutation check: 4 deliberate defects (no cancel at reset, no viewport swap, x-only broken,
  no x snap in a gap). The x-only defect survived first, because rooms one view tall pin y; with varied room sizes all 4
  fail the suite.
- Step 6: `LevelRoomHandler`, generated from the `MegaLevelScreen` callbacks. Check script: 108 lines identical, except
  receivers. `LevelRoomHandlerTest` (4 tests). core 286, 0 failures.
- Step 7: `084a` and `031d` merged into `master`, then `master` into this branch, no conflict. engine 585, core 313.
- Step 8: `game.cameraController`, `game.services` (a `lateinit` field, for the room handler), `MegaServices.cameraController`.
  The level screen builds the parts. `CameraManagerForRooms`, `CameraShaker`, `coerceIntoBounds` and
  `focusSnappedAway` are gone. engine 585, core 313. Short launch of `INTRO_STAGE`: no exception.
- Step 9: root `CLAUDE.md`.
- Step 10 (after `./gradlew clean`): `engine:test core:test lwjgl3:jar` passes. engine 585, core 313 (3 temporary), 0
  failures. 96 unique warnings: no new one, and 7 fewer than the 051b baseline (from `084a`). The contract grep finds
  nothing in `core/src/main`, `engine` and `lwjgl3`; the frozen copy in the test sources still names the old code until
  step 12. Callback check: 108 lines identical (step 6; the file has not changed since).
- Step 11: the owner ran the manual checks. Result: all tests passed.
- Step 12 (after `./gradlew clean`): deleted `Temp051bCameraEquivalenceTest` and `Temp051bFrozenCameraManager`.
  `engine:test core:test lwjgl3:jar` passes. engine 585, core 310, 0 failures. 96 unique warnings. The contract grep
  finds nothing in the sources of `core`, `engine` and `lwjgl3`.
- Overlap before the merge: see `docs/NOTES.md`. `master` has no code change since this branch last took it in.
- A stalled test run from step 3 stayed alive in this worktree until step 12. It held no result and changed no file;
  it was stopped before the final build.
