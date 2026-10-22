# 013: Level flow state machine; pause as an overlay

- Status: planned
- Type: refactor
- Depends on: `015`
- Related: `006`, `012`, `037`, `038`, `053`
- Branches: none yet

## Goal

Replace the fragile first-unfinished-wins handler `when` in `MegaLevelScreen` with an explicit level flow state machine.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- `LevelFlow` states (spawning, playing, room transition, boss intro, boss fight, victory, dying, game over) replace the ordered handler `when` at `MegaLevelScreen.kt:906-913`.
- Level events become state machine inputs.
- Pause, and later other menus, as an overlay stack. Remove the `game.paused` checks.

## Owner's input

Answer to review item 12 (2026-10-22, verbatim):

> Yes, one mistake I made is not utilizes step machines enough. A lot of logic buried in sequential if statements, for loops, and in-code branches that would be much easier to change and maintain if done through state machines.

## Open questions

- Must a boss fight and a room transition ever overlap (for example X-style camera zones during a boss)?

## Sources

- `NOTES.md` in this folder.
