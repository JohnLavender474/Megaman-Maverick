# Process log: mega-refactor-031f-menu-input

## Status

- State: merged
- Goal: the menus and skip screens read frames, with `Esc` and the back action; the `controllerPoller` facade goes
- Done: the scan, `docs/SCOPE.md`, `docs/CONTRACT.md` and `docs/PLAN.md` (approved), plan steps 0 to 12, a partial play test (see the entry of step 13), the owner's merge approval
- Next: none
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- No branch exists yet. The folder was prepared on `master`. This is the "menu-input branch" of the `031` decisions
  of 2026-10-26 (item `ITEM.md`).
- Proposed goal (not a scope; the scan and `docs/SCOPE.md` decide):
  - The menus (`MegaMenuScreen` and its subclasses), the skip screens (`LogoScreen`, `AbstractStoryScreen`,
    `GameOverScreen`, `GetWeaponScreen`, `SimpleInitGameScreen`, `CreditsScreen`) and `ScreenController` read frames,
    not `game.controllerPoller`.
  - `Esc` is back in menus and pause in gameplay (`031` decision of 2026-10-25, with a `TODO` to consider another
    action later). The back action in menus.
  - Removals when no caller is left: `game.controllerPoller`, `FrameControllerPoller` and `FrameControllerPollerTest`,
    the engine `IControllerPoller`, and the diagnostics entry label `"controllerPoller"` in
    `MegamanMaverickGame.render`.
- The scan waits for `031e` to merge, because `031d` and `031e` change the code that this branch reads.

## 2026-11-01

- No branch exists yet. Scan on `master` at `c6b8cecf7`, read only. The game was not launched. Read: the root and
  `docs/mega-refactor/` `CLAUDE.md`, `docs/issues/README.md` and the 4 open issues on pause and input, the item `ITEM.md` and
  `NOTES.md`, the `031e` folder, the `012`, `013`, `034`, `047`, `055`, `082` and `084` items, the `051e` scope and diff, all menus and
  skip screens, `MegaLevelScreen` (pause and frame order), `MegamanMaverickGame.render`, `AbstractMenuScreen`, `KeyboardInput`,
  `InputFrameBuilder`, and the `gdx-backend-lwjgl3` `1.14.2` sources (`DefaultLwjgl3Input`, `Lwjgl3Window`, `Lwjgl3Application`).
- Search result: 9 files contain `controllerPoller`, 12 files contain it or `IControllerPoller` or `FrameControllerPoller`, 9 read
  sites in 8 files.
- Draft `docs/SCOPE.md` written, with 11 open questions. Main results:
  - The poller has no state. It reads the frame that `render()` writes once, so the 9 reads move with no change in any frame.
  - `Esc` reaches nothing today. It has no action in menus or gameplay.
  - `ScreenController` is dead code: `screenController` has no assignment.
  - Bug candidate, by reading only: a `START` held at the start of a lock pauses the game with a dead menu (`docs/SCOPE.md`, bug
    candidate 1, question 3).
- Overlaps are in `docs/SCOPE.md`, because this session had leave to write only that file and this log. Copy them to `docs/NOTES.md`
  at branch start.

## 2026-11-01 (contract)

- No branch exists yet. Draft `docs/CONTRACT.md` written on `master`, read at `72be803bd`. The code is the same as at `a41a82ceb`: the commits
  between them change only docs. The game was not launched, and `~/.prefs` was not read. The check counts: 18 automated, 33 named tests, 11 review,
  45 manual, plus `S1` and `X1`.
- The owner changed section 4 and intended change 6 of `docs/SCOPE.md` during this work (`72be803bd`). The contract follows the new text.
- Lock room: `REFACTOR_TEST_051 --room basic`. The spawn is 12 tiles left of the open right edge, with no enemies. `REFACTOR_TEST_029` and
  `REFACTOR_TEST_031` have no transition near a spawn.
- No check needs a known prefs state, so the contract has no backup step. `S1` and `X1` compare file hashes, as a read-only proof.
- Text in `docs/SCOPE.md` to correct is in the contract, open question 5. This session did not change that file.

## 2026-11-01 (contract approved)

