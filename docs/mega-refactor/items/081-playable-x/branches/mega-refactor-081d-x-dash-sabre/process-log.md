# Process log: mega-refactor-081d-x-dash-sabre

## Status

- State: merged
- Goal: phase 3 of X (ground dash, dash jump, dash wall kick), less knockback, the owner's X tuning and the feel fixes; the sabre moved to `081e`
- Done: all steps; play test passed 2026-11-07
- Next: none (merged into `master`)
- Brief: https://claude.ai/artifact/6V3KDMvtCVHhpnDQJkh6SX
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-06

- Branch start on `master` after `081c` merged (`61460cd31`, docs `91a079e57`) and `047g` merged (`3e8655f02`). Owner (2026-11-06): "fold in a few additional changes to the next phase": the owner's uncommitted X tuning in the main checkout (values, buster spawn point), the Z-sabre slash, and more resistance to the knockback when X takes damage.
- The owner's uncommitted X tuning in the main checkout was applied as commit `a15fa6184` (values, buster spawn point,
  blank lines); `core:test` passes with it. The main checkout keeps the same edits, uncommitted.
- Scan by a subagent: `temp/081d/scan.md`. `SCOPE.md`, `CONTRACT.md`, `BRIEF.html`.
- Owner (2026-11-06): D1 to D4 as recommended: the sabre to `081e` (`UP` + `B`), the knockback like Megaman's, the
  ground dash on `R` and `DOWN` + `A`. `SCOPE.md` and `CONTRACT.md` approved.

## 2026-11-06 (work)

- Step 2: X builds its own `KnockbackComponent` from `MegamanXValues.KNOCKBACK_X` (8) and `KNOCKBACK_Y` (5), with
  `SpringHead` exempt as before. X's body turns on the side friction only while damaged, with
  `KNOCKBACK_DRAG` 6/s (Megaman's `STANDARD_RESISTANCE_X`): about 1.3 tiles in the air, less on a floor (block
  friction). After the damage the friction is off again. Test added in `MegamanXTest`.
- Step 1: a subagent added room `ground_dash` (spawn `5`, columns 182-236; script `temp/081d-map/add_room.py`): a
  20-tile floor, a 6-tile pit with a floor 3 down and a step back, 8 tiles of floor, a 2-high ledge 5 wide, 12 tiles
  of run-out, the right wall. The XML parses; the analyzer output is in `temp/081d-map/`.
- Steps 3 to 5: `BehaviorType.GROUND_DASHING`; X's `GroundDash` behavior in `MegamanXMoves.kt` (start on `R` or
  `DOWN` + `A` on the ground; 0.083 s start delay; 12.5 t/s down to 8 t/s at 21.1 t/s^2; 0.5 s; then the end slide of
  0.283 s; ends on the opposite side, a wall, off a ledge or a jump). It runs before the jump, so a jump during the
  dash (after the delay) or with `R` held is a dash jump: `dashSpeedInAir` (the run uses 8 t/s in the air) and no air
  dash after it. A kick with `R` held pushes away at 8 t/s and also sets the dash air speed. An air dash started on
  `R` starts at 12.5 t/s (flat). The body keeps the side speed during the ground dash. Animation key `dash`.
- Bug found by a test: the landing reset of `airDashUsed` and `dashSpeedInAir` ran after the behaviors in the jump
  frame, while X was still over the ground, so X could air-dash after a dash jump. The reset now waits until X is
  over the ground and not jumping.
