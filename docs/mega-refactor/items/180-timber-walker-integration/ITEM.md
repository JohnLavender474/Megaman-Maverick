# 180: Timber Walker: refine with an artist and put into the game

- Status: idea
- Type: feature
- Objectives: `O4`, `O9`
- Depends on: `078f` (the Timber Woman stage, for the walker's place)
- Related: `078`
- Branches: none yet

## Goal

The Timber Walker mini boss becomes a finished fight in the Timber Woman stage. Branch `078g` merged it as a first
pass: AI-made art (round 7) and a first moveset, spawned only in the test map `RefactorTest078`
(`./run-mega-man-maverick-alpha.sh --level REFACTOR_TEST_078 --room walker_gate --heartTanks all --healthTanks all`).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope.

- Art with an artist: refine and expand the walker's frames. The round-7 art questions (steel color, background walk
  legs, guard motion, tell contrast, chop reach, grab head) are in `temp/078-timber-walker-miniboss/NOTES.md` of the
  main checkout, "Round 7". The frame generator and the asset builder are in `temp/078-timber-walker-miniboss/` and
  `temp/078g-timber-walker/` (`build_walker_assets.py`, rebuild steps in the `078g` process log).
- The moveset made final in play rounds (`docs/ways-of-working/gameplay.md`).
- The walker's room in the Timber Woman stage (with `078f`), and the room before it with the boss gate.
- Then the final pass: the tests from the `078g` `docs/TEST-DEBT.md`, mutation checks, the review before the play
  test.
- Loose tasks of `078g`: the Met runs away at the defeat, log pieces that hurt, walk-back frames.

## Owner's input

- 2026-11-23 (branch `078g`): merge the first pass; the mini boss will be refined and expanded with an artist, and its
  integration into the game is a TODO.

## Open questions

## Sources

- Branch `078g`: `items/078-robot-master-alt-paths/branches/mega-refactor-078g-timber-walker/` (`SCOPE.md`,
  `TEST-DEBT.md`, `TODO.md`, `process-log.md`).
- Smoothness review of the first pass: `.worktrees/mega-refactor-078g-timber-walker/temp/078g-timber-walker/review-smoothness.md`
  (copied to the main checkout's `temp/078g-timber-walker/` at the merge).