- The owner approved `docs/CONTRACT.md` (verbatim: "yes"), with the recommendations of questions 2 to 6. `docs/SCOPE.md` text corrected as question 5 says, with the 2 pause seams in its API table. No answer changed.
- Issue filed on `master`: `docs/issues/2026-11-01-start-release-at-lock-start-pauses.md`.

## 2026-11-01 (plan)

- No branch exists yet. Draft `docs/PLAN.md` written on `master`, read at `5c02dc8aa`. No build, no launch, no commit. `~/.prefs` was not read. The 2
  local edits of the owner were not touched.
- 15 steps: 0 branch start, 1 baseline, 2 and 3 frames, 4 the system frame, 5 to 8 the back action, 9 and 10 the pause seam, `Esc` and the lock tests,
  11 removals, 12 contract checks, 13 the owner's play test, 14 merge. The contract's own text says "plan step 1" for the baseline, so the
  baseline is step 1 and the branch start is step 0.
- All 109 checks have a line in the check map: 18 A, 33 T, 11 R, 45 manual, `S1` and `X1`.
- `SCOPE.md` and `CONTRACT.md` line numbers were checked on the current code. The moved numbers are in the plan table. The contract and scope text that no
  longer fits is plan open question 1. This session changed neither file.
- Read at `084g` (`fd8e5e85a`, and its worktree docs): no shared file with A6, A7 or A8. `LevelRoomHandler.kt` is in the merge rule of step 14.
- A design choice in the plan: `isPauseRequested` takes a `ButtonStatus` for `Esc`, not the system frame. So `MLS` keeps the `Esc` read and the `TODO`,
  and A15, A16 and A18 stay true as written.

## 2026-11-01 (plan approved, branch start)

- The owner approved `docs/PLAN.md` (verbatim: "yes"), with a) on its 3 questions. `docs/CONTRACT.md` and `docs/SCOPE.md` text corrected as question 1 says: A2, A3, A11, R6, a note on the moved line numbers, section 4, the API table, "Measured size" and 2 question numbers. No answer changed.
- The 2 line numbers of the boss-defeat lock in the issue file are now `:754`.
- Plan step 0: branch start on `master`.

## 2026-11-01 (implementation)

