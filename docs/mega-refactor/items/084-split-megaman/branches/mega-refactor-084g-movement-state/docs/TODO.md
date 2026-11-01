# TODO: mega-refactor-084g-movement-state

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `runLeft` and `runRight` differ for `RODENT_CLAWS` when the run guard stops the run during a shot: with `LEFT` and `RIGHT` both pressed, the facing goes right, and it never goes left (`ControllerComponent.kt:165-171,210-213`). This is probably an accident of the copy. The fix below removes it.
- Found in the owner's play test (2026-11-01). Both exist on `master`, by a read of the code; `084g` did not cause them.
  - With `LEFT` and `RIGHT` both held, a running Mega runs in place: the run guard keeps `running` true and applies no impulse.
  - With both held at a wall, Mega can face the wall during a wall slide: for example, at a right wall, hold `RIGHT`, add `LEFT`, and jump. The guard skips the facing write, and `WallSlideBehavior.kt` never writes `facing`. There are also 2 cases of 1 frame: the first frame of each slide, and a release of the wall button with both held.
  - Fix: `fix/left-right-last-pressed`, cut from `master` after `084g` merges, because it needs `RunComponent`. See `docs/NOTES.md`, "Decisions".
