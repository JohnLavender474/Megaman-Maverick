# TODO: mega-refactor-060m-protoman-moves

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `PlayerBuster.shoot` has 6 pose flags and `BusterParams` 6 pose offsets, each with its exception (step 6). One
  pose enum with a map of offsets and one exception would be one shape for every pose (`docs/CODE_STYLE.md`).
- Proto Man's teleport does not set the `A` task at the end (`AIR_DASH`) or reset an air dash at the start, as
  Megaman's does (step 5); share the teleport lines when a teleporter room is played with Proto Man.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- The swim stroke pushes the wrong way in `DOWN` and `RIGHT` gravity (found at step 2, 2026-11-16). The swim speed
  is already signed by the gravity (`SWIM_VEL_Y * upSign()`), and the stroke negates it again for `DOWN` and
  `RIGHT`, so the stroke pushes toward the floor there. `UP` and `LEFT` are correct. `MegamanSwimTest` pins the old
  signs, and step 3 keeps them (rule for rule), with a `TODO` in `players/PlayerSwim.kt`. The jump uses the signed
  speed with no second sign. Proto Man gets the same signs in step 5.
