# TODO: mega-refactor-081e-x-sabre

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Merge `DebrisPiece`, `BlockPiece` and `WoodCratePiece` into one falling-piece entity.
- Merge `Wreck` into `MockSpriteEntity`.
- A shared simple enemy shot, starting with `JunkShot`.
- `TickFrames`: the engine `Animation` with durations in seconds, in place of 60 Hz ticks.
- The sabre damage by slash kind and by target, in place of one static value.
- Tests: replace the reflection in `FriendlyFireTest` (047b) and `EntityExistenceTest` (062a) with real flows.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
