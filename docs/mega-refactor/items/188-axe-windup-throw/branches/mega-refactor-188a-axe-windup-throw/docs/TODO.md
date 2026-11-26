# TODO: mega-refactor-188a-axe-windup-throw

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- X's own charge sounds and tuning on his `BusterCharge` (item `081`).
- `docs/CODE_STYLE.md` names `playerChargeComponent` in its named-argument example, and
  `objectives/O8-characters-and-modes.md` names `PlayerCharge.kt` and `stopCharging()`: update them on `master` at
  the merge.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- No `cartin_axe_throw` or `jetpack_axe_throw` animation: a throw (or a hold) in a cart or a jetpack asks for a key
  with no def, and the sprite keeps its last region (same gap for the throw on `master`).
- The atlas has a `climb_finish_axe_throw` region with no def.
