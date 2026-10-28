# 070: More flexible controller settings

- Status: idea
- Type: feature
- Depends on: `031`
- Related: `034`, `047`, `060`, `069`
- Branches: none yet

## Goal

The controller settings are too restrictive now. A player with a keyboard or a modern gamepad has more buttons than
an NES pad, but cannot use them: for example, they cannot have a dedicated button for the next weapon and another
for the previous weapon. The only flexible part is `SELECT`, whose action is one setting (`selectButtonAction`:
`NEXT_WEAPON`, `AIR_DASH` or `START`). Compatibility with an NES pad stays important: the game must stay fully
playable with only the d-pad, `A`, `B`, `START` and `SELECT`. Extra buttons add customizability on top.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- **Optional extra buttons** with physical names (as `031` decided), for example `L` and `R` (`069` adds `R` for X).
  They are unbound by default on an NES pad, so the NES layout does not change.
- **An action per optional button:** generalize `selectButtonAction` into one action setting for each optional
  button (`SELECT`, `L`, `R`, ...). Actions, for example: `NONE`, `NEXT_WEAPON`, `PREVIOUS_WEAPON`, `AIR_DASH` /
  `DASH`, `START`. The input frame stays keyed by button (`031`); the per-player setting says what each optional
  button does, as `selectButtonAction` does now.
- The setting is per player slot and stored with the bindings (`031e`, then `034`).
- **Per-character overrides** at runtime, as `069` does for `SELECT` in an `X_LEVEL`: an override never writes the
  saved settings.
- The controls screen gets one row per optional button with its key and its action. The layout must not assume a
  fixed button count (now two values assume 8 buttons; see `069` `NOTES.md`).

## Owner's input

2026-10-28 (verbatim): "This has me thinking that we need a new item to redo the controller settings since right now
they can be a bit too restrictive. For example, what if the player wants to support a dedicated button for swapping
weapons? The compatibility with an NES controller is important, but as such there should be customizability
available to those using a keyboard or advanced controller."

Context: item `069` question 1. In an `X_LEVEL`, `SELECT` is always "dash", so X loses `SELECT`'s usual action.

## Open questions

1. Which optional buttons, and how many: `L` and `R` only (SNES), or also `X` and `Y` (modern pads)?
2. Is the action list the same for every character, or does each character have its own list (X has "dash",
   Megaman has "air dash")? Does an action a character does not have do nothing?
3. Does `069` wait for this item (so `R` is one optional button whose default action for X is "dash"), or does `069`
   add `R` first and this item builds on it?
4. Can two buttons have the same action (for example `SELECT` and `R` both "dash" for X)?
5. Do the default bindings for a keyboard and a modern pad give the optional buttons actions at once, or does the
   player turn them on?

## Sources

- `NOTES.md` in this folder.
- Item `031` `ITEM.md` (bindings decisions) and `NOTES.md` (the `selectButtonAction` scan).