- New tests in `MegamanXMovesTest` (12). `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes:
  `engine` 639, `core` 1121, `smokeTest` 450.
- Step 6: a `TODO` at the `DOWN` + `A` dash start (it also drops X through a one-way block). The `081e` branch folder
  is prepared on `master` at the merge (D1, D2 in `docs/NOTES.md`).
- Step 7: 14 mutation checks (`temp/081d/mutate.py`). 13 caught. "The landing reset waits for the jump end" was not
  caught (the script reported it as a compile error by mistake: a warning text has "e: "), because the test rig copied
  the reset. The reset is now `resetAirMovesOnLanding()`, called by X's updatable and by the rig; a test fails
  without the jump check.
- Review before the play test, agent 1 (branch diff). Fixes, each with a test that fails without it:
  - Bug: when a ground dash ended with the side held, X stopped for 1 frame and then did the walk start. The dash end
    now hands over to the walk (`runTime` past the walk start, `running`, walk speed).
  - Bug: a plain kick (no `R`) after a dash jump kept the dash air speed. The kick now sets `dashSpeedInAir` to
    whether `R` is held; `airDashUsed` is set only with `R`.
  - Risk: the knockback drag started 1 world step late, and a friction flag could stay on after a gravity change. The
    drag value is now fixed by direction, and only the side axis friction switches with `damaged`.
  - Nit: damage clears `dashSpeedInAir`. The stale `TODO` on the dash button speed is removed.
  - Kept: a dash jump from a `DOWN` + `A` dash needs a side held (X6 keeps a dash only while the dash input or a side is
    held); in the contract as a play-check note.
  - Logged: the owner's commit `a15fa6184` reordered 2 imports in Megaman's `SpritesComponent.kt` (no behavior change);
    kept as a small clean-up, not a Megaman change.
- Review before the play test, agent 2 (the manual checks): every check runs with the owner's tuning, and X can reach
  the gate of `X6_INTRO` `room1` with the 1.5-tile body. Fix: a 1-frame stop after a dash jump or a dash off a ledge
  (the run counted the ground dash as active on the takeoff frame); the run now uses `holdsSideSpeedForRun`. Test
  added. The contract has its play-check notes (a plain jump plus an air dash crosses the pit; walk over the ladder
  top; the sabre block at tile 88 needs a jump shot).
- After the fixes: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.

## 2026-11-06 (play test feedback)

- Owner, at the `ground_dash` play check: with `DOWN` + `A` held for a dash, a second `A` press must give a dash jump,
  also with `DOWN` held, while the dash time runs. Done: `JumpParams.downBlocksGroundJump` (Megaman `true`, his old
  rule; X "not while the dash time runs"); the dash jump counts from the dash start (also in the start delay) until
  the end slide. The `A` press that starts the dash does not also jump (the dash is jumpable from its next frame).
  2 new tests; the dash-jump test fails with Megaman's rule. `MegamanJumpTest` passes unchanged.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- Owner (2026-11-06, at the play test): "proper X controls are incompatible with the NES controller". D4 reversed:
  the ground dash starts only on `R`; `DOWN` + `A` and `JumpParams.downBlocksGroundJump` are removed (`PlayerJump.kt`
  and Megaman's `JumpBehavior.kt` are again as on `master`). The dash jump window (dash start to end slide) stays.
  The 3 `DOWN` + `A` tests are replaced by 1 test that `DOWN` + `A` starts no dash. The 4 Gradle tasks pass.
- A subagent reviews X's movement logic against `mmx-x6` (logic, not values); notes in `temp/081-x6-review/`.

## 2026-11-06 (X6 logic review)

- A subagent compared X's movement logic with `mmx-x6` (`Player.java`, `TerrainMover.java`, the RE notes in
  `mmx-x6/docs/re/findings/players/`). 3 of its X6 claims checked by hand (`Player.java:731`, `:1099-1104`,
  `:1111-1118`). Owner (2026-11-06): match X6 in all of them (list in `docs/NOTES.md`, "Decisions").
- Shared code: `WallSlideParams` got `allowedWhileJumping` and `holdsWithoutInput`, `AirDashParams` got `decel` and
  `minSpeed`. Megaman passes `false`, `false`, `0`, `0`: his paths do not change, and his baseline tests pass.
- X: `MegamanXMoves.kt` (the rules above), `resetAirMovesOnLanding` (landing edge resets the walk start),
  `endKickLockOnBlock`; `MegamanX.airborne`.
- 11 new or changed tests; 11 mutation checks (`temp/081d/mutate2.py`), each caught. The 4 Gradle tasks pass:
  `engine` 639, `core` 1133, `smokeTest` 450.

## 2026-11-06 (dash on SELECT)

- Owner: the dash key change to `L` clashed with `SELECT`; the owner chose "SELECT is ALWAYS dash when playing as X",
  with a `TODO` that records the button changes. Done (see `docs/NOTES.md`, "Decisions"): X reads `SELECT` for every
  dash input; the `R` button is removed (no reader); `SELECT` never pauses for X. Tests changed from `R` to `SELECT`;
  the settings store tests use `SELECT` as the button missing from an old save. The 4 Gradle tasks pass.
- At the merge on `master`: remove the `R` button from `CLAUDE.md` (root), and record the change in `069` and `081`.

## 2026-11-07 (feel fixes, seam fix, brake pose)

- Owner answers (2026-11-06): "Apply all" (the X fixes), "Fix in 081d" (the block seam), "Yes to both" (walk speed for
  the kick-away and the push-off; the X6 jump top). The engine timing question went to item `134` (on `master`).
  Decisions and X6 sources in `docs/NOTES.md`.
- Shared code: `JumpParams` got `startPressed`, `downBlocksGroundJump` and `endKeepsFall`; Megaman passes
  `isJustPressed(A)`, `true` and `false` (his old rules). `BlockSideRule` keeps the touched blocks for each side
  fixture.
- X: `MegamanXMoves.kt` (`keepStartPresses`, `runPressedSide`, `GroundDash.braking`), `MegamanXController.kt` (run on
  the press frame), `MegamanXSprites.kt` (key order, the held kick pose, `dash_end`), `MegamanXValues.kt`.
- Art: `dash_end.png` (owner) and `dash_end_shoot.png` (Claude) copied from the main checkout; atlas packed again (39
  old regions with the same pixels; 2 new).
- Tests: 17 new (`MegamanXMovesTest` 14, `BlockSideRuleTest` 3). `core:test`: 1149, 0 failed.
- Mutation checks: `temp/081d/mutate3.py`, results in `temp/081d/mutation-results-3.txt`.
- Review before the play test, agent 2 (the manual checks): 5 rows pass. No room in the checks had a wall of 2
  stacked blocks for the seam fix. Fix: block `24` of `RefactorTest081.tmx` (`walls` room) split into 2 blocks (new
  object `47`; seam 3 tiles above the floor); a Megaman control row `WILY_STAGE_2 --room room9` (a stacked left wall).
- Review before the play test, agent 1 (the diff): 2 real findings and 2 low findings. Fixes:
  - after a plain jump, an `A` or `SELECT` press on the landing frame gave an air dash just above the floor. Owner
    (2026-11-07): "Ground move". X probes 1.5 nominal frames below the feet before an `A` or `SELECT` air dash; if X
    lands, the press is kept for the ground move; if the landing does not come, the air dash starts 1 frame later
    (`landsWithinFrame`, `pressHeldForLanding`; `TODO` for a frame longer than the probe, item `134`);
  - `BlockSideRule`: old touched blocks could stay after `WorldSystem.reset()` (no end calls) and hold the side
    sense. The body reset now clears the sets (`BodyComponentCreator`), and the end also drops blocks that are not
    live, are `NO_SIDE_TOUCHIE` or are slopes;
  - low: a press is kept only on a landing (not after a wall touch or a cutscene end);
  - low: a press is kept for 1 render frame, not 1 world step (`TODO`, item `134`).
- 7 new tests; 9 mutation checks (`temp/081d/mutate4.py`, `mutate5.py`), each caught. The 4 Gradle tasks pass:
  `engine` 639, `core` 1156, `smokeTest` 450.
- Second review of the fixes (`76c719a93`): the probe started at the body edge (the landing sense reaches 1.6 px
  below the feet, so a slow landing was missed), and it used the bounds before rotation in `LEFT` and `RIGHT`
  gravity. Fix: the probe is the shape of the feet gravity sensor, extended in the fall direction, with the sensor's
  filter (and no one-way block with `DOWN` held). A missing world container throws (fail fast). The 2 dead prune
  conditions in `BlockSideRule.end` are removed. The probe `TODO` also names moving platforms. 2 new tests; 1 mutation
  check (`temp/081d/mutate6.py`), caught. The 4 Gradle tasks pass: `engine` 639, `core` 1158, `smokeTest` 450.
- Owner play test: "tests passed", with no other remark. This is the merge approval. The diff review has no open
  threads.
- `master` changed after the branch start (the owner's X tuning, the dash end art, `047j`). It touched the same X
  files, so `master` was merged into the branch (`0fc24891a`). One simple conflict in `MegamanXValues.kt`: `master`
  added no lines at the end of the object, and the branch added the knockback and dash values; the branch block is
  kept. The 4 Gradle tasks pass after the merge: `engine` 639, `core` 1174, `smokeTest` 450, JAR built.
