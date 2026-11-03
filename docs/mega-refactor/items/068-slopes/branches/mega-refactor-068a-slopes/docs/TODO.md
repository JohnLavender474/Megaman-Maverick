# TODO: mega-refactor-068a-slopes

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- X6 flat step-down: a body that leaves a slope onto a flat floor up to 8 X6 px lower keeps its ground (X6
  `resolveFloor`, item `NOTES.md`, "X6 slopes"). Needs a probe after `resolveCollisions`, so it goes with `017`.
  Deferred by the owner on 2026-11-03.

- Item `003` validator rule: every object in a `blocks_slopes*` layer is a rectangle named `GroundSlope`, with
  `side` set and a ratio of 1/4 or 1/2. An object with no name there spawns as a full `Block`.

- Slope pass-through from below: a body that jumps or air dashes up into a slope snaps up onto the surface at the top
  of its jump (or at once in a dash). A one-way platform lets it fall back down. A fix pushes up only a body whose
  bottom was on or above the surface in the cycle before, which needs state for each body. Found by the owner in the
  `jump_onto` play test, 2026-11-03; the owner does not plan levels that need it. `TODO` at `GroundSlope`.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
