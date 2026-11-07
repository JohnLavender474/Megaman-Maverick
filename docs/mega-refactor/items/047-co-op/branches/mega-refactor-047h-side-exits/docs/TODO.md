# TODO: mega-refactor-047h-side-exits

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- With slot 0 held by a side `WARP` rule, or dead, the exit walls drop and slot 1 can walk into the next room with no
  transition, because the room scan follows only slot 0. Same as before `047h`. Phase 6 (death and respawn). (Review
  before the play test.)
- A player past an exit wall but outside the shared span (a jump over a room top into a taller room) never counts as at
  the exit, and the wall keeps it from coming back. Rare: it needs open headroom above the room. (Review before the play
  test.)
- After a transition, both players stand on one spot. Spread them out. (Scope, out of scope.)
- A cue that shows who waits at an exit. (Scope, out of scope.)

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).
