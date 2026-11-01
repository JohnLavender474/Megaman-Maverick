# 085: Remove the dead `ConstKeys.FOCUS` override in `Megaman`

- Status: dropped
- Type: refactor
- Objectives: `O1`
- Depends on: none
- Related: `051`, `084`
- Branches: none yet

Dropped on 2026-10-31 (owner): the planned branch `mega-refactor-084f-focus-progress` of item `084` removes this code when the focus leaves `Megaman`.

## Goal

`Megaman.getFocusPosition()` reads `ConstKeys.FOCUS` (`Megaman.kt:902`), and `onSpawn` removes it (`Megaman.kt:502`),
but nothing puts it. Remove the dead override, or give it a real caller.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

## Owner's input

None yet. This item comes from a backlog candidate of `051b`.

## Open questions

## Sources

- `NOTES.md` in this folder.
- The `051b` branch `docs/TODO.md`.
