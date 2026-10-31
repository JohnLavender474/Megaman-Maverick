# Process log: mega-refactor-084c-movement

## Status

- State: active
- Goal: Post-action momentum and the room-transition carry as player components, and the `AButtonTask` transitions out of the
  `Megaman` lambda; the game plays the same.
- Done: branch start (docs commit on `master`), worktree, scan, `docs/SCOPE.md` and `docs/CONTRACT.md` (approved 2026-10-30), `docs/PLAN.md` (all approved 2026-10-30), plan steps 1-9, and step 10 up to the merge.
- Next: the owner's approval of the merge. This branch must merge before `051c`.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- Branch start. The owner chose this as the next `084` branch. Created the branch and its worktree
  `.worktrees/mega-refactor-084c-movement/` from `master`.
- Scan of momentum, the room carry and the `AButtonTask` transitions. Draft `docs/SCOPE.md` written for approval.
- The owner approved `docs/SCOPE.md` with both recommendations (see `docs/NOTES.md`, "Decisions"). The room-carry `!!` is in
  `docs/TODO.md` as a bug candidate. Draft `docs/CONTRACT.md` written.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written (10 steps).
- The owner approved `docs/PLAN.md`.
- Step 1: baseline `./gradlew engine:test core:test lwjgl3:jar` passes.
- Step 2: a subagent added `carry_left`, `carry_right` and `carry_top` to `RefactorTest084.tmx` (88 x 32 tiles; a 3-step stair and a ledge
  were needed to reach the gap and the ladder top). The rooms `knockback` and `spring_head` did not change. The short launch of each
  room ran after the code of steps 3-5, not on the baseline, because the map came back later: `carry_left` and `carry_right` load with no
  exception (ended by `timeout`, code 124). `carry_top` has no spawn.
- Step 3: frozen copy `Temp084cFrozenMovement` and `Temp084cMovementEquivalenceTest` added.
- Step 4: `PostActionMomentumComponent`, `PostActionMomentum.kt`, `defineMomentumComponent()`. `core:test lwjgl3:jar` passes, with
  `PostActionMomentumTest`. The temporary comparison matches on 20 seeds of 3000 frames in 2 directions.
- Step 5: `RoomTransitionCarryComponent`, `RoomTransitionCarries.kt`, `defineRoomTransitionCarryComponent()`. `Body.reset()` does not clear the
  bag, so the old keys did survive a death, and `reset()` keeps the carried values. `core:test lwjgl3:jar` passes, with
  `RoomTransitionCarryTest`, which also records that the `!!` throws. The temporary comparison matches, except for the `!!` case itself.
- Step 6: `M/MegamanAButtonTask.kt` with `updateAButtonTask()`; the block is unchanged, and the lambda calls it last.
- Step 7: found a defect in the design: see `docs/NOTES.md`, "Decisions". `onSpawn` calls `PostActionMomentumComponent.reset()`. `docs/SCOPE.md` and `docs/CONTRACT.md`
  updated; the scope change needs the owner's approval. The other readers run inside the player's own systems. Both `grep` checks find nothing. The temporary suite does not
  run the destroy path, so only the owner's death check covers this case.
- Step 8: `./gradlew engine:test core:test lwjgl3:jar` passes. Short launches of `carry_left` and `carry_right`: no exception (ended by `timeout`, code 124).
- Step 8a (scope change, plan change): a request from `051c`, with the owner's approval of the bug fix, added the room-carry crash fix (`docs/SCOPE.md`, "In scope",
  item 7). It goes outside the behavior-preserving move. The cause in the code is confirmed: `LevelRoomHandler.kt:114` submits the event, `:143` sets the gravity at
  once, and `WorldGravity.set` writes the direction with no delay. 5 new tests failed on the old code (3 with `NullPointerException`, 2 on an assertion). After the fix,
  `./gradlew engine:test core:test lwjgl3:jar` passes (946 tests, 0 failures, 0 errors). The temporary carry comparison now uses begin and end pairs with the same `carries`
  value, because the fix changes the other cases on purpose. Short launches of `carry_left` and `carry_right`: no exception (code 124). `edge_rotated` is only on `051c`, so
  the automated tests are the proof here. `WilyStage3_v4.tmx` (`room7`, `room9`) also sets `megaman_direction`, not tested. `RefactorTest051` `gravity_room` was added to
  the play test: it keeps the vertical axis, so it does not reproduce the crash.
- Step 9, in progress: the owner's check of `REFACTOR_TEST_051 --room to_gravity` passes (2026-10-30, owner: "play test in `to_gravity` room passes"). The other checks have no result yet.
- Step 9: the owner's play test passed (2026-10-30, owner: "yes, approved; all other tests pass"): all checks in "Manual playthrough" and "Behaviors to check" in `docs/CONTRACT.md`. The same message approved the reset-order change. `edge_rotated` was not played on this branch; it is check 18 of the `051c` play test.
- Step 10: `Temp084cFrozenMovement` and `Temp084cMovementEquivalenceTest` deleted; `MovedEntity` and `CarriedEntity` are now `private`, because only their own test files use them. `./gradlew engine:test core:test lwjgl3:jar` passes (943 tests, 0 failures, 0 errors; 3 fewer, the temporary tests). Both `grep` checks find nothing. Overlap: see `docs/NOTES.md`. `master` has only docs changes, so no merge of `master` into the branch.
