# TODO: mega-refactor-029b-enemy-players

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `GapingFish` crashes when it spawns: `GapingFish.kt:224` asks for the atlas region `GapingFish/frozen`, which `Enemies1.txt` does not have (it has `Chomping`, `Gaping`, `Swimming`). `findRegion(...)` gives null and the `Animation` constructor throws. No live level places a `GapingFish` (only old map versions and test maps do).
