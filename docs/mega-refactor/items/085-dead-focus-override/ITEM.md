# 085: Remove the dead `ConstKeys.FOCUS` override in `Megaman`

- Status: idea
- Type: refactor
- Depends on: none
- Related: `051`, `084`
- Branches: none yet

## Goal

`Megaman.getFocusPosition()` reads `ConstKeys.FOCUS` (`Megaman.kt:902`), and `onSpawn` removes it (`Megaman.kt:502`),
but nothing puts it. Remove the dead override, or give it a real caller.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

## Owner's input

None yet. This item comes from a backlog candidate of `051b`.

## Open questions

- Does `084` remove this code when it splits `Megaman`? If so, this item can go into a `084` branch.

## Sources

- `NOTES.md` in this folder.
- The `051b` branch `docs/TODO.md`.
