# Process log: mega-refactor-016c-entity-services

## Status

- State: active
- Goal: Entities take `MegaServices` in place of `MegamanMaverickGame`; factory, test helper, `IMegaGameEntity` cleanup; the `016a` forwards become direct reads
- Merge target: the parent branch `mega-refactor-016-master` (not `master`)
- Done: branch start; scan (`docs/SCAN.md`); `docs/SCOPE.md` (approved 2026-10-29); `docs/CONTRACT.md` (approved 2026-10-29); `docs/PLAN.md` (approved 2026-10-29)
- Done also: plan steps 1 to 8 (2026-10-29); the automated contract checks pass
- Done also: the owner's play test passed (2026-10-29); plan step 9: the temporary suite is deleted, the automated
  checks pass again
- Next: the owner's approval to merge into `mega-refactor-016-master` (squash in the parent's worktree; no merge into
  `master`). At that merge, the full SHA of the `016c` squash commit goes into `.git-blame-ignore-revs` in a separate
  commit in the parent's worktree
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-29

- Branch start on `master`. Worktree `.worktrees/mega-refactor-016c-entity-services`, created from the parent `mega-refactor-016-master`.

## 2026-10-29: scan and scope (gate 1)

- Read the root `CLAUDE.md`, `docs/mega-refactor/CLAUDE.md`, the `016` `ITEM.md` and `NOTES.md`, the `016a` and
  `016b` branch folders, and item `072`.
