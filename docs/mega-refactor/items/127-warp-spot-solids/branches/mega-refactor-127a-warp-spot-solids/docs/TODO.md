# TODO: mega-refactor-127a-warp-spot-solids

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- The warp cap does not run while no free partner is in view (for example the partner above a `NONE` top edge); the
  held player waits until the partner is back in view. `TODO` in `CameraViewEdges.updateHolds` (review, 2026-11-07).
- A partner on a ladder stands, but its side spots have no ground, so the player lands on the partner's spot in the
  ladder without a climb and can fall and warp again. `TODO` in `CameraViewEdges.stands` (review, 2026-11-09).
- The landing after the cap tests only for solids, so it can land in a `DEATH` or `TELEPORTER` fixture at the partner's
  spot (owner decision of 2026-11-09: wait only for solids) (review, 2026-11-09).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
