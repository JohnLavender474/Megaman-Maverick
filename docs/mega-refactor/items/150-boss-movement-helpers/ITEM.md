# 150: Shared movement helpers for bosses and enemies

- Status: idea
- Type: refactor
- Objectives: `O1`, `O4`
- Depends on: none
- Related: `060`, `048`, `118`
- Branches: none yet

## Goal

Remove the movement code that bosses and enemies copy from each other, with small shared helpers that their state
machines call. Not the player's moves: those use a different model (behaviors and input, not state timers).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Counts from the `060j` research (2026-11-10, `master`
`845b194ad`, see `NOTES.md`):

- A `jump(target)` helper: the same 3 lines in `TimberWoman`, `PreciousWoman`, `DesertMan` and `InfernoMan`, with
  variants in 4 more bosses (19 boss and enemy files call `MegaUtilMethods.calculateJumpImpulse`).
- A wall-jump x kick: 4 lines in the same 4 bosses.
- A run with zero-on-turn and a speed clamp: 8 to 10 lines in `TimberWoman` and `PreciousWoman` (and the run push in
  `BikerKibbo`).
- The 4 contact fixtures (feet, head, 2 sides): 20 to 27 lines in 16 boss and enemy files.
- Each move keeps the boss's feel: the same values and the same order of velocity writes.

## Owner's input

2026-11-10 (from the `060j` discussion): "What if I want an enemy or boss that moves almost if not exactly like
Megaman?" The research found that no boss or enemy moves like the player today; the duplication is boss to boss. The
owner chose to make this item.

## Open questions

- Does a boss run use the generic `runStep` from `060j` (`entities/movement/Run.kt`), or its own helper with the
  zero-on-turn and the clamp?

## Sources

- `NOTES.md` in this folder.
