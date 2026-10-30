# TODO: mega-refactor-016b-entity-host-reads

## Tasks

Loose tasks that are not steps in `PLAN.md`.

- Before `016c`: decide the home of the pathfinding debug shapes reads (`Bat.kt:405`, `SubmarineJoe.kt:281`,
  `game.getShapes()`). A separate research agent looks for a better approach (owner, 2026-10-28).
  Done on `master` by the `--debugPathfinding` fix (merged into this branch at `7291362c8`): `Bat` and `SubmarineJoe`
  no longer read `game.getShapes()`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

None.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

None new. `InsertionOrderPriorityQueue.size` is fixed at `0` (`SCAN.md` section 6); it is already filed as
`docs/issues/2026-10-22-insertion-order-priority-queue-size.md`.