- Baseline on the branch start `9ba76c833`: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean
  engine:test core:test lwjgl3:jar` passes; 575 engine and 249 core tests, 0 failures; 81 unique warnings. Same as
  the parent baseline.
- Two scratch probes on the base, deleted at once (`git status` clean after each): a constructor test (436 entity
  classes build by their one-argument primary constructor; only `PushableBlock.InnerBlock` has 2 parameters), and a
  compile of the `IMegaGameEntity` cleanup (2 breaks, both fixed by a generic bound with no call site change).
- `docs/SCAN.md` written. `docs/SCOPE.md` written (draft) with 12 open questions.

## 2026-10-29: scope approved; contract draft (gate 2)

- Owner, through the coordinator (2026-10-29, verbatim): "Approved". Questions 3 to 10 and 12 are accepted as
  recommended. Changed by the owner, recorded on `master` as `016` decisions 13-15:
  - Question 1 (verbatim): "I agree with your assertion. I think it's worth keeping in mind though that it might be
    best for a \"camera service/component/helper/manager\" to handle things like these fields instead of directly
    tying them to the camera itself." New class `GameCameraState` (`rotating`, `focusSnappedAway`), holder field
    `camera`.
  - Question 2: "Agreed" to a small settings object: new class `GameSettings` (`performance`), holder field
    `settings`.
  - Question 11 (verbatim): "I agree with you, remove them." The 3 defaults are removed, not moved; 48 callers change.
- `SCOPE.md` updated and marked approved; the answers are in `docs/NOTES.md` "Decisions".
- `docs/CONTRACT.md` written (draft).

## 2026-10-29: contract approved; plan draft (gate 3)

- Owner, through the coordinator (2026-10-29, verbatim): "approved". `CONTRACT.md` is marked approved.
- Owner, through the coordinator (2026-10-29, "yes"): a note in `SCOPE.md` (not a scope change). The owner wants the
  camera to become "a raw camera with components attached to it that affect is behavior and rules" (item `051`,
  recorded on `master` in items `051` and `058`). `GameCameraState` is the read side for entities; until `051` it
  holds state only (the two flags); no camera behavior or logic moves into it.
- `docs/PLAN.md` written (draft).

## 2026-10-29: plan approved; scope addition (the `Performance` enum)

- Owner, through the coordinator (2026-10-29, verbatim): "I approve moving the enum out." and "plan approved". The
  nested enum `MegamanMaverickGame.Performance` becomes a top-level `Performance` next to `GameSettings`; its 5 user
  files change (`Bat.kt`, `FloatingCan.kt`, `MegamanMaverickGame.kt`, `GameSettingsPanel.kt`, `DesktopLauncher.kt`).
  The contract check "no code line in `entities/` names `MegamanMaverickGame`" stays strict. Recorded in `SCOPE.md`
  and `PLAN.md` (step 2) as an owner-approved scope addition. `PLAN.md` is marked approved.

## 2026-10-29: implementation, plan steps 1 to 8 (gate 4)

- Baseline at `f21b3e037`: as at the branch start (575 engine, 249 core, 81 warnings).
- Step 1 (`3a40b28f1`): `Temp016cFrozenGameForwards` and `Temp016cForwardsEquivalenceTest` (200 seeded sequences of 60
  writes); the new side calls the old game functions. Core 250.
- Step 2 (`b0469276d`): `Performance` (top level, `state/`), `GameSettings`, `GameCameraState`, the `LevelState`
  members; the game members go; `S/step2_rewrite.py` changed 163 lines; `S/step2_check.py`: 142 line pairs, 0 problems.
  No setter call was an expression, so no hand edit at a call site. Hand edits: `MMG` (by hand), the `GameSettingsPanel`
  import, the 10 test stubs of the current level (`every { game.levelState } returns LevelState().apply { ... }`),
  `LevelStateTest` (the game-function cases are now `LevelState` cases with the same expectations), the suite's new
  side, 4 unused imports in `MMG`. New tests: `GameCameraStateTest`, `GameSettingsTest`. Core 258.
  - Plan deviation: `MegamanMaverickGameParams.performance` is `lateinit`, and unit tests build `MegamanMaverickGame()`
    with no performance. So `GameSettings.performance` is `lateinit` too, and `create()` sets it from
    `params.performance` as its first line (not a constructor copy). Before `create()` a read throws, as
    `getPerformance()` did. `GameSettingsTest` checks the unset read, a set value, the session object and the enum
    values; the start value from the launch flag is checked by the smoke launch with `--performance low` and manual
    check 8.
  - The existing comment on the focus-snapped flag moved from the game function to `GameCameraState.focusSnappedAway`.
- Step 3 (`66fd31000`): `players` and `gravity` before `MegaEntityFactory.init`. Smoke launch `INTRO_STAGE room1`: 20
  seconds, no exception.
- Step 4 (`6fee3e683`): `IMegaGameEntity` cleanup; the 3 defaults removed; `S/step4_rewrite.py` changed 47 lines (48
  calls); `S/step4_check.py`: 47 line pairs, 0 problems.
- Step 5 (`d07a433eb`): `MegaServices`; `S/step5_swap.py` changed 954 lines in 452 main files and 17 tests;
  `S/step5_check.py`: 950 line pairs, 0 problems. Hand edits outside the token rule: `MegaServices.kt`, the
  `postCreate` lines, `GameEntityUtils` (`game.groups`, and its unused `GroupsSystem` import), `LightSourceUtils` (an
  unused game import from before this branch), `CartV2ItemTest` (`megaContactRules` gets its own relaxed host mock;
  the contact rules keep the host until `016d`), `DrWilyTest` (the line `verify(exactly = 0) { game.setCurrentScreen(any(),
  any()) }` and its unused import go: the holder has no `setCurrentScreen`, so `DrWily` cannot call it; the
  `GAME_COMPLETE` asserts stay). Two smoke launches, no exception.
- Step 6 (`ebdd560d7`): `TestServices.kt` (`testServices(...)`; a real `RotatableCamera` builds in a unit test). The
  17 tests use it; stubs of holder fields became arguments; tests that stub `players.targetFor` pass a relaxed
  `MegaPlayers` mock; 4 setups are reordered so that the arguments exist first (`DrWilyTest`, `MegamanStunTargetTest`,
  `PlayerItemRuleTest`, `CartV2ItemTest`). No assert line changed. New: `EntityConstructorsTest` (more than 400
  classes, `InnerBlock` skipped), `MegaEntityFactoryTest`. Core 260.
- Step 7 (`2de63cb0d`): root `CLAUDE.md`.
- Step 8: all automated contract checks pass (`CONTRACT.md` "Results"). 3 smoke launches, no exception.
- Small change outside the scope (owner, 2026-10-29, verbatim: "Go ahead and do that for me now in this same feature
  branch."): `run-game-alpha.sh`, `run-game-debug.sh` and `run-game-test.sh` no longer pass `--performance medium`.
  The owner's manual check 8 (`--performance low`) failed with "Can only specify option --performance once", because the
  scripts already passed the option. The default in `DesktopAppArgs` is `MEDIUM` (`DesktopLauncher.kt:29`), so a run
  without the option behaves the same. No game code changes.

## 2026-10-29: play test passed; temporary suite deleted (plan step 9)

- Owner, through the coordinator (2026-10-29, verbatim): "All tests pass". All 16 manual checks and the behaviors in
  `CONTRACT.md` pass. Manual check 8 ran with the run script after `29bbb5e04` (no `--performance` in the scripts).
- `22338b9e5`: `Temp016cForwardsEquivalenceTest` and `Temp016cFrozenGameForwards` deleted in their own commit.
- Automated checks again: clean build passes; engine 575, core 259, 0 failures; 81 unique warnings, no new one; no
  `Temp016c` name left. No game launch.
- `docs/TODO.md` at the merge: no backlog candidates, no bug candidates.
