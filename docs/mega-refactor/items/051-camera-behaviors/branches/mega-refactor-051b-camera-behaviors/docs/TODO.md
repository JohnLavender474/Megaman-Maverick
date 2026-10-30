# TODO: mega-refactor-051b-camera-behaviors

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `Megaman.getFocusPosition()` reads `ConstKeys.FOCUS` (`Megaman.kt:907`), and `onSpawn` removes it (`:507`), but
  nothing puts it. Remove the dead override, or give it a real caller.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
