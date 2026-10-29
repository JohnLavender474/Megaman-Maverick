# 069: A dash button for X

- Status: planned
- Type: feature
- Depends on: `031`
- Related: `010`, `024`, `029`, `034`, `047`, `055`, `060`, `068`, `070`, `081`
- Branches: none yet

## Goal

X (a future playable character, ported from X6) needs a separate dash button, as in X6: a ground dash, an air dash,
the dash jump and the dash wall kick. Megaman's controls must not change. A player with an NES-style pad must still
be able to play X.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Facts are in `NOTES.md`.

- **The button:** a new `MegaControllerButton` entry at index 8, always bound and sampled, read only by X.
  - The input frame has room (`MAX_BUTTONS = 32`). Megaman's `ControllerComponent` and behaviors read named
    buttons only, so his controls do not change.
  - A per-character control layout from `CharacterDef` (option b) does not fit item `031`: bindings belong to the
    player slot, not the character, and `CharacterDef` does not exist yet (`060`). The character decides which
    buttons it reads; the binding decides which key gives the button.
- **Fixes the new entry needs:**
  - `ControllerMappingExtensions.kt` is an exhaustive `when`: the new entry needs a pad default (compile error
    otherwise).
  - `ControllerSettingsScreen` lists all entries, but two layout values assume 8 buttons: the `SELECT ACTION` arrow
    Y (hard-coded `1.6`) and its "up" target (`SELECT`). Both must move.
  - Tests that check the buttons (`MegaControllerButtonTest`: index equals ordinal).
- **Defaults:** keyboard `U` (free; above `J`, next to the action keys `J`, `K`, `L`); pad `R1` (free).
- **One dash input, as in X6 (option B):** the dash input is `R` or `SELECT`. It starts the ground dash and the air
  dash. `A` is only jump and wall kick. An NES pad has 4 buttons for X (`A` jump, `B` shoot, `SELECT` dash, `START`
  pause): the SNES X layout without L/R weapon switching, which X1 also did from the pause menu.
- **`SELECT` is always "dash" in X levels:** a new `LevelType.X_LEVEL`. In a level of that type, the select action
  is "dash" for the whole level, whatever the saved `selectButtonAction` is. This is a runtime override only: it does
  not write the setting, and the saved setting applies again in all other levels.
- **No `DOWN` + `A` fallback** and no double-tap. With `SELECT` always "dash", every pad has a dash input. X6 on foot
  has no double-tap (only the Ride Armor has one).
- **No input buffer.** X6 has none: a dash jump is "jump pressed in the same update as the dash start, or while dash
  is held or a ground dash runs". The dash wall kick is "dash held when the wall kick starts". These are all reads of
  the current frame, which Maverick already gives.
- **Behaviors:** X's air dash reuses `AirDashBehavior` with values and a dash-input check passed in (suppliers, as
  `060` proposes). The ground dash is a new behavior, because its speed curve differs (X6: 6.5, then slows by
  0.1875 per update down to 4.125).

## Owner's input

2026-10-28 (summary): from a session in `mmx-x6`. X needs the X6 dash button in the extra levels built for him.
Megaman's controls must not change.

## Decisions

- 2026-10-28: the new button is named `R` (a physical name, as item `031` decided for all buttons).
- 2026-10-28: option B. One dash input does the ground dash and the air dash; `A` is only jump and wall kick. The
  owner's first idea for option A was to keep `DOWN` + `A` for both dashes, as for Megaman's slide. It was dropped,
  because it blocks moves such as the dash jump (it needs dash held when the jump starts).
- 2026-10-28: the select action is always "dash" when playing as X, in levels of a new type `X_LEVEL`. The override
  must not overwrite the saved settings. The owner's words: "dash" instead of "air dash", "because dashing as X is
  both air dashing and ground dashing".
- 2026-10-28: the proposals in this file are agreed, with the changes above.

## Open questions

1. In an `X_LEVEL`, `SELECT` loses its usual action (`NEXT_WEAPON` or `START`). Is weapon switching from the pause
   menu enough for X? (2026-10-28: the owner opened item `070` for dedicated buttons such as a weapon switch; this
   question waits for it.)
2. What does the controls screen show for "SELECT ACTION" while the override is on (for example during pause in an
   `X_LEVEL`): the saved value, or "DASH" with the row locked?
3. Does the controls screen show the new row always, or only when X is available? Always is simpler; a hidden row
   needs a "which characters exist" check in a menu.
4. Item `010` (fixed tick) is an idea. Input is read once per render frame, and the frame rate changes with the
   performance tier. X6 rules that count updates (dash time 30 updates, air dash 18) must become seconds, or wait for
   `010`. Which?
5. Does X come as a third character after Proto Man (`060`), or does X become the second caller that shapes
   `CharacterDef`?
6. Should saved bindings load at boot first (`034`)? Now they load only from the "LOAD SAVED SETTINGS" menu item, so
   a remapped dash key is lost on restart until `034`.

## Sources

- `NOTES.md` in this folder.
- X6 input: `~/IdeaProjects/mmx-x6/src/main/java/mmx/x6/input/Buttons.java`.
- X6 movement: `~/IdeaProjects/mmx-demake-tools/docs/players/movement.md`, `air.md`, `ride-armor.md`.
