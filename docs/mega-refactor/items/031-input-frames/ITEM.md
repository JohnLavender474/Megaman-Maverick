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

## Open questions

- Rename the buttons to intents (JUMP, SHOOT) now, or keep the button names?
- Analog stick: digital threshold only, or real analog movement for some characters?
- Must touch and keyboard still merge for player 1 on desktop?
- How many players at most (2 or 4)? (Decision H.)

## Sources

- `NOTES.md` in this folder.
