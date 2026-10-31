# Process log: mega-refactor-084b-effects

## Status

- State: active
- Goal: Trail sprites, underwater bubbles, the death orbs and the out-of-bounds kill out of `Megaman`; the game plays the same.
- Done: branch start (docs commit on `master`), worktree, scan, `docs/SCOPE.md` (approved 2026-10-30), `docs/CONTRACT.md` (approved 2026-10-31), `docs/PLAN.md` (approved 2026-10-31), plan steps 1-9, and step 10 up to the merge.
- Next: the owner's approval of the merge.
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- Branch start. The owner chose this as the next `084` branch. Created the branch and its worktree
  `.worktrees/mega-refactor-084b-effects/` from `master`. The first try ran from the wrong folder and made the worktree in `docs/mega-refactor/.worktrees/`; I removed it and
  the empty branch, and made them again. Nothing was lost.
- Scan of the trail, the bubbles, the orbs and the out-of-bounds kill. Draft `docs/SCOPE.md` written for approval.
- The owner approved `docs/SCOPE.md`, with a change: the speed is `DEFAULT_EXPLOSION_ORB_SPEED`, and the spawn props allow an optional override (see `docs/NOTES.md`, "Decisions"). Draft `docs/CONTRACT.md` written.

## 2026-10-31

- The owner approved `docs/CONTRACT.md`. The checks then took the new order in `docs/mega-refactor/CLAUDE.md`, "Experimental checks come first", with no change to the checks. Draft `docs/PLAN.md` written (10 steps).
- The owner approved `docs/PLAN.md`.
- Step 1: baseline `./gradlew engine:test core:test lwjgl3:jar` passes (943 tests, 0 failures, 0 errors).
- Step 2: a subagent added `out_of_bounds` to `RefactorTest084.tmx` (map 112 x 32 tiles; room columns 90-109, pit columns 98-101, spawn `4`; a ceiling was added to match the other rooms). The old rooms did not change. Launches on the baseline code: no exception in
  15 launches of `out_of_bounds` (6 by me, with exit code 124 and a 25 s `timeout`) or in 5 of `carry_left`. The bounds error printed in 3 of the 15 `out_of_bounds` launches, and no launch sent input. A temporary position log (removed, not committed) showed one such case: `x=3201`, `y=-53`,
  in the pit columns and just past the limit of -48, with `inPlay` true. So Mega moved from the spawn into the pit, and the kill fired at the expected place. The cause of the movement is not proven; the likely cause is key or gamepad input that reached the game window. The owner's play test covers it.
- Step 3: `BubbleEmitterComponent`, `M/components/EffectComponents.kt`. `core:test lwjgl3:jar` passes; the tests failed first, at compile.
- Step 4: `TrailSpriteComponent`. `core:test lwjgl3:jar` passes; the tests failed first, at compile.
- Step 5: `isOutsideLevel` in `C/entities/utils/LevelBounds.kt`. `core:test` passes; the test failed first, at compile.
- Step 6: `ExplosionOrb.spawnRing`, `DEFAULT_EXPLOSION_ORB_SPEED` and the optional `ConstKeys.SPEED` prop. The 2 orb tests failed on the old code (`(-1, 1)` in place of `(-112, 112)`), then passed. The 8 new directions have the same order and signs as the 2 old tables. The test adds the body directly, because `init()` needs a real texture atlas.
- Step 7: the diff matches `docs/CONTRACT.md`, "Frame order". The 3 `grep` checks find nothing. Unused imports removed.
- Step 8: `./gradlew engine:test core:test lwjgl3:jar` passes (961 tests, 0 failures, 0 errors). Short launches of `out_of_bounds`: no exception in 2 launches (ended by `timeout`, code 124). `Megaman.kt` has 694 lines, down from 789.
- Step 9: the owner's play test passed (2026-10-31, owner: "tests pass"): all checks in "Manual playthrough" and "Behaviors to check" in `docs/CONTRACT.md`. The owner did not report Mega moving with no input.
- Step 10: the owner approved the merge (verbatim: "yes", then "Proceed with merging the feature branch to master"). `master` then had a code commit `678dbc614` (the owner's import clean-up in `Megaman.kt` and `DamageReactionComponents.kt`), so I merged `master` into the branch. The one conflict was the `entities.state` imports; the result is `entities.state.*` and the `isOutsideLevel` import. `./gradlew engine:test core:test lwjgl3:jar` passes (961 tests, 0 failures, 0 errors), the 3 `grep` checks find nothing, and a short launch of `out_of_bounds` has no exception (code 124).
