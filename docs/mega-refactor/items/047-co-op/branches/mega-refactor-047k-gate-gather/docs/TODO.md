# TODO: mega-refactor-047k-gate-gather

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `GateBlock` in single-player: today it spawns only in co-op (`SessionRules.spawnGateBlocks`, `TODO` at
  `GateBlock.canSpawn`). A later stage may want a player to stand on top of a closed `UP` gate in single-player too.
- `UP` and `DOWN` gates in co-op: the `GateBlock` and the full-width `GATE` fixture (D8, D9) pass the play checks, but
  the gates are not yet as the owner wants (owner, 2026-11-18; the details are not written yet). Until this item
  is done, levels for co-op avoid `UP` and `DOWN` gates (`docs/ways-of-working/levels.md`). `TODO` at `Gate`.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
