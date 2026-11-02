# Process log: mega-refactor-047a-second-slot

## Status

- State: active
- Goal: with `--players 2`, a second `Megaman` plays in a level on its own device
- Done: plan steps 1 to 9
- Next: the owner's merge approval
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-01

- No branch exists yet. The folder was made on `master` ahead of time.
- Scan done. Draft `docs/SCOPE.md` written. The owner approved it, accepted the `O2-R9` gap, and chose `master` as the merge target for all of `047`.
- Branch started.
- Corrected a scope fact (no change of scope): the 2 spawns with `direction=left` are both in `RefactorTest051.tmx`.
- The owner approved `docs/CONTRACT.md`.
- Plan-time checks done: the pad order is the connection order, `room16` holds the boss `Gate`, and the overlap read is in `docs/NOTES.md`.
- The owner approved `docs/PLAN.md`.
- Steps 1 to 8 done. `./gradlew engine:test core:test lwjgl3:jar` passes. Launches with `timeout 12` gave no exception: the 5 rooms on the baseline code, `default_spawn` and `linked_spawn` with `--players 2`, `default_spawn` with no flag, and `DESERT_MAN` `room16` with no flag.
- With `--players 2` and no pad, the log has the no-pad error once.
- Changes from the plan:
  - `MAX_PLAYERS` is a new constant, with `parsePlayers` and `MegamanMaverickGame` as callers. The plan did not list it.
  - Steps 5 to 8 are in one commit, because `MegaLevelScreen.kt` and the handlers mix these steps.
  - There is no automated test for the death slot filter. The rule is in `MegaLevelScreen.onEvent`, and `testServices()` builds services, not the screen. The manual checks `slot_0_death` and `slot_1_death` cover it, as the contract allows.
  - `EndLevelEventHandler` locks only a live player. `LocksComponent` clears only on a destroy, so a lock on a dead slot 1 would stay after the level. The screen's `BEHAVIORS` lock has no other clear path.
- Scope text corrected: item 10 named a `direction=left` room in the test map, but the plan and the contract use `REFACTOR_TEST_051`.
- Next: the owner's play test.
- Owner's play test done, verbatim: "Tests pass". No fix was necessary, so the control checks did not run again.
- Play test finding: in `DESERT_MAN` with `--players 2`, when both players stand on quicksand, neither player can get out. The owner's report names the single `FeetRiseSinkBlock` as the cause. The research is done, and the issue is `docs/issues/2026-11-01-two-players-stuck-in-quicksand.md`. The owner decided that each sinkable entity sinks and rises on their own. The bug goes in "Known wrong with `--players 2`" in `docs/CONTRACT.md`.
- The owner approved the merge, verbatim: "Yes, 047a tests passed".