- Step 1 (baseline, on `bd7eca889`, clean build): the build passes. `Eb` = 621, `Cb` = 521, 0 failures. 75 unique warning lines with the line numbers removed (95 `^w:` lines). The plan's text says "95 unique warnings" from `docs/mega-refactor/process-log.md`; 95 is the raw line count on this base, and 75 is the unique count. Compare the list, as A4 says. The paths in `build-baseline-warnings.txt` are worktree paths; step 14.9 normalizes them. `docs/NOTES.md` has the overlaps, with `084g` read again at `7439ebaac`. `docs/TODO.md` has the candidates, with contract question 4 and plan question 3.
- Step 2: `MegaPlayers.menuInput`; `MegaMenuScreen` reads it at its 2 lines, and `selectionButtons` goes. New `MegaMenuScreenTest`, and 2 cases in `MegaPlayersTest`. Run 1 (`menuInput` gives `InputFrame.RELEASED`, no poller stub): T17, T27, T28 and T30 failed; T18, T29 and T31 passed, as predicted. Run 2 (the production `FrameControllerPoller` as a stub): T27 to T31 passed on the old reads, and only T17 failed, as predicted. With the code and no stub, all 7 pass. Test change: `MegaPlayers.get` is not an `operator`, so T18 calls `get(...)`. Engine 621 (`Eb`), core 528 (`Cb + 7`), 0 failures, 75 warning lines, no new line. Smoke launch `--screen MAIN_MENU_SCREEN`: pass (`timeout`, no exception).
- Step 3: the 6 skip screens and `ScreenController` read `menuInput`, with the same buttons and edges. `END_BUTTONS`, `START_BUTTONS` and `EARLY_EXIT_BUTTONS` go. No new test, as the plan says. Engine 621, core 528 (`Cb + 7`), 0 failures, 75 warning lines, no new line. The `grep` for `controllerPoller` in `screens` and `controllers` gives no output. Smoke launches `--screen SIMPLE_INIT_GAME_SCREEN` and `--screen CREDITS_SCREEN`: pass (`timeout`, no exception).
- Step 4: `SystemButton`, `KeyboardInput.systemHeld()`, and a private `systemFrameBuilder` in `G`, which builds on the line after the slot-0 build. Before the code (`systemHeld() = 0L`): T11, T13, T14 and T15 failed; T12 and T16 passed, as predicted. The frame has no reader until step 5, as the plan says. Engine 621, core 534 (`Cb + 13`), 0 failures, 75 warning lines, no new line. `grep -c 'ESCAPE' C/input/KeyboardInput.kt` gives `2`. Smoke launch `--level REFACTOR_TEST_029 --room movement`: pass (`timeout`, no exception).
- Step 5: `backRequested()` and `onBack(delta)` on `AbstractMenuScreen`; the confirm test in `render` gets an `else if`. `MegaMenuScreen.backRequested()` reads `game.systemInput`, the first reader of the system frame. Run 1 (the 2 functions, no call in `render`): T1 and T9 failed; T2 to T8 and T10 passed, as predicted. Run 2 (mutation, not committed: the call after the `let`, outside the guard, with no `else`): T3, T4, T5, T6, T7 and T8 failed, as predicted. Engine 631 (`Eb + 10`), core 534 (`Cb + 13`), 0 failures, 75 warning lines, no new line. Smoke launch `--screen MAIN_MENU_SCREEN`: pass (`timeout`, no exception). No behavior change: every `onBack` is empty until step 6.
- Step 6: `onBack` in `SelectDifficultyScreen`, `ExtrasScreen`, `CSS` and `MainMenuScreen`, with `backRequested` and the panel test on `MainMenuScreen`. `SaveGameScreen.kt` does not change. No new test, as the plan says. R1 and R5 by reading the diff: each `onBack` calls the `onSelect` of the row, no `onBack` holds a copy of a row body, and the root returns at once. Engine 631, core 534, 0 failures, 75 warning lines, no new line. Smoke launches `--screen EXTRAS_SCREEN` and `--screen KEYBOARD_SETTINGS_SCREEN`: pass (`timeout`, no exception).
- Step 7: `LPS.backRequested` and `onBack`. R4 by reading the diff: the grid path writes no `currentWeapon`, does not call `setCurrentButtonKey`, and reaches the guard `!exiting && settingsScreenSlide.finished` of `onAnySelection`, which `backRequested` and `isInteractionAllowed` make true. The panel path runs the `GSP` row, and `buttons` holds that row (`LPS:253`). No new test, as the plan says. Engine 631, core 534, 0 failures, 75 warning lines, no new line. Smoke launch `--level REFACTOR_TEST_029 --room movement`: pass (`timeout`, no exception); it does not reach this path.
- Step 8: `LevelSelectScreenV2.onBack` goes to `SAVE_GAME_SCREEN`, with the `TODO` of contract question 4, wrapped to 120 columns. It does not set `selectionMade` and plays no sound. No new test, as the plan says. Engine 631, core 534, 0 failures, 75 warning lines, no new line. Smoke launch `--screen LEVEL_SELECT_SCREEN`: pass (`timeout`, no exception).
- Step 9: new `LPR` with `isPauseRequested`; `MLS.render` calls it with named arguments; `isPauseButtonRequested()` and its 2 imports go. No behavior change. Before the code (a body of `false`): T20 and T23 failed; T25 passed, as predicted. Engine 631, core 537 (`Cb + 16`), 0 failures, 75 warning lines, no new line. Smoke launch `--level REFACTOR_TEST_051 --room basic`: pass (`timeout`, no exception).
- Step 10: `game.inputLocked`; `isPauseRequested` gets `locked` and `backStatus`; new `queuePause`; `MLS` calls both, with the `Esc` `TODO` at the `backStatus` argument. Run 1 (the parameters ignored, `queuePause` with no test): T19, T21, T24 and T32 failed; T22, T26 and T33 passed, as predicted. Run 2 (mutation, not committed: `queuePause` reads the lock when it adds the runnable): T32 failed and T33 passed, as predicted. Engine 631, core 544 (`Cb + 23`), 0 failures, 75 warning lines, no new line. `grep -c 'inputLocked' MLS` gives `2`. A15 and A16 pass as written. A18: the `TODO` wraps after `(031`, so the date is on the second line; the plan asks for the wrap. Smoke launches `--level REFACTOR_TEST_051 --room basic` and `--level REFACTOR_TEST_029 --room player_death`: pass (`timeout`, no exception).
- Difference from the plan text: the third `TODO`, at `queuePause`, says `MegaLevelScreen:753`, not `:754`. On this branch the boss-defeat lock is at `:753`, after the import changes of steps 9 and 10. The rest of the text is as the plan gives it.
- Step 11: deleted `FrameControllerPoller.kt`, `IControllerPoller.kt` (its folder goes with it) and `FrameControllerPollerTest.kt`. `G` loses the 2 imports, the field and the assignment, and the label is `"inputFrames"`. Engine 631, core 541 (`Cb + 20`), 0 failures, 75 warning lines, no new line. A10 and A11 give no output, and `ls` of the `polling` folder fails. Smoke launch `--level REFACTOR_TEST_029 --room movement`: pass (`timeout`, no exception).
- Step 12 (contract checks, on `230844f6e`, clean build, base `bd7eca889`):
  - A1: pass. A2: engine 631 (`Eb + 10`), 0 failures: pass. A3: core 541 (`Cb + 20`, the minimum), 0 failures: pass. A4: 75 unique warning lines, `comm` against `build-baseline-warnings.txt` gives no new line, and no line left the list: pass. A5: `utils/diagnostics/run_tests.sh`, 35 passed: pass.
  - A6: 2 lines (`D` `IControllerPoller.kt`, `M` `AbstractMenuScreen.kt`): pass. A7: 21 lines: the 20 paths of the contract and `C/screens/levels/LevelPauseRules.kt`, the one allowed addition: pass. A8: `MenuScreenTest.kt`, `KeyboardInputTest.kt` and `MegaPlayersTest.kt` changed, `FrameControllerPollerTest.kt` deleted, `MegaMenuScreenTest.kt` and `LevelPauseRulesTest.kt` new: pass. A9: no output: pass.
  - A10: no output, and `ls` of `polling` fails: pass. A11: both `grep`s give no output: pass. A12: against `master`, 1 new line, the `Esc` read; `ESCAPE` is on 2 lines, both in `C/input/KeyboardInput.kt`: pass. A13: `1`, `1`, `1`; `inputLocked` is on 2 lines of `MLS` (`:873`, `:882`): pass. A14: only `inputLocked` (`:227`) and `slot0FrameBuilder.locked` (`:681`, `:690`): pass. A15, A16: the listed files, and `LPR` is in neither list: pass. A17: 9 files: pass. A18: 1 new `TODO` line in `MLS`, at the `Esc` read; the date is on its second line: pass.
  - T1 to T33: all 33 names are in the test results, once each, and all pass. Names:
    - `MenuScreenTest`: T1 "should run onBack once, with the delta of the frame, and nothing else"; T2 "should not run onBack with no back request"; T3 "should not run onBack when a selection is made"; T4 "should not run onBack when interaction is not allowed"; T5 "should not run onBack when the current key is null"; T6 "should not run onBack when the current key has no button"; T7 "should run the confirm and not onBack in one frame when onSelect gives true"; T8 "should run the confirm and not onBack in one frame when onSelect gives false"; T9 "should run a navigation first, then onBack, in one frame"; T10 "should have no back request in a subclass with no override, and act as before".
    - `KeyboardInputTest`: T11 "gives the bit of BACK for Esc"; T12 "gives 0 for each other key, also the Android BACK key and Enter"; T13 "keeps Esc and the key of A apart"; T14 "gives 1 press edge for a held Esc and 1 release edge, through a real builder"; T15 "gives a press of Esc in the system frame while a lock releases the slot frame"; T16 "has 1 entry, BACK, and passes the button check".
    - `MegaPlayersTest`: T17 "gives the frame of slot 0, not of slot 1"; T18 "is RELEASED before any write, also with no registered player".
    - `LevelPauseRulesTest`: T19 "is false for the START release that the first locked build makes"; T20 "is true for a real START release with no lock"; T21 "is true for a press of Esc with no lock, and false while Esc stays held"; T22 "is false for a press of Esc with the lock on"; T23 "is true for a press of SELECT only with the START action, and false for a release of SELECT"; T24 "is false for SELECT with the START action and the lock on"; T25 "is false when paused, not alive or not in play"; T26 "is false for a START held across the end of a lock, and true for its real release"; T32 "drops the pause when a lock starts after the request, in the order of render()"; T33 "runs the pause once when no lock starts".
    - `MegaMenuScreenTest`: T27 "gives the direction of each d-pad button that is just released"; T28 "gives the first of UP, DOWN, LEFT and RIGHT for 2 releases in one frame"; T29 "gives null for a d-pad button that is just pressed or pressed"; T30 "is true for a release of START and for a release of A"; T31 "is false for a release of B or SELECT and for a press of START".
  - R1 to R11 (read `git diff master...HEAD -- core/src/main engine/src/main`): all pass. R1: the 5 rows go through `onSelect`, then `selectionMade` and `onAnySelection`; no `onBack` holds a row body. R2: `MegaMenuScreen.backRequested()` reads only the system frame; `MainMenuScreen` and `LPS` add their slide test. R3: the 9 reads keep their buttons and edges. R4, R5: pass. R6: `isPauseRequested` keeps the order and adds `!locked`, with `Esc` between the 2 old tests; `queuePause` reads the lock in the runnable. R7: 1 getter, no setter. R8: the system build is on the line after the slot-0 build. R9: each new API has the production caller of the plan table, and no other public API is added. R10: `capacity` stays `1`; `PlayerSlot(0)` in a menu read is only in `menuInput`. R11: `WilyFinalBoss.kt:311` submits `TURN_CONTROLLER_OFF`, so the same guard acts on it.
  - Code comments: 3 new `TODO`s, at `queuePause`, at the `Esc` read in `MLS` and at `LevelSelectScreenV2.onBack`. 2 old comments went with the deleted files.
  - Overlap: `master` moved during this work to `1fa0be319`, with `084g` merged. Result in `docs/NOTES.md`. `LevelRoomHandler.kt` is on the merge rule, so step 14.2 forces a merge of `master`. This session did not merge. L1 to L6 depend on the room transition, so the play test covers the combined code only if the merge comes first.
  - The S, E, L, P, M, C, D, V, K, G, W and X checks are not run: they are the owner's.

