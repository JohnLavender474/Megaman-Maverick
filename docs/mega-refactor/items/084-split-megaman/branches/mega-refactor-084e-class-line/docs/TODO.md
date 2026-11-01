# TODO: mega-refactor-084e-class-line

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `MegaRegionProcessor` and the `regionProcessor` parameter of `MegamanAnimations` have no implementer. `024` can use them for the weapon spawn pixels, or they can go.
- The old `Cart` has no play effect, and of the live maps only `Test1.tmx` holds it; stages use `CartV2`. With it, `FeetCartRule`, `FixtureType.CART`, `BodySense.TOUCHING_CART`, `ConstKeys.CART` and the `CART` check in `MegaContactFilter` can go. Covered by item `092`: no new item at the merge.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
