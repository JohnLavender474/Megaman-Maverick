# TODO: mega-refactor-016a-typed-level-state

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

None.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `EndLevelEventHandler.beamEnd` (`EndLevelEventHandler.kt:227` on `44c6b5fa3`) sets the Megaman beam flag to
  `true` at the end of the beam-out; `false` is the likely intent. No visible effect found: the level screen is left
  at `END_LEVEL`, and the next player spawn sets the flag again. `016a` keeps the write as it is (`SCAN.md`
  section 9). Filed on `master` (owner request, 2026-10-28):
  `docs/issues/2026-10-28-end-level-beam-flag-left-true.md`.
