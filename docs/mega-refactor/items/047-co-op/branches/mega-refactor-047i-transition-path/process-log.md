# Process log: mega-refactor-047i-transition-path

## Status

- State: active
- Goal: at a gathered room exit, each player keeps its own coordinate on the axis across the transition (its own path)
- Done: all steps of the revised `PLAN.md` (D3) and the vertical guard
- Next: owner play test and Diffity review
- Brief: https://claude.ai/artifact/91tCTTsMquWTHfA2GdxFp9
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-17

- Branch start on `master`: branch folder from the template, `ITEMS.md` row `active`, `ITEM.md` branch list. 
- Scan done: `CameraRoomExits` is the only start of a gathered transition; `MegaPlayer.playerOnRoomTransition` is the only reader of the transition `POSITION`. No overlap with the other active worktrees.
- Draft `SCOPE.md` and `CONTRACT.md`; brief rendered and published (link in the status block).
- Owner answers: D1 typed field `LevelState.gatheredExitSide`; D2 add a ledge room pair. `SCOPE.md` and `CONTRACT.md` approved; brief republished as approved; `PLAN.md` written.
- Step 1: a subagent added `coop_ledge` (id 209) and `coop_ledge_next` (id 210) to `RefactorTest047.tmx` at y=960..1408, x=64..1088, with `level_builder` (`add-room`, `add-block`, `add-spawn`), a hand-added `player_2` property and slot spawn, and painted `tiles` (250 cells, gid 175). The ledge (block 214) has 3 tiles of open space below it, so its top is 4 tiles above the floor, the same as `coop_start` platform 148.
- Steps 2 to 4: `LevelState.gatheredExitSide`, writer in `CameraRoomExits`, clears in `LevelRoomHandler.onEnd` and the 2 `MegaLevelScreen` reset sites, reader in `MegaPlayer.playerOnRoomTransition`. 4 new test cases. `core:test`: 2420 tests, 0 failed.
- Step 5, mutation batch (1 run, 3 mutations): reader sets the full point → fails "keeps its own y and takes only the path x at a gathered side exit"; writer removed → fails "marks the transition as gathered with the exit side only when it starts"; handler clear removed → fails "clears the gathered exit side". Code restored with `git checkout -- core/src/main` (clean status).
- Step 6: `engine:test core:test core:smokeTest lwjgl3:jar detektMain core:verifyRandomRule` pass (exit 0).
- Step 7, review before the play test, 2 read-only agents:
  - Diff agent: no bugs. The side is set before `BEGIN_ROOM_TRANS` is handled; it cannot go stale (the only cancel is in the `cameraController.reset()` path, whose 2 runtime sites clear it); 1 player, gates and the room scan never write it. Low notes: a held player would keep its own height, possible only with 3 or more players (fixed with a `TODO` in `CameraRoomExits`); a `NEXT_ROOM_REQ` that restarts a running gathered transition keeps the side (no real path found, not changed). Test gaps: only `LEFT` in the reader test; no gate case for the writer; the 2 `MegaLevelScreen` clears have no test harness (play test covers them).
  - Manual-check agent: all 5 checks pass (commands, rooms, spawns, `player_2`, exit span, head room under the ledge 3 tiles, ledge reachable, `room14` gates cover both exits so `CameraRoomExits` skips them).
- Owner question after the review: is `LevelState.gatheredExitSide` a smell? 6 read-only agents (ownership, co-op roadmap, skeptic; and for `LevelState` as a whole: lifetimes, concerns, history). Owner decisions D3 (the transition owns the value) and D4 (the other `LevelState` findings become new items on `master` after `047i`), in `SCOPE.md`. Scope change: `SCOPE.md`, `CONTRACT.md`, `PLAN.md` revised; brief to be republished.
- Diffity server stopped (no open threads) before the redesign.
- Revised plan steps 1 to 5: `LevelState.kt`, `LevelRoomHandler.kt` field clear and `MegaLevelScreen.kt` restored to the branch start; `CameraRoomTransition.axisOnly` (set by `start`, cleared at `END` and by `cancel()`); `CameraRoomBounds.transitionToRoom(..., axisOnly)`; `CameraRoomExits` passes `axisOnly = true`; `RoomTransitionEventKeys.AXIS_ONLY` (`PropertyKey<Boolean>`) on `BEGIN` and `CONTINUE`; `MegaPlayer` reads it with `!!` (fail fast: `LevelRoomHandler` is the only sender). `ProtomanTest` builds a `BEGIN_ROOM_TRANS` by hand, so it now adds the key. New `CameraRoomTransitionTest` (engine, 3 cases). `engine:test` 713 / 0 failed; `core:test` 2420 / 0 failed.
- Revised step 6, mutation batch (1 run, 5 mutations): END clear removed → fails "is set by the start and cleared when the pan ends"; cancel clear removed → fails "is cleared by cancel"; writer passes `false` → fails "starts an axis-only pan at a gathered exit"; handler sends `false` → fails "puts the axis-only value of the transition on the begin and continue events"; reader sets the full point → fails "keeps its own y and takes only the path x in an axis-only pan". Code restored (clean status).
- Revised step 7: all automated checks pass (exit 0).
- Brief: D3 and D4 marked, rendered (fixed a sideways scroll at 390px from 2 eyebrow chips), republished to the same link.
- D4 work (new items and the bug file) waits for the merge of `047i`, on `master`.
- Owner question: does the reader handle a vertical transition? No: only side exits pass `axisOnly = true`, and a vertical axis-only pan would set the wrong axis. Owner decision: fail fast for now. `CameraRoomTransition.start` throws `IllegalArgumentException` for an axis-only start that is not `LEFT` or `RIGHT`, with a `TODO` that names phase 3 (item `047`). Tests: throw for `UP`, `DOWN`, `null` (pan does not start); a vertical start that is not axis-only still starts.
- Review of the redesign (1 read-only agent, diff only): no bugs, no unnamed behavior change, style and scope clean. 2 low test gaps, both fixed: the "shared" half of the `LevelRoomHandlerTest` case now also checks `CONTINUE_ROOM_TRANS`; a `CameraRoomTransitionTest` case shows that the latest start wins (a gate during a gathered pan).
- Mutation: the check removed → fails "throws for a vertical or undirected axis-only start, and does not start the pan"; restored with Edit (the auto-mode classifier blocked a `git checkout --` restore). Engine tests 716 / 0 failed. All automated checks pass (exit 0).
- Owner play test: passed (2026-11-17). Diffity: 0 threads. Owner idea at the `room14` check: a gate should need both players, as a side exit does; this is phase 4 of the shared screen (item `047`), the next branch.
- Pre-merge code review: the `/code-review low` skill run reported "no diff" (it read the main checkout on `master`); a review agent with the worktree and the explicit range `4bfc07420..84c82d8d9` runs in its place.
- `master` since the branch start: 67 files, none shared with the branch (Desert Man art and map, docs). No merge of `master` into the branch.
