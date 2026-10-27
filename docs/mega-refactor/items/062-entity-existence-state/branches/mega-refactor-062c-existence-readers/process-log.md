# Process log: mega-refactor-062c-existence-readers

## Status

- State: active
- Goal: readers of `dead` and `spawned` ask named questions of the existence state
- Done: planning (scope, contract, plan approved); steps 1 to 11
- Next: owner merge approval
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-27

- Branch start on `master`. Worktree `.worktrees/mega-refactor-062c-existence-readers`.
- Scan: all readers of `MegaGameEntity.dead` (about 88 in 60 files) and `IGameEntity.spawned` (12 in `core`, 2 in the
  engine), grouped by the question each one asks (scratchpad `062c-dead.txt`, `062c-spawned.txt`). Every `dead`
  reader asks "is the entity live after the next update?", also for the player (a death is a destroy). Two holders
  mix the two questions (`WilyFinalBoss` orbs, `FreezableEntityHandler` shield); they stay as they are. Draft
  `docs/SCOPE.md` with 3 open questions (the names, a player-level `dead`, remove the old getters here or in `062d`).
- Owner review of the draft: no more flags on the entity. Decision (owner, "Go for it"): `liveNow` and
  `liveAfterUpdate` are constructor values of `EntityExistenceState`, one row per state (`docs/NOTES.md`). Open
  question 1 closed; `docs/SCOPE.md` updated. Questions 2 and 3 stay open with the same recommendations.
- Owner (2026-10-27, "Go with your recommendations"): questions 2 and 3 closed as recommended (no player `dead`;
  remove `spawned` and `dead` here). Scope approved. Draft `docs/CONTRACT.md`: a temporary suite on the five states, a
  substitution check script over the diff, the compiler after the removal, and nine short manual checks (rooms found
  in the live TMX files with scratchpad `find_rooms.py`).
- Contract approved (owner, 2026-10-27: "yes"). Draft `docs/PLAN.md` (11 steps): baseline, the enum table and its
  tests, the rewrite and check scripts, five batches, the removal of the old getters, the contract checks, and the
  deletion of the temporary suite.
- Plan approved (owner, 2026-10-27: "yes").
- Step 1 (baseline): clean build passes; 539 engine and 172 core tests, 0 failures; 110 unique warnings (scratchpad
  `062c-warnings-baseline.txt`).
- Step 2: `EntityExistenceState(liveNow, liveAfterUpdate)`; the permanent table test in `EntityExistenceStateTest`;
  the temporary `TempOldExistenceFlagsTest` with the frozen `TempOldExistenceFlags` (all five states match).
  541 engine tests, 0 failures.
- Step 3: scratchpad `062c-rewrite.py` and `062c-check-substitutions.py` (the check undoes the renames with its own
  code). Self-test on a hand-made file of 12 reads (including a comment and a string): the rewrite skips the comment
  and the string; the check passes the correct diff and reports a diff with a lost `!`.
- Steps 4 to 8 (one commit each): bosses (27 lines), enemies and base classes (21), special/blocks/hazards/
  projectiles/sensors/utils (33), Megaman (15), screens/game/engine (9), then the tests by hand (the test helpers
  `IGameEntity.dead` removed; test names that named the flags updated). Each batch: compile, check script with no
  unmatched line except the reviewed ones below, all tests pass.
- Casts dropped where the value is already an `IGameEntity` (reviewed by hand; the check lists them as unmatched with
  the right sign): `CapsuleTeleporter.kt:245,270`, `GravitySwitcharoo.kt:218` (its `MegaGameEntity` import removed),
  `DrippingToxicGoop.kt:111`, `RotationAnchor.kt:114`; `PetitDevil.kt:300` (`parent` is an `IGameEntity?` `var`, so
  the cast became `parent!!`). Kept: `FreezableEntityHandler.kt:93` and `ShieldEntity.kt:68` (`IFreezableEntity` does
  not extend `IGameEntity`).
- Owner question during step 6 (should every entity contract such as `IFreezableEntity`, and `IDamageable`/`IDamager`
  renamed with an `Entity` suffix, extend `IGameEntity`, or be components?): a read-only review agent was sent; out of
  this branch's scope.
- Step 9: `IGameEntity.spawned` and `MegaGameEntity.dead` removed; `toString` prints `existenceState`. Clean build
  passes; 541 engine and 172 core tests, 0 failures; the same 110 warnings (compared without line and column);
  `contact-rule-selection.txt` unchanged. Check on the full branch diff for `main`: 99 lines matched; 25 unmatched,
  all expected (the enum, the removed getters, `toString`, two imports, the six cast sites).
- Review agent report (contract interfaces): no entity contract in `core`, and neither `IDamageable` nor `IDamager`,
  has a non-entity implementer, so all can extend `IGameEntity`; `IDirectional`, `IActivatable`, `IEventListener`,
  `IDamageNegotiator`, `IFaceable`, `ISizable` cannot or should not. Recommendation: a type-only clean-up first (two
  branches: contracts plus casts plus `destroy()` on `IGameEntity`; the damage rename alone), then state-only
  contracts to components inside `048`/`023`. Written as a backlog candidate in `docs/TODO.md`.
- Second review agent (owner question: `spawn`/`destroy` on `IGameEntity`): recommends `destroy()` only (as
  `IGameEntity : IDestroyable`), in the first branch of the "entity contracts" backlog candidate; `spawn` has no caller
  that needs it. It found the `Starkner` `ALIVE` cleanup in `destroy()` (checked in the code): a bug candidate in
  `docs/TODO.md`.
- Step 10: the contract's automated checks passed at step 9. Owner play test (2026-10-27): "All tests pass for 062c".
- Step 11: `TempOldExistenceFlagsTest` (with the frozen `TempOldExistenceFlags`) deleted. Clean build passes; 540
  engine and 172 core tests, 0 failures; the same 110 warnings; the jar builds.
