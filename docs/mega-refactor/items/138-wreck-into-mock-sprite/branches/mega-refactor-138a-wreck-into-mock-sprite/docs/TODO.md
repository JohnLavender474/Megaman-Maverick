# TODO: mega-refactor-138a-wreck-into-mock-sprite

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

- **Spawner camera order.** `MegaLevelScreen` updates the spawners with the camera bounds of the frame before, then
  moves the camera, then draws. An entity whose spawn rect is its sprite rect (for example a `MapSprite`) can miss
  1 frame where it enters the view. Found by the `138a` review (Wreck B lost its 4 px spawn lead). Fix: test the
  spawners after the camera update, or with a small view margin.

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
