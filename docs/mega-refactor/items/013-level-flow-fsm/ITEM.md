# 013: Level flow state machine; pause as an overlay

- Status: planned
- Type: refactor
- Depends on: `015`
- Related: `006`, `012`, `029`, `037`, `038`, `047`, `053`, `082`
- Branches: none yet

## Goal

Replace the fragile first-unfinished-wins handler `when` in `MegaLevelScreen` with an explicit level flow state machine.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `LevelFlow` states (spawning, playing, room transition, boss intro, boss fight, victory, dying, game over) replace the ordered handler `when` at `MegaLevelScreen.kt:906-913`.
- Level events become state machine inputs.
- Pause, and later other menus, as an overlay stack. Remove the `game.paused` checks.
- The flow is the orchestrator that owns the level reset (with the `038` routine), in place of the 24 listeners that react to `PLAYER_SPAWN` today.

## Owner's input

Answer to review item 12 (2026-10-22, verbatim):

> Yes, one mistake I made is not utilizes step machines enough. A lot of logic buried in sequential if statements, for loops, and in-code branches that would be much easier to change and maintain if done through state machines.

Item `029` research (2026-10-23, verbatim):

> This makes the case further that we need a dedicated orchestrator rather than having this logic spread around all over the place. This is outside the scope of the current item but should definitely be documented.

Motivation from `029`: co-op needs one owner for level-wide steps (level reset, boss intro locks for all players, pause owner). Spread-out listeners cannot tell "one player respawned" from "the level reset". See `029` `NOTES.md`, "Co-op gaps and ideas".

## Open questions

- Must a boss fight and a room transition ever overlap (for example X-style camera zones during a boss)?

## Sources

- `NOTES.md` in this folder.
