# Notes: 069 A dash button for X

Research for this item: scans, counts, options, X6 facts (name the file and section) and
reviews. Give the date and the `master` commit that each finding is based on.

## 2026-10-28: research (read-only agent, `master` at `d711798da`)

Paths: `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `M/` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

### Buttons and bindings

- `M/controllers/MegaControllerButton.kt:7-15`: `UP(0,W)`, `DOWN(1,S)`, `LEFT(2,A)`, `RIGHT(3,D)`, `A(4,K)`,
  `B(5,J)`, `START(6,ENTER)`, `SELECT(7,L)`. `DPAD_BUTTONS` and `ACTION_BUTTONS` at L22-23; a new entry is in
  neither.
- Pad defaults: `M/controllers/ControllerMappingExtensions.kt:5-14`, an exhaustive `when` with no `else`. Unused pad
  buttons: `buttonX`, `buttonL1`, `buttonR1`.
- `loadControllerButtons()` (`M/controllers/ControllerUtilsExtensions.kt:16-34`) loops over all entries. It does not
  apply saved prefs at boot; they load from "LOAD SAVED SETTINGS" (`ControllerSettingsScreen.kt:178-225`) or on pad
  connect when `loadPrefsOnConnect` is true (`M/input/GamepadInput.kt:16-28`).
- Save: one integer per button, keyed by `button.name` (`ControllerUtilsExtensions.kt:36-54`). A new name falls back
  to its default, so old pref files stay valid.
- `ControllerSettingsScreen` loops over all entries (L261-314). Rows start at 12.75 and drop by 1. The
  `SELECT ACTION` arrow Y is hard-coded at L401 (`1.6`), and its "up" target is `SELECT` (L338). With a 9th entry
  after `SELECT`, the arrow sits beside the new row and "up" skips it. The screen may run off the bottom; check it in
  the game.

### Code that loops over all buttons

- All entries: `M/MegamanMaverickGame.kt:208` (`InputFrameBuilder`), `:292` (`checkInputButtons`);
  `ControllerUtilsExtensions.kt:19, 26`; `M/controllers/ScreenController.kt:112, 192`; `ControllerSettingsScreen`.
- Tests: `MegaControllerButtonTest.kt:11,15` (index equals ordinal), `GamepadInputTest`, `KeyboardInputTest`.
- `InputFrame` packs 2 bits per button in a `Long`, `MAX_BUTTONS = 32` (`E/input/InputFrame.kt:9`).
- Keyboard and pad sampling loop over the bound buttons (`M/input/KeyboardInput.kt:9-18`, `GamepadInput.kt:36-44`).
- Touch overlay: an image per entry, but only the 8 named buttons are placed (`ScreenController.kt:150-181`). Touch
  reaches no game button now (TODO at L144).
- Fixed lists, not affected: menus (`MegaMenuScreen.kt:19-26`), pause (`MegaLevelScreen.kt:917-918`), cutscene and
  skip screens in `M/screens/other/`, `MegamanValues.BUTTONS_TO_UNFREEZE = [B]`, and the entities that read player
  input (`PipePortal`, `SpringBouncer`, `SpiderWeb`, `GroundSlideBlocker`).
- Megaman's `ControllerComponent` binds `B`, `LEFT`, `RIGHT`, `SELECT` (`M/entities/megaman/components/
  ControllerComponent.kt:122-127`). No record and replay code exists (`055` is planned). No HUD reads buttons.

### Input timing

- No input buffer, coyote time or double-tap logic anywhere.
- One frame per render frame: `players.inputs[slot0] = slot0FrameBuilder.build(...)` (`MegamanMaverickGame.kt:539`),
  before `screen.render`. `InputFrameBuilder.build` (`E/input/InputFrameBuilder.kt:14-34`) gives `JUST_PRESSED`,
  `PRESSED`, `JUST_RELEASED`, `RELEASED`.
- Not a fixed tick. Only `WorldSystem` has fixed sub-steps. The tiers: VERY_LOW 30 fps, LOW 45, MEDIUM 60, HIGH 90
  (`MegamanMaverickGame.kt:152-157, 884-895`), and the tier can drop by itself (L520-530). A window counted in frames
  would change with the tier.

### Megaman's slide and air dash

- Slide: on the ground, `DOWN` held and `A` just pressed (`GroundSlideBehavior.kt:52-54`). Jump is blocked on the
  ground while `DOWN` is held (`JumpBehavior.kt:31`). Values in `MegamanValues.kt:72-76`.
- Air dash: `M/entities/megaman/behaviors/AirDashBehavior.kt`.
  - Input: `A`, or `SELECT` when `selectButtonAction == AIR_DASH` (L33-41). Starts on just-pressed with
    `aButtonTask == AButtonTask.AIR_DASH` (L60).
  - Air only, except with `RUSH_JET` (L51). Gravity off while dashing; constant speed (L104).
  - Values are `const val`s in `MegamanValues.kt:49-57`; `airDashBehavior()` takes no parameters. The input check is
    tied to `A`, `SELECT` and the `AButtonTask` state machine (`AButtonTask.kt`).
- No `CharacterDef` exists. Its design is in `029` `NOTES.md` (L62, L113, L221, L293, L796). `031` `NOTES.md:28`
  lists later intent names: `JUMP`, `SHOOT`, `DASH`, `PAUSE`, `SWITCH_WEAPON`.

### X6 facts

- `~/IdeaProjects/mmx-x6/src/main/java/mmx/x6/input/Buttons.java:8-23`: `DASH` is a separate button (key `L`).
  `JUMP` `K`, `BUSTER` `J`, `WEAPON` `I`, `WEAPON_NEXT` `O`, `WEAPON_PREV` `Q`. `pressed()` is a one-update edge
  (L58-60). Opposite directions cancel (L39-44). No buffer.
- No double-tap dash for X or Zero on foot (`docs/players/movement.md:107-109, 229`; `air.md:200, 359`). The Ride
  Armor has one: "dash, or double tap (12-update window)", ground only (`ride-armor.md:72, 110`).
- Dash jump: jump pressed in the same update as the dash start (`movement.md:111-113, 223`), or during the dash
  (L123). Any jump that starts while dash is held or in a ground dash is a dash jump (`air.md:74`). No ledge grace
  time (`air.md:106`).
- Wall kick: jump pressed next to a wall. With dash held it is a dash wall kick (speed x 4.125, no air dash after it)
  (`air.md:135-139, 178-181`).
- Ground dash: start speed 6.5, slows by 0.1875 per update to 4.125, at most 30 updates (15 with the Hyper Dash
  part). It ends at a wall, when the opposite direction is held, or when dash and the directions are released
  (`movement.md:37-39, 114, 127-140, 220`).
- Air dash: speed 6.5, 18 updates (10 with the part), once per time in the air, none after a dash jump or a dash wall
  kick (`air.md:186-206`). Dash jump air speed 4.125 (`air.md:45`).

## 2026-10-28: air dash input: options (`master` at `d711798da`)

Fact: for Megaman, `SelectButtonAction.AIR_DASH` replaces `A` for the air dash (`AirDashBehavior.kt:33-41`), and
`SELECT` then no longer does `NEXT_WEAPON` (`SelectButtonAction` has `NEXT_WEAPON`, `AIR_DASH`, `START`).

- Option A, the same as Megaman: `A` in the air (or `SELECT` when set to `AIR_DASH`) starts the air dash; `R` starts
  the ground dash only.
  - Pros: one muscle memory for both characters; works on an NES pad with no setup; reuses the setting.
  - Cons: not the X6 feel (in X6, jump in the air does only a wall kick); a jump pressed a little too far from a wall
    gives an air dash instead of a wall kick; the dash jump and the dash wall kick need "dash held", which only `R`
    can give; the two dashes use different buttons.
- Option B, X6-style: one dash input (`R`, or `SELECT` when set to `AIR_DASH`) does both dashes; `A` is jump and wall
  kick only.
  - Pros: the X6 and SNES feel; with `SELECT` mapped, an NES pad has the 4 SNES-style buttons; the dash jump and dash
    wall kick work with either dash input.
  - Cons: with default settings, an NES pad has no air dash for X; the same setting does a little more for X than
    for Megaman.
