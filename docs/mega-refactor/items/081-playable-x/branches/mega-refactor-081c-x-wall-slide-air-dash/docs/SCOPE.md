# Scope: mega-refactor-081c-x-wall-slide-air-dash

- Status: approved
- Approved: 2026-11-06 (brief decisions D1 to D4)

## Goal

Phase 2 of X: the wall slide with the wall kick, and the air dash. Megaman plays the same.

## Item

`081`, "Phases", phase 2 (owner, 2026-11-05): "wall slide and air dash (similar to Megaman's air dash)". "Branch
split", step 2: "`airDashBehavior()` and the wall-slide behavior get parameters." Open question 8: the air dash input.

## Current state

The full scan with `file:line` evidence is in `temp/081c/scan.md` (git-ignored, 2026-11-06, `master` at `ad8c20290`).

- Megaman's `wallSlideBehavior()` (84 lines) and `airDashBehavior()` (148 lines) take the `Megaman` type and read
  `aButtonTask`, the weapons, `movementScalar`, water, Rush Jet and `wallSlideNotAllowedTimer`. A slide end gives
  Megaman a new air dash through `aButtonTask`.
- `players/PlayerJump.kt` is the pattern: a shared behavior with a params object, and each class passes its rules.
- X today: `MegamanXBody.kt` sets the side speed to 0 every frame when X does not run, which stops a kick or a dash.
  The run input acts during a slide and would act during a kick lock.
- X6 (`mmx-x6` `Player.java`, `PlayerConst.java`): the air dash is always on (the Falcon armor) and starts on a jump
  press in the air or on the dash button. Values: grab 0.133 s, slide 2.695 t/s, kick 10.547 t/s up and 3.75 t/s
  away with a 0.167 s lock, air dash hover 0.133 s then 0.3 s at 7.734 t/s, 1 per air.
- `X6_INTRO` `room1` has a route with the wall kick: the 2.5-tile step at tile 20, the 4-tile face at tile 50 with a
  2-tile opening, the `SabreBlock` and the ladder top, and climbs of 10 and 14 tiles. The air dash is not needed.
- `047g`: no shared files.

## In scope

- `players/PlayerWallSlide.kt` (`WallSlideParams`, `playerWallSlideBehavior`) and `players/PlayerAirDash.kt`
  (`AirDashParams`, `playerAirDashBehavior`). Megaman's 2 behaviors pass his rules. `isRunSidePressed` moves to
  `players/`.
- Baseline tests on Megaman's wall slide, air dash and `aButtonTask`, written on the old code first.
- X: the grab, the slide, the kick and the kick lock, the air dash, the X6 values, the 2 body and controller fixes,
  and the animation keys `wallslide`, `walljump` and the `dash` strip for the air dash, each with `_shoot`.
- 2 new rooms in `RefactorTest081.tmx`: `walls` and `dash`.
- `players/README.md`.
- D1 (owner): X's air dash starts on Megaman's input (`A` pressed in the air, and `SELECT` when the slot's
  `selectButtonAction` is `AIR_DASH`) and on a new dash button. The button: `MegaControllerButton.DASH`, index 8,
  keyboard `U`, pad `R1` (the defaults that `069` planned), always bound, shown in the controls screen, read only by X.
  The fixes it needs: the pad default in `ControllerMappingExtensions.kt`, the 2 layout values in
  `ControllerSettingsScreen`, `MegaControllerButtonTest`.
- D2 (owner): the X6 air dash shape: a 0.133 s hover, then a fixed 0.3 s at 7.734 t/s, no end bump, 1 per air (a new
  one after a landing or a wall grab), ended early by the opposite input or a wall.
- D3 (owner): the X6 wall rules: a 0.133 s grab, a fixed slide at 2.695 t/s, a kick on any wall touch in the air (also
  while rising, after 0.167 s from a jump start), the 0.167 s kick lock (no input, faces the wall), a push-off at
  3.75 t/s for 0.067 s when the wall input is released.
- D4 (owner): no new sounds, with a `TODO`.

## Out of scope

- The ground dash, the dash jump and the dash wall kick (phase 3). The faster X6 start speed of a dash from the dash
  button (phase 3); in this branch the dash button gives the same air dash as `A`.
- The sabre, climb and crouch. `CharacterDef`.

## Affected areas and risks

- `entities/megaman/behaviors/WallSlideBehavior.kt`, `AirDashBehavior.kt`, the Megaman controller, the
  `entities/megamanx/` files, new files in `players/`, the test map.
- Risk: the params refactor changes Megaman's play. Baseline tests first; Megaman control play checks with water,
  Rush Jet, claws and `SELECT` as air dash.
- Risk: the X6 windows are short (0.133 s and 0.167 s). The entry into the 2-tile opening in `X6_INTRO` can be too
  hard; a play check.

## Overlap with other branches

- `047g`: no shared files.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R2` | no effect | Shared functions, no base class. |
| `O1-R5` | no effect | Each new API has 2 production callers. |
| `O8-R1` | helps | X gets the slide and the dash from shared behaviors, not a copy. |
| `O8-R2` | no effect | No new `is Megaman` line. |
| `O8-R3` | no effect | |

### New APIs and their first callers

| API | Production callers |
|---|---|
| `WallSlideParams`, `playerWallSlideBehavior` | `Megaman.wallSlideBehavior()`, `MegamanX` behaviors |
| `AirDashParams`, `playerAirDashBehavior` | `Megaman.airDashBehavior()`, `MegamanX` behaviors |
| `isRunSidePressed` in `players/` | the Megaman controller, the shared wall slide, `MegamanX.run` |
| `JumpParams.wallJumpAllowed` (D3) | Megaman's and X's jump params |
| `AirDashParams.startDelay` (D2) | X; Megaman passes 0 |
| `MegaControllerButton.DASH` (D1) | X's air dash start |

## Open questions

None. D1 to D4 answered 2026-11-06 (see `NOTES.md`, "Decisions").
