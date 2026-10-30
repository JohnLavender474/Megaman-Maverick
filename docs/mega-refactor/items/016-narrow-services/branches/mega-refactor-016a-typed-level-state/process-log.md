# Process log: mega-refactor-016a-typed-level-state

## Status

- State: active
- Goal: Typed `LevelState` fields and camera flags replace the `game` property bag
- Merge target: the parent branch `mega-refactor-016-master` (not `master`)
- Done: branch start; scan (`docs/SCAN.md`); `docs/SCOPE.md` (approved 2026-10-28); `docs/CONTRACT.md` (approved 2026-10-28); `docs/PLAN.md` (approved 2026-10-28)
- Done also: plan steps 1 to 7; the owner's play test passed (2026-10-28); the temporary suite is deleted
- Next: the owner's approval to merge into `mega-refactor-016-master` (squash in the parent's worktree; no merge into
  `master`)
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-28

- Branch start on `master`. Worktree `.worktrees/mega-refactor-016a-typed-level-state`.

## 2026-10-28: scan and scope (gate 1)

- Read the root `CLAUDE.md`, `docs/mega-refactor/CLAUDE.md`, the `016` `ITEM.md` and `NOTES.md`, and the `064b` branch
  folder as the model.
- Coordinator message: the branches of `016` merge into the parent `mega-refactor-016-master`, not into `master`
  ("Parent branches" in `docs/mega-refactor/CLAUDE.md` on `master`). Written in the status block, `SCOPE.md` and
  `docs/NOTES.md`.
