# 069: A dash button for X

- Status: dropped (2026-11-05): folded into `081`
- Type: feature
- Objectives: `O8`
- Depends on: `031`, `081a` (the first X branch, because only X reads the new button)
- Related: `010`, `024`, `029`, `034`, `047`, `055`, `060`, `068`, `070`, `081`
- Branches: none

## Dropped

2026-11-05 (owner): this item is folded into `081`. Only X reads the dash button, and `081` already had this work as
a step. The dash input is now part of `081` phase 3 (see `081` `ITEM.md`, "Phases"). This file and `NOTES.md` stay as
the record of the research and the decisions. `081` continues the open questions here.

## Goal

X (a future playable character, ported from X6) needs a separate dash button, as in X6: a ground dash, an air dash,
the dash jump and the dash wall kick. Megaman's controls must not change. An NES-style pad is not enough to play as
X, because `SELECT` is its only free button, and `SELECT` is not usable as a dash in real play.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Facts are in `NOTES.md`.

- **The button:** a new `MegaControllerButton` entry at index 8, always bound and sampled, read only by X.
  - The input frame has room (`MAX_BUTTONS = 32`). Megaman's `ControllerComponent` and behaviors read named
    buttons only, so his controls do not change.
  - A per-character control layout from `CharacterDef` (option b) does not fit item `031`: bindings belong to the
    player slot, not the character, and `CharacterDef` does not exist yet (`081`). The character decides which
    buttons it reads; the binding decides which key gives the button.
- **Fixes the new entry needs:**
  - `ControllerMappingExtensions.kt` is an exhaustive `when`: the new entry needs a pad default (compile error
    otherwise).
  - `ControllerSettingsScreen` lists all entries, but two layout values assume 8 buttons: the `SELECT ACTION` arrow
    Y (hard-coded `1.6`) and its "up" target (`SELECT`). Both must move.
  - Tests that check the buttons (`MegaControllerButtonTest`: index equals ordinal).
- **Defaults:** keyboard `U` (free; above `J`, next to the action keys `J`, `K`, `L`); pad `R1` (free).
- **One dash input, as in X6 (option B):** the only dash input is `R`, and only X has it. It starts the ground dash
  and the air dash. `A` is only jump and wall kick.
- **`SELECT` is always `NEXT_WEAPON` for X,** whatever the saved `selectButtonAction` is. X has his own rules, so his
  own controller reads no `selectButtonAction`. This does not write the setting. This item adds no new `LevelType`.
- **`selectButtonAction` is for Megaman, and for Proto Man when he exists.** `AIR_DASH` is specific to these
  characters.
- **No `DOWN` + `A` fallback** and no double-tap. An NES-style pad is not a target for X, so it needs no fallback. X6
  on foot has no double-tap (only the Ride Armor has one).
- **No input buffer.** X6 has none: a dash jump is "jump pressed in the same update as the dash start, or while dash
  is held or a ground dash runs". The dash wall kick is "dash held when the wall kick starts". These are all reads of
  the current frame, which Maverick already gives.
- **Behaviors:** X's air dash reuses `AirDashBehavior` with values and a dash-input check passed in (suppliers, as
  `081` proposes). The ground dash is a new behavior, because its speed curve differs (X6: 6.5, then slows by
  0.1875 per update down to 4.125).

## Owner's input

2026-10-28 (summary): from a session in `mmx-x6`. X needs the X6 dash button in the extra levels built for him.
Megaman's controls must not change.

## Decisions

- 2026-11-06 (owner, in `081d`): `SELECT` is always X's dash, whatever the select action is (the ground dash, the
  dash jump, the dash wall kick and the faster air dash). `A` in the air still starts the plain air dash. The `R`
  button is removed, and `SELECT` never pauses the game for X. This replaces the `R` decisions below.
- 2026-11-06 (owner, in `081c`): the `R` button exists now (index 8, keyboard `U`, pad `R1`), read only by X. It
  starts X's air dash. Changed from option B below: `A` in the air and `SELECT` (when the select action is air dash)
  also start X's air dash, as for Megaman. The ground dash on `R` comes in phase 3 of `081`.

- 2026-10-28: the new button is named `R` (a physical name, as item `031` decided for all buttons).
- 2026-10-28: option B. One dash input does the ground dash and the air dash; `A` is only jump and wall kick. The
  owner's first idea for option A was to keep `DOWN` + `A` for both dashes, as for Megaman's slide. It was dropped,
  because it blocks moves such as the dash jump (it needs dash held when the jump starts).
- 2026-10-28: the select action is always "dash" when playing as X, in levels of a new type `X_LEVEL`. The override
  must not overwrite the saved settings. The owner's words: "dash" instead of "air dash", "because dashing as X is
  both air dashing and ground dashing".
- 2026-10-28: the proposals in this file are agreed, with the changes above.
- 2026-11-01 (owner): an NES-style pad is not enough to play as X. The only dash button on it would be `SELECT`, and
  that is not feasible in real play. This replaces the first goal of this item, that a player with an NES-style pad
  can play X. `O9-R1` records the exception.
- 2026-11-01 (owner): the `SELECT` override and `LevelType.X_LEVEL` are removed. This replaces the 2026-10-28 decision
  above. The override existed so that every pad has a dash input, and that is no longer a goal. For X, `SELECT` is
  always `NEXT_WEAPON`, whatever the option says, and `R` is the only dash input. `AIR_DASH` is specific to Megaman
  and Proto Man. This closes open questions 1, 2 and 7.
- 2026-11-01 (owner, from `081`): the rules for `SELECT` and the dash are done on a player basis, not on a level
  basis. If Megaman plays an X6-based extra level later, he keeps his own controls there.
- 2026-11-01 (owner, from `081`): X is the second caller of `CharacterDef`, before Proto Man (`060`). This closes
  open question 5.

## Open questions

1. Closed 2026-11-01: `SELECT` is always `NEXT_WEAPON` for X (see "Decisions").
2. Closed 2026-11-01: the controls screen shows the saved value. That value applies to Megaman and Proto Man, not to X.
3. Does the controls screen show the new row always, or only when X is available? Always is simpler; a hidden row
   needs a "which characters exist" check in a menu.
4. Item `010` (fixed tick) is an idea. Input is read once per render frame, and the frame rate changes with the
   performance tier. X6 rules that count updates (dash time 30 updates, air dash 18) must become seconds, or wait for
   `010`. Which?
5. Closed 2026-11-01: X is the second caller of `CharacterDef` (see "Decisions").
6. Should saved bindings load at boot first (`034`)? Now they load only from the "LOAD SAVED SETTINGS" menu item, so
   a remapped dash key is lost on restart until `034`.
7. Closed 2026-11-01: the override is removed (see "Decisions").

## Sources

- `NOTES.md` in this folder.
- X6 input: `~/IdeaProjects/mmx-x6/src/main/java/mmx/x6/input/Buttons.java`.
- X6 movement: `~/IdeaProjects/mmx-demake-tools/docs/players/movement.md`, `air.md`, `ride-armor.md`.
