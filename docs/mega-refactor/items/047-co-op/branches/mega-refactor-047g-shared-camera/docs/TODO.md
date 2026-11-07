# TODO: mega-refactor-047g-shared-camera

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `WARP` does not test the spot beside the partner for solids. A player can land inside a wall or a platform. Fix: test both sides and the partner's own spot, or warp to the partner's last ground position. (Found by the review before the play test.)
- With 2 players in a room with a `051` zone or scroll, the zone and the scroll read the group midpoint as their focus point, and `CameraEdgeBlocks` still kills only slot 0 at a kill edge. Phase 5 of the shared screen.

- Keep a player's surroundings alive out of view (cull only what is out of the view and far from every player). The real fix for a player out of view; split screen `094` needs it too. About 2270 blocks and 88 entity files use the camera cull. (Owner's F3, play test 2026-11-06.)
- `WARP` visuals, phase 2: the held player's sprite floats back into the view, and bubbles move to the warp spot before the warp. Phase 1 (this branch) draws a guide line and circle. (Owner, play test 2026-11-06.)

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
