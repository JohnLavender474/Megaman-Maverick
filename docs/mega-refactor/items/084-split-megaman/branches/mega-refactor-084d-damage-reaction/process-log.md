# Process log: mega-refactor-084d-damage-reaction

## Status

- State: merged
- Goal: Stun, knockback, recovery flash and freeze as player components, one per mechanism; the game plays the same.
- Done: branch start (docs commit on `master`), worktree, scan, `docs/SCOPE.md` and `docs/CONTRACT.md` (approved 2026-10-30), `docs/PLAN.md` (all approved 2026-10-30), plan steps 1-9.
- Next: plan step 10 (the owner's play test).
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- Branch start. The owner chose this branch as the next `084` branch (see the 084 `ITEM.md`, "Decisions"). Created the
  branch and its worktree `.worktrees/mega-refactor-084d-damage-reaction/` from `master`.
- Overlap check with the active branches: see `docs/NOTES.md`.
- Scan of the 4 mechanisms, their readers, the tests and the overlap. Draft `docs/SCOPE.md` written for approval.
- The owner approved `docs/SCOPE.md` with all 3 recommendations (see `docs/NOTES.md`, "Decisions"). The `SPAWN` lock is
  in `docs/TODO.md` as a bug candidate. Draft `docs/CONTRACT.md` written.
- The owner approved `docs/CONTRACT.md`. Draft `docs/PLAN.md` written (11 steps).
- The owner approved `docs/PLAN.md`.
- Step 1: baseline `./gradlew engine:test core:test lwjgl3:jar` passes (876 tests, 0 failures, 0 errors).
- Contract correction: the pulser and `TimberWoman` send only `STUN_BOUNCE_IF_ON_SURFACE`, and `STUN_BOUNCE_ALWAYS` has no
  production sender. So a pulse in the air does nothing. The stun check in `docs/CONTRACT.md` now says this. The scope and
  the gameplay do not change, so this needs no new approval; the owner is told.
- Step 2: a subagent built `RefactorTest084.tmx` with the rooms `knockback` (4 static `Saw` objects, 4 `GravitySwitcharoo`
  objects) and `spring_head`. `REFACTOR_TEST_084` added. A short launch of each room on the baseline code: no exception. The
  launches ended by `timeout` (code 124). `Saw` with `length=0` has no precedent in a map; the owner's play test confirms that
  it stays still. The 8 hits of the `knockback` check cost 24 of 30 health on normal mode, and Mega dies before the 8th on hard
  mode. `GLACIER_MAN --room room6` has `Coldier` (`SmallIceCube`) and `IceFox` (`IceBomb`), so no `freeze` room is needed.
- `master` (`6b0097aa1`, with `051b`) merged into the branch before the code steps, with no conflict.
- Step 3: frozen copy `Temp084dFrozenDamageReaction` and `Temp084dDamageReactionEquivalenceTest` added; `core:test` passes.
- Step 4: `KnockbackComponent`, `knockBackFrom`, `defineKnockbackComponent()`. `noDmgBounce` and the `when` left `Megaman`. `core:test lwjgl3:jar` passes, with `KnockbacksTest` and the temporary comparison.
- Step 5: `FreezeComponent`, `IThawRule`, `MashThawRule`, `Freezes.kt`, `defineFreezeComponent()`. The state and `IFreezableEntity` left `Megaman`; 4 readers import the extension. `core:test lwjgl3:jar` passes. The temporary comparison matches on 20 seeds of 3000 frames.
- Step 6: `StunComponent`, `Stuns.kt`, `defineStunComponent()`. The state left `Megaman`; `Megaman` stays the listener and calls `onStunPlayerEvent`. `MegamanStunTargetTest` uses `defineStunComponent()`, with 2 more cases. `core:test lwjgl3:jar` passes. The temporary comparison matches on 20 seeds of 3000 frames.
- Step 7: `DamageRecoveryComponent`, `defineDamageRecoveryComponent()`, the `damageRecovery` field. `invincible` and `SpritesComponent` read the field; a `TODO` at the getter names `060`. `core:test lwjgl3:jar` passes. New `DamageRecoveryComponentTest` and `MegamanInvincibleTest`. The temporary comparison matches on 20 seeds of 3000 frames.
- Step 8: the destroy window. Each reader of `stunned`, `frozen` and the flash runs inside the player's own systems, or in `canFireWeapon` from them, so none runs while Mega is out of the engine. `GameEngine.destroyNow` and `GameEngine.reset` both call component `reset()`, so a spawn after a destroy starts clean. The lambda diff matches the table in `docs/CONTRACT.md`. Both `grep` checks find nothing.
- Step 9: `./gradlew engine:test core:test lwjgl3:jar` passes, and a short launch of `REFACTOR_TEST_084` has no exception (ended by `timeout`, code 124).
- Step 10: the owner's play test passed (2026-10-30, owner: "All other tests pass"): the manual checks in `docs/CONTRACT.md`. The `--diagnostics` captures were not done: the owner skipped them ("I'll skip this for now"), so the frame-time effect is not measured.
- Step 11: the temporary suite is deleted, and `KnockbacksTest` helpers are private again. `./gradlew engine:test core:test lwjgl3:jar` passes (922 tests, 0 failures, 0 errors). `master` has no new commits. Overlap with `031e`: see `docs/NOTES.md`.
- The owner approved the merge (verbatim: "yes"). `master` then had no new code commits, so no second merge into the branch.
