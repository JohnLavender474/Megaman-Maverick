# Process log: mega-refactor-084g-movement-state

## Status

- State: merged
- Goal: Move the movement state out of `Megaman`: `canMove`, the run state, the crouch timers, and typed values in place of 4 property bag lines; the game plays the same.
- Done: branch start (docs commit on `master`), worktree, scan, `docs/SCOPE.md` (approved 2026-11-01), `docs/CONTRACT.md` (approved 2026-11-01), `docs/PLAN.md` (approved 2026-11-01), plan steps 1 to 6, the automated checks, the owner's play test.
- Next:
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-01

- Branch start. `031e` is on `master`, so this branch no longer waits for it. Created the branch and its worktree `.worktrees/mega-refactor-084g-movement-state/` from `master`.
- Scan of `canMove`, the run state, `runLeft` and `runRight`, the crouch timers, and the 2 bag keys. Draft `docs/SCOPE.md` written for approval.
- The owner asked if `canMove` is better as a component. A read-only subagent compared 4 options, and I checked its code facts. The owner approved the member form and the scope, with `IGameEntity.canMove` removed (see `docs/NOTES.md`, "Decisions"). Draft `docs/CONTRACT.md` written.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written.
- The owner approved `docs/PLAN.md`.
- Step 1 done. `MegamanRunTest` passes on the old `runLeft` and `runRight`. It has no case for `damaged` alone: `damageTimer` is protected, and `damaged` is also a term of `canMove`, so the movement lock case covers the same branch of the guard.
- The owner asked to merge `master` into this branch, which took in `051e`. The merge at `f16f1aaa3` had no conflict. After `./gradlew clean`: engine 621, core 555, 0 failures, `lwjgl3:jar`, 95 unique warnings. The `051e` lines in the shared files do not touch the run code.
- Step 2 done. `RunComponentTest` failed first because the type was missing. `LevelRoomHandlerTest` has a new case with a player that is not `Megaman`. `MegamanRunTest` passes with no change to its expected values, and 15 casts remain.
- Step 3 done. `run(delta, side)` replaces `runLeft` and `runRight`. The plan said that the `RODENT_CLAWS` difference is a parameter; `side` already gives it, so no new parameter is needed. `MegamanRunTest` passes with no change to its expected values.
- Step 4 done. `MegamanCrouchDelayTest` passes on the old code and, with no change to its expected values, after the timers moved into `crouchBehavior()`.
- Step 5 done. `currentAnimKey` and `airDashDirection` are fields on `Megaman`, with no reset, so each keeps its value through a respawn as the bag did. `MegamanAirDashDirectionTest` failed first because the field was missing. `MegamanKeys.kt` and `ConstKeys.ANIMATION_KEY` are gone. `canMove` has its `TODO`.
- Step 6 automated checks at `09a753db6`, after `./gradlew clean`: engine 621, core 570, 0 failures, `lwjgl3:jar`, 95 unique warnings. 15 casts to `Megaman` remain, and the old names are only in `CrouchBehavior.kt`. A launch of `movement` gives code 124 with no exception. Review of the run function against "Frame order": the checks keep their order, and the 2 reads of the opposite button became 1 read in the same frame, with no write between them. `master` has no change since the merge at `f16f1aaa3`.
- Owner play test, in progress. The owner reported 2 problems with both left and right held. A read-only subagent found both on `master` too, and I checked its main code claims. The owner approved a last-pressed rule in a separate fix (see `docs/NOTES.md`, "Decisions", and `docs/TODO.md`, "Bug candidates").
- The owner reported that the `HeartTank` in `DESERT_MAN` `room14` no longer sinks in the quicksand. A read-only subagent found the cause in `926c46c55` (2025-11-14), before the refactor, and I checked its main claims. `084g` changes no file in that path. Filed on `master` as `docs/issues/2026-11-01-heart-tank-does-not-sink-in-sand.md`.
- The owner reported "tests pass": the play test of `docs/CONTRACT.md` passes, apart from these 2 bugs, which exist on `master` too and go to separate fixes.
- The owner approved the merge (verbatim: "yes"). `master` has only docs commits since the merge at `f16f1aaa3`, so no second merge into the branch was needed.
