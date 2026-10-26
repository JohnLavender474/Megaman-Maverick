# 031: Input intents per player (`InputFrame`), sources and router

- Status: idea
- Type: refactor
- Depends on: `029`
- Related: `006`, `008`, `010`, `011`, `012`, `032`, `034`, `047`, `055`
- Branches: none yet

## Goal

Each player reads one immutable input frame per tick, built from input sources, instead of polling a global controller.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `InputFrame` (held, pressed and released per action, plus axes), built once per tick at today's poll point.
- Sources (keyboard, pad N, touch, replay, AI). A router assigns them to player slots. A join screen for co-op.
- Entities read their owner's frame. Menus use `UiInput`. The collision handler reads no input.
- A control lock per player with today's release sequence.
- Record and replay of frames (with `010` and `011`).

## Owner's input

Answer to review item 30 (2026-10-22, verbatim):

> yes, let's discuss further

## Decisions

- H (2026-10-22): co-op comes in steps from easiest to hardest: shared screen, then split screen, then online. The input design must not block split screen or online lockstep.
- 2026-10-25: keep the button names (`A`, `B`, `START`, `SELECT`, the d-pad). The controls mirror the NES
  controller, and the names make it easy to know which button is which. The frame is keyed by button, not by
  intent, so the controller is not coupled to intents. What a button does stays the job of the code that reads it.
- 2026-10-25: analog sticks use a digital threshold only. No character moves faster or slower than with the d-pad.
  So `moveX` and `moveY` are not needed; the frame holds the four d-pad buttons like the other buttons.
- 2026-10-25: shared-screen co-op has at most 2 players. Split screen and online may need 4. The input router and
  the frames must support 4 slots, even if the first co-op step uses 2.
- 2026-10-25 (bindings): two players with the same gamepad model must be able to have different maps, so a binding
  belongs to the player, not only to the device model.
- 2026-10-25 (bindings): `selectButtonAction` is one setting per player.
- 2026-10-25 (bindings): one key per button (1:1). The stick reaches the d-pad through the fixed threshold, not
  through a binding.
- 2026-10-25 (bindings): leaving the bindings screen during a level goes back to pause, with no restart (issue
  `2026-10-25-bindings-screen-restarts-level.md`).
- 2026-10-25: the screenshot logic takes a generic trigger, not the `P` key; the launcher gives the trigger for now,
  with a `TODO` to update it later.
- 2026-10-25 (bindings): bindings and `selectButtonAction` are per slot now, kept in one settings object per slot.
  Profiles (a named copy of that object) wait for a feature branch during or after co-op (`047`).
- 2026-10-25 (bindings): `Esc` is a reserved system key and can never be bound.
- 2026-10-25 (bindings): the stick uses one constant threshold (proposal `0.5` per axis; see `NOTES.md`). No
  revert timer.

## Open questions

- Which inputs belong to which player, and what happens when a gamepad is unplugged or stops responding, in
  single player and in co-op? The owner's cases (2026-10-25): one player on a gamepad and another on the keyboard
  must not merge into player 1; a gamepad can be unplugged or stop responding in single player or in co-op, and
  the game must handle all of these. Ideas from the owner: an input settings UI that the mouse can drive (the mouse
  is not assigned to any player), and the keyboard as a trusted input that can always reach it. See `NOTES.md`,
  "Input assignment: cases and options (2026-10-25)".
- `Enter`: fully reserved, or a normal key in gameplay (default `START`) that always confirms in the system
  screens? See `NOTES.md`, "More owner answers on bindings".
- What triggers a screenshot after the `TODO`?
- The open binding questions 7 to 10 in `NOTES.md` (`--resetBindings`, bindable triggers, touch overlay layout,
  `selectButtonAction` migration). Questions 5 (threshold) and 7 (revert timer) are answered.

## Sources

- `NOTES.md` in this folder.
