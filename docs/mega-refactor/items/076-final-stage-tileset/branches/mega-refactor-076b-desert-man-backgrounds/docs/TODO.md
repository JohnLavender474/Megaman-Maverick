# TODO: mega-refactor-076b-desert-man-backgrounds

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `O5-R1`, found by the `076b` review (pre-existing on `master`): `AnimatedBackground.update` passes a lambda that captures `delta` to the non-inline `Matrix.forEach` (`AnimatedBackground.kt:57`, `Matrix.kt:201`): one lambda object per frame per animated background. `Background.update` (`Background.kt:131`) builds a debug string each frame for every background, also when the tag is filtered out.
- The `.tps` projects have `allowRotation` set to `true`; with `Basic` no region is rotated today, but the setting allows it. Consider `false` in all projects (owner rule: no rotation).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
