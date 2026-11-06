# Process log: mega-refactor-081c-x-wall-slide-air-dash

## Status

- State: active
- Goal: phase 2 of X: wall slide (with the wall kick) and air dash
- Done: steps 1 to 8 (play test next)
- Next: the owner play test (`CONTRACT.md`)
- Brief: https://claude.ai/artifact/AJmyLtfrHVBmjHQekmA5Ru
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-06

- Branch start on `master` after `081b` merged (`30393d44d`, docs `896857c72`). Owner (2026-11-06): "start 081c: phase 2, wall slide and air dash".
- Scan by a subagent: `temp/081c/scan.md`. `SCOPE.md`, `CONTRACT.md` and `BRIEF.html` (from the template),
  published as a private Artifact page.
- Owner (2026-11-06): D1 "Megaman's input + R dash button" (a new answer: `A` in the air, `SELECT` as air dash, and
  a `DASH` button now), D2 to D4 as recommended. `SCOPE.md` and `CONTRACT.md` approved.

## 2026-11-06 (work)

- Step 1: a subagent added rooms `walls` (spawn `3`) and `dash` (spawn `4`) to `RefactorTest081.tmx` (now 184 x 16
  tiles; script in `temp/081c-map/`). The old right wall of `targets` moved to the right end of `dash`, so all rooms
  connect. `walls`: a 9-tile kick wall (10 does not fit under the ceiling), a 2.5-tile step (top half-tile not
  painted), a 4-wide chimney, a `no_side_touchie` wall (block 28, blue tile gid 28), a 4-tile face with a 2-tile
  opening that blocks the floor (the way on needs a kick). `dash`: pits of 3, 4 and 5 tiles with a floor 3 tiles down
  and a step back, and a 2-tile tunnel 6 long. The XML parses; the level-analyzer output is in `temp/081c-map/`.
- Step 2: baseline tests on the old Megaman code: `MegamanWallSlideTest` (16) and `MegamanAirDashTest` (21), with a
  shared test rig. They pass on the old code. Each class failed when 1 line of the old behavior was broken (the end
  bump; the A task at the slide end).
- Step 3: `players/PlayerWallSlide.kt` (`WallSlideParams`, `playerWallSlideBehavior`); `players/PlayerInput.kt`
  (`isRunSidePressed`, `Facing.runButton`), used by both controllers and the slide. `JumpParams` got
  `wallJumpAllowed` (Megaman passes "is wall sliding", the old rule) and `ignoreRelease` (Megaman passes false; X's
  kick lock comes in step 5). `MegamanWallSlideTest` and `MegamanJumpTest` pass unchanged.
- Step 4: `players/PlayerAirDash.kt` (`AirDashParams` with `startDelay`, `playerAirDashBehavior`), and the shared
  input reads `airDashInputPressed` and `airDashInputJustPressed` (`A`, or `SELECT` when the slot's select action is
  air dash). Megaman passes his rules (weapons, water, Rush Jet, the end bump, `aButtonTask`).
  `MegamanAirDashTest` passes unchanged.
- Steps 5 and 6: `entities/megamanx/MegamanXMoves.kt`: X's wall slide (grab, fixed slide, push-off on release, a new
  air dash), jump (a kick on a slide or on any wall touch in the air, away from the wall at 3.75 t/s, the 0.167 s lock
  with `ignoreRelease`), and air dash (hover 0.133 s, then 0.3 s at 7.734 t/s, no end bump, 1 per air, facing the held
  side, ended by the opposite side). Starts on `A` (or `SELECT` when the select action is air dash) through the shared
  `airDashInputJustPressed`, and on `DASH`. The body keeps the side speed during a kick lock, a push-off and a dash; the
  run input does nothing then and during a slide. New values in `MegamanXValues`. Animation keys `dash`, `wallslide`
  (its strip faces left, so the flip is inverted) and `walljump`, each with `_shoot`.
- `MegaControllerButton.DASH` (index 8, keyboard `U`), pad default `R1` (`ControllerMappingExtensions`), and the
  controls screen: the select-action row Y and its "up" target now follow the button count.
  `PlayerSettingsTest` and `PlayerSettingsStoreTest` counted 8 buttons; they now read the button count.
- New `MegamanXMovesTest` (23). `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes: `engine` 639,
  `core` 1073, `smokeTest` 449.
- Step 7: `players/README.md` shows the 3 shared moves with params and the input file, and the rule for what goes in
  the params.
- Step 8: 15 mutation checks (`temp/081c/mutate.py`), each caught by at least 1 test (the shared slide gate and
  gravity, the dash hover, end bump and select input, the wall jump speed; X's grab, push-off, kick side, kick lock,
  kick on a wall touch, 1 dash per air, the `DASH` button, the body side speed, no run during a slide).
- Review before the play test, agent 1 (branch diff): no change to Megaman found (wall slide, air dash, jump). Fixes:
  - Bug: no 0.167 s wall delay after a jump start (D3). New `wallDelayTimer`, reset on every jump; a kick on a plain
    wall touch waits for it. New test; it fails without the fix.
  - Bug: `DASH` and `SELECT` did nothing during a rising jump. Only an `A` press that started a jump in the same frame
    now blocks the dash; X's jump has `blocked = { AIR_DASHING }`. New test (the hover also ends the jump).
  - Risk: the push-off fired on any slide end. Now only when the wall side is released and X can move, and not when
    damaged or stunned. New test.
  - Risk: a change between an animation and its `_shoot` version restarted it (the grab or the hover frame came
    back). `onChangeKey` now keeps the elapsed time.
  - Risk: an old save with no `DASH` entry kept the default `U` (`R1`) also when another button already had it. The
    keyboard and pad loads now unbind such a default (`NO_CODE`, label "NONE"), and the keyboard read skips
    `NO_CODE`. 2 new tests; the clash test fails without the fix.
  - Nits: the first max-timer duration of the shared dash is neutral; a `TODO` that the dash button gives the same
    dash as `A` until phase 3.
  - Not changed: a shot during the kick lock goes toward the wall (X faces the wall, as D3 says); a question for the
    owner. `touchedWallSide()` prefers the left in a 1-body shaft.
- After the fixes: `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes (`engine` 639, `core` 1078,
  `smokeTest` 449).
- Review before the play test, agent 2 (the manual checks; it read the code before the agent-1 fixes): every check
  runs. X can now go from spawn `0` of `X6_INTRO` `room1` to the gate (route in the contract). Its 3 code findings (the
  dash during a rise, the wall delay, the old-save clash) were already fixed. The contract now says how to show the
  `no_side_touchie` wall (from the chimney top, not from the floor), that the opening needs 2 kicks, and that the
  5-tile pit needs a dash near the jump top. In water, Megaman's air dash works only after a jump into the pool, as on
  `master`.
