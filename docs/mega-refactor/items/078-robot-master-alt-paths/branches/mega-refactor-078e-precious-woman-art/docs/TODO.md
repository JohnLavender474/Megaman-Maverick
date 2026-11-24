# TODO: mega-refactor-078e-precious-woman-art

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- The boss room background is one fixed picture (step-3 decision), with a seam in the pan from `room7`. When item
  `177` (backgrounds drawn per room) merges, give the boss room its own far and mid objects clipped to the room.
  Not a new item: add it to `177` as a level to move.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `Platforms1` is packed as `png8`: each repack moves the colors of `Platform2` (round 8: 175 px, RGB up to 19 against
  round 7; 236 px, RGB up to 31 against `master`) and of `BreakableIce/SmallShards` (RGB up to 11 against `master`).
  Alpha does not change. Options: a page format with no palette for `Platforms1`, or fewer colors in the atlas.
