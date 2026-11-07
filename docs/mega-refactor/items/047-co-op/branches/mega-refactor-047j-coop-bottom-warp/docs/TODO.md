# TODO: mega-refactor-047j-coop-bottom-warp

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- A player held below the view is left behind when the partner walks through a side room exit: `CameraRoomExits`
  ignores held players, the transition starts, and `releaseAll()` releases the held player in the old room. Since
  `047g`/`047h`; the new default makes it easy to reach (`coop_shaft` into `coop_wide`). (Review before the play test.)
- Co-op exploit: a player can skip a spike floor in a tall room by falling fully below the view while the partner is
  far above; it warps in place of dying. Most exposure: `WILY_STAGE_3` `room5`. (Review before the play test.)

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