- Baseline on the branch start `44c6b5fa3`: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean
  engine:test core:test lwjgl3:jar` passes; 571 engine and 219 core tests, 0 failures; 87 unique warnings. Same as
  the `master` baseline in the brief.
- Scan (`docs/SCAN.md`): 58 external bag calls in 30 files on 8 keys, 11 internal keys, the default and the set and
  clear moments of each key. Compiler probe (one local `core:compileKotlin`, reverted, `git status` clean): with the
  7 bag functions overridden as `@Deprecated(level = ERROR)` in the game and the internal calls routed through
  `properties`, the build reports exactly the 58 `grep` sites. A first try that only removed `IPropertizable`
  stopped at the errors inside `MegamanMaverickGame.kt`.
- `docs/SCOPE.md` written (draft) with 13 open questions.
- `docs/TODO.md`: one bug candidate (the `EndLevelEventHandler` beam flag).

## 2026-10-28: scope approved; contract draft (gate 2)

- Owner, through the coordinator (2026-10-28, verbatim): "Scope approved". All 13 recommendations are accepted.
  `SCOPE.md` is marked approved; the answers are in `docs/NOTES.md` "Decisions".
- The bug candidate in `docs/TODO.md` (the `EndLevelEventHandler` beam flag) was filed on `master` at the owner's
  request: `docs/issues/2026-10-28-end-level-beam-flag-left-true.md`.
- `docs/CONTRACT.md` written (draft): automated checks (build and tests against the baseline, `grep` for bag calls and
  the removed `ConstKeys`, the temporary suite, the permanent tests, two smoke launches), 10 manual checks in real
  rooms (rooms from the live TMX files), no intended changes, and the 2 unreachable differences of open question 7.

## 2026-10-28: contract approved; baseline run; plan draft (gate 3)

- Owner, through the coordinator (2026-10-28, verbatim): "Manual checks pass. Contract approved". `CONTRACT.md` is
  marked approved.
- Baseline run: the owner ran the 10 manual checks of `CONTRACT.md` before any code change, so on the old code
  (branch at `c582a5848`, code equal to `44c6b5fa3`). Result: pass. This is not the contract play test; the play
  test after the implementation is still required.
- Checked for open question 13: a real `MegamanMaverickGame()` builds in a plain `core` unit test (a scratch test,
  `core:test` run once, then deleted; `git status` clean). The temporary suite can use a real game object.
- `docs/PLAN.md` written (draft): 7 steps; step 3 has one commit per key group (3a to 3d).

## 2026-10-28: plan approved; implementation

- Owner, through the coordinator (2026-10-28, verbatim): "plan approved". `PLAN.md` is marked approved.
- Steps, one commit each (each: `clean engine:test core:test lwjgl3:jar` green, no new unique warning):
  - 1 `1c3e0579a`: `LevelState` (5 fields) and the private typed fields of the game for the 11 internal keys. The
    5 setters that returned the old map value (`Any?`) now return `Unit`; no caller used the value.
  - 2 `40021f1ea`: `Temp016aFrozenGameBag`, `Temp016aBagEquivalenceTest` (200 seeds x 200 write moments) and
    `LevelStateTest`. Mutation check by hand: `cameraRotating` starting `true` made both suites fail; reverted.
  - 3a `babaa026c`: `roomTransition` (21 reads, 5 writes). The commented line in `ReactorMan_OLD.kt:354` follows,
    so the contract `grep` for the removed `ConstKeys` stays clean. One unused `ConstKeys` import removed
    (`ControllerComponent.kt`). Docs: the counts in `SCAN.md`, `SCOPE.md` and `PLAN.md` are corrected:
    `ROOM_TRANSITION` has 26 calls (21 reads), `WEAPONS_ATTAINED` 5 external calls; the first draft copied 27/22 and
    4 from the item `NOTES.md`. The total of 58 and the scope do not change.
  - 3b `c06b16486`: `megamanBeam`, `megamanBeamCenter` (10 calls).
  - 3c `4babd9331`: `coins`, `levelPoints`, `timeSinceLastFloatingPoints`, `lastFloatingPointsType` (17 calls).
  - 3d `c02b62796`: `game.weaponsAttained` (5 calls and `resetPlayerProgress`). Unused `OrderedSet` import removed
    from `GetWeaponScreen.kt`; the KDoc of `resetPlayerProgress` names the field. After this step no code calls the
    bag.
  - 4 `72f5d3428`: the game is no longer an `IPropertizable`; the 11 unused `ConstKeys` are gone.
  - 5 `6fd9ae190`: root `CLAUDE.md`.
- Step 6, automated contract checks on `6fd9ae190`: all pass.
  - Build green: engine 571, core 226 (219 + `Temp016aBagEquivalenceTest` 1 + `LevelStateTest` 6), 0 failures.
  - Warnings: 81 unique, no new one; 6 unchecked-cast warnings are gone (5 in `MegamanMaverickGame.kt`, 1 in
    `GetWeaponScreen.kt`).
  - `grep` for `game.*Property(`: 0 lines. `grep` for the 11 removed `ConstKeys`: 0 lines. No `IPropertizable` and
    no `properties` in `MegamanMaverickGame.kt`.
  - The 8 tests that stub `getCurrentLevel()` pass with no change.
  - Smoke launches (`timeout 45` around `run-game-alpha.sh`, which includes the Gradle start-up; about 20 to 25
    seconds of game): `--level SMB3_1 --room room2` and `--level WILY_STAGE_1 --room room8`. Both ran to the timeout
    with no exception. The only `ERROR` line is the known content issue
    `docs/issues/2026-10-27-wily-stage-1-duoballcanon-on-blocks-layer.md` (not caused by this branch).
- Waiting for the owner's play test.

## 2026-10-28: play test passed; step 7

- Owner, through the coordinator (2026-10-28, verbatim): "All tests pass". The play test of the 10 manual checks of
  `CONTRACT.md` on the implemented code passed. Written in `CONTRACT.md` "Results".
- Step 7: `Temp016aBagEquivalenceTest` and `Temp016aFrozenGameBag` deleted in their own commit (`8036b34df`).
- Automated checks again (no game launch, as the coordinator asked): `clean engine:test core:test lwjgl3:jar` passes;
  engine 571, core 225, 0 failures; 81 unique warnings, no new one against the 87 of the baseline; `grep` for bag calls
  and for the 11 removed `ConstKeys` gives 0 lines; no `Temp016a` name in the sources.
- `docs/TODO.md`: no backlog candidate. One bug candidate, already filed on `master`
  (`docs/issues/2026-10-28-end-level-beam-flag-left-true.md`). The smoke-launch `ERROR` line of `WILY_STAGE_1`
  is the known issue `docs/issues/2026-10-27-wily-stage-1-duoballcanon-on-blocks-layer.md`.
- Not merged. The merge target is the parent `mega-refactor-016-master`; the merge needs the owner's approval.
