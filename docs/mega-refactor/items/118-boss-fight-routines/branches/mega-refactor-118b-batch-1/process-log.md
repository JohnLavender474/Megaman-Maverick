# Process log: mega-refactor-118b-batch-1

## Status

- State: merged
- Goal: `TimberWoman`, `MoonMan` and `ReactorMan` (6 fights) on the fight flow
- Done: scan; `SCOPE.md`, `CONTRACT.md` and `PLAN.md` approved; all plan steps; merged into `master` as `cd1af498b`
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-02

- No branch exists yet. Scan at `16a708322`, and draft `SCOPE.md`. A mid-level fight needs no separate plan: only the exit event differs, and the boss gives it.
- Later the same day, the owner rejected `LevelVictory` for a mid-level fight. The research on the 1-frame shift (`docs/NOTES.md`) found no adverse effect. The owner chose 2 plans (`standardBossPlan`, `midBossPlan`) with the resume as a step, and approved `SCOPE.md`.

## 2026-11-03

- The owner approved `CONTRACT.md`. Draft `PLAN.md`.
- The owner approved `PLAN.md`. Branch start: `ITEMS.md` row `active`, `ITEM.md` "Branches", and the worktree.
- Step 1: base `engine:test core:test` pass at `2db9e6ffc`. No overlap with `compactor-level` or `x6-intro-extra-level` (`docs/NOTES.md`).
- Step 2: `FightPlan.music` is nullable, `resolve` skips a missing music, `stageBossPlan` is `standardBossPlan`, and `LevelVictory` sends `VICTORY_EVENT`. 2 new tests. Mutation check: `LevelVictory` back to `getEventOnDefeated()`, and `resolve` writing `plan.music?.name` (both seen in `git diff`); both new tests failed, and passed after the undo. `core:test`: 796 tests, 0 failures.
- Step 3: `ExitMidBossFight`, `ResumeAfterMidBoss`, `AwaitBossGone`, `midBossPlan`; `find` takes the map props; `FightDirector.start` and `Fight` take the level music supplier, and the screen passes `{ music }`. The test fight has 2 players now. 6 new tests. Mutation checks, one at a time, each seen in `git diff`: the exit event type, `ExitMidBossFight` finished with slot 0 dead, no `TURN_CONTROLLER_ON`, no unlock, no level music, no wait for the boss to go, the `end` default `false`. Each one failed at least 1 new test, and all passed after the undo. `core:test`: 801 tests, 0 failures.
- Step 4: `TimberWoman`, `MoonMan` and `ReactorMan` in `find`, with no music, and the `fights.owns` guard in the screen's `INTERMEDIATE_BOSS_DEAD` branch, in one commit. 3 new tests in `FightTest` and 2 in `MegaLevelScreenEventsTest`. The "no plan" list now also names `MechaDragon_OLD`, `ReactorMan_OLD` and `SphinxMiniBoss`. Mutation checks: the guard removed (1 test failed), `MoonMan` left out of `find` (2 tests failed); both seen in `git diff`, and all passed after the undo. `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes: engine 636 tests, core 804 tests, 0 failures.
- Step 5: the `fights` README has a generic mid-level example, the map props choice, the supplier for a value that the screen can change, and the rule that a fight can stay open for the `run` that delivers its last event.
- Step 6: one 12 s launch of the jar for each of `TIMBER_WOMAN room14`, `MOON_MAN room14`, `REACTOR_MAN room14`, `WILY_STAGE_1 room6` and `WILY_STAGE_3 room6`. Each one ran until the time limit, with no exception. The only error line is the known `DuoBallCanon` spawner error in `WILY_STAGE_1`.
- Step 7: the owner ran the play checks in `CONTRACT.md` (7 experimental, 4 control). All passed. No fix was needed.
- Step 8: the owner approved the merge. Squash commit `cd1af498b` on `master`.
