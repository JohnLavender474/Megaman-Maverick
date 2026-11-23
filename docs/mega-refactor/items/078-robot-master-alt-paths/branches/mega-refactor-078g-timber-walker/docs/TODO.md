# TODO: mega-refactor-078g-timber-walker

## Tasks

Loose tasks that are not steps in `PLAN.md`.

- Timber Walker defeat: the Met jumps out, cries and runs away (`TODO` in `TimberWalker.onDefeated`).
- Timber Walker log: log pieces that hurt after the break (`TODO` in `TimberWalkerLog`).
- Timber Walker: `WALK` back home plays the forward walk while the walker moves backward (no walk-back frames).
- [x] `level-builder`: add the commands `add-gate` and `add-boss-room` (owner request). Reason: the hand-written
  script for `RefactorTest078.tmx` got the entry gate wrong 3 ways (`boss_key` on the entry gate, `room` named the
  room the gate is in, `direction` named the far side).

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- Timber Walker: refine and expand the mini boss with an artist (owner, 2026-11-23), then put it into the game. The
  branch merged as a first pass in the test map `RefactorTest078` only; no real level spawns it. Open work: the art
  with an artist (round-7 art questions in `temp/078-timber-walker-miniboss/NOTES.md` of the main checkout), the
  moveset final, the walker's place in the Timber Woman stage (with `078f`), the tests from this branch's
  `docs/TEST-DEBT.md`, mutation checks and the review before the play test, and the loose tasks above.

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- Engine `Animation.reset()` sets only `elapsedTime`, not `currentIndex`. A caller that reads `getIndex()` after
  `reset()` and before `update()` gets the index of the last play (the walker's 1-frame flash, smoothness review
  finding 1). `Animator` hides it because it calls `update()` after the reset. The walker now calls `setIndex(0)`.
  Fix in the engine: `reset()` also sets `currentIndex = 0`; add the check to `AnimationTest`.
- `MegaLevelScreen` starts a camera shake only when the last one is finished, so a second shake request within
  0.5 s is dropped (smoothness review finding 9; shared level-screen code).
