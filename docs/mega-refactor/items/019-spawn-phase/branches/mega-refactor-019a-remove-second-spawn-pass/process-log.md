# Process log: mega-refactor-019a-remove-second-spawn-pass

## Status

- State: merged (2026-10-22)
- Goal: run one spawn pass per frame instead of two.
- Done: research; branch start; `docs/SCOPE.md` and `docs/CONTRACT.md` (approved); draft `docs/PLAN.md`.
- Next: none. `019b` (destroy notification) continues item `019`.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-22

- Created this folder on `master` with a draft scope, after the decision K research. The owner
  wants this as the first, separate step of `019`. No branch was created and no other work was
  started.
- The owner accepted the two rare cases (see `docs/SCOPE.md`). Branch start on `master`.
- In the worktree: updated the overlap section and added the `TODO` comment to the scope.
- The user approved `docs/SCOPE.md`. Planning step 3: wrote the draft `docs/CONTRACT.md`.
- The user approved `docs/CONTRACT.md`. Planning step 4: wrote the draft `docs/PLAN.md`.
- Plan step 1: baseline taken (shared by the wave 1 branches). See `docs/NOTES.md`.
- The user approved `docs/PLAN.md`.
- Step 2 (`a5a1f12d7`): removed the `spawnsMan.update(delta / 2f)` after `engine.update`; the pass
  before it is now `spawnsMan.update(delta)`, with the `TODO` for the two rare cases.
  `playerSpawnsMan.run()` and the second drain loop are unchanged.
- Step 3: clean build succeeds; `engine:test` 427 and `core:test` 49 tests pass; 133 compiler
  warnings (same as the baseline).
- Play test places (rooms with `spawn_room` objects and a player spawn): Desert Man `room13` →
  `room14` (`TorikoPlundge`, `HeartTank`), Desert Man `room10` (51 objects), Wily Stage 1 `room8`
  → `desert_man_room` (`BulbBlaster`), Wily Stage 1 `room3` ↔ `room4` (`LaserBeamer`).
- Merged `master` (with `008a`, squash `c731cf9ab`) into the branch (`1c1a4ee1d`). Checks again: clean build; 427 engine and 77 core tests pass; 133 compiler warnings. The code diff to `master` is still only the spawn pass in `MegaLevelScreen.kt`.
- Step 4, play test (owner): entities spawn as expected. The owner also saw the old `SpawnersLayerBuilder` errors for misplaced TMX objects; they are not caused by this branch and are filed on `master` as `docs/issues/2026-10-22-misplaced-tmx-objects.md`.
- The owner approved the merge. Merged `master` again (docs only changed); checks: clean build, 427 engine and 77 core tests pass, 133 warnings. Squash-merged into `master`.

