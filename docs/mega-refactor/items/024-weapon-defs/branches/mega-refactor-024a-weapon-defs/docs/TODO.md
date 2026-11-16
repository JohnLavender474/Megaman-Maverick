# TODO: mega-refactor-024a-weapon-defs

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- Moon Scythes: the post-shot step fills a reused array with the spawned scythes and returns without clearing it when 4
  or fewer exist (old `MegamanWeaponsHandler.kt:336`, now `MoonScythesState.fadeExtraScythes`). The array keeps old
  and dead entries, so a later shot can count more than 4 and fade live scythes too soon. `024a` keeps the behavior
  (a refactor), with a `TODO` at the line.
