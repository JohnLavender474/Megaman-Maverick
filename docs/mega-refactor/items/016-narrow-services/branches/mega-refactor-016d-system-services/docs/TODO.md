# TODO: mega-refactor-016d-system-services

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `PathfinderResult.targetReached` (engine) has a misleading value: `Pathfinder.call()` returns `false` when it finds
  a path to the target (`Pathfinder.kt`, the `return PathfinderResult(path, false)` in the loop) and `true` only when the
  start is the target or out of the max distance. No production code reads it today. Rename or fix it (engine, no
  gameplay effect while it has no reader). Found while writing `MegaPathfinderFactoryTest`, which checks the path end
  instead.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