## 2026-11-01 (merge of `master` before the play test)

- `master` moved to `1fa0be319` (`084g`), in 1 path of the step 14.2 list: `LevelRoomHandler.kt`, at the end of a room transition. L1 to L6 test the transition, so `master` went into the branch before step 13 (`d0ba57b9b`), with no conflict, as `031e` did. The play test then covers the combined code.
- Clean build exit 0. Engine 631 (`Eb + 10`). Core 590: 570 on `master` plus 20. 0 failures. 75 unique warning lines, no new line against `build-baseline-warnings.txt`.

## 2026-11-01 (plan step 13: partial play test, merge approval)

- The owner did not run all 45 manual checks. The owner's result (verbatim): "Admittedly, I did not run each and every test just because it's so many of them. I tested some of the keyboard-only tests and found no issues. I want to approve the changes".
- Result: no problem found in the checks that the owner ran. The owner did not name which checks, so this log gives no pass for any single check.
- Not run, because the owner tested only keyboard-only checks: the `[pad]` checks L4, C3, C4, C5, V2, V3 and G2.
- Not known: which of the other 38 manual checks ran, and if S1 and X1 ran. So these items have no recorded live proof: the soft-lock fix in the full game (L1, L2), `Esc` out of a pause under a lock (L6), the row logic of M1 to M5 and C1 to C5, which has no unit test, and the prefs compare (S1, X1).
- The owner accepts this gap. Merge approval for this branch, on the combined code at `add2c9838` (verbatim, as above): "I want to approve the changes".
