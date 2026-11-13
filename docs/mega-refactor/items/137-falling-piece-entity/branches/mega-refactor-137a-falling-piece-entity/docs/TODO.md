# TODO: mega-refactor-137a-falling-piece-entity

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `FallingPiece.load`, `FallingPiece.throwDebris`, `WOOD_CRATE_REGIONS` and `BLOCK_REGIONS` use a Kotlin
  `Array<String>`, not a LibGDX `Array`
  (`docs/CODE_STYLE.md`). Changing them touches the `DEBRIS` declarations of 5 owners and `D1000DefeatTest`, which
  `060c` and `139a` also change. Do it after those merge.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
