# Process log: mega-refactor-031d-devices

## Status

- State: merged
- Goal: devices: the keyboard aliases, every connected gamepad (with the index-shift and code-0 fixes), the stick at `0.5`, the triggers
- Done: steps 1, 2 (build half), 3 to 8
- Next: none. Merged.
- Blocked: the `master` half of P1 (captures 1 and 2) is pending with the owner

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- No branch exists yet. The folder was prepared on `master`. The goal is from the `031` decisions of 2026-10-25 and
  2026-10-26 (item `ITEM.md`).
- Scan on `05fc5ed12`: the root and `docs/mega-refactor/` `CLAUDE.md`, the item `ITEM.md` and `NOTES.md`, the `031c`
  folder (`SCOPE.md`, `CONTRACT.md`, `process-log.md`), the 6 input issues, `C/input/`, `C/controllers/`, `E/input/`,
  `E/controller/`, `MegamanMaverickGame` (create, render, onEvent), `ControllerSettingsScreen`, `GameSettingsPanel`,
  the input tests, and the `gdx-controllers` `2.2.3` and `jamepad` `2.0.20.0` sources. Draft `docs/SCOPE.md` written.
- The owner approved `docs/SCOPE.md` (question 1: "I agree"; questions 2 to 6: "approved"; all as recommended).
  Answers in `docs/SCOPE.md` and the item `ITEM.md` (decision of 2026-10-30).
- Draft `docs/CONTRACT.md` written. Test rooms used (all checked in the live TMX `game_rooms` layers): `REFACTOR_TEST_029`
  `movement` and `player_death` (`RefactorTest029.tmx`), `INTRO_STAGE` `room1` and `room12` (`IntroStage_v3.tmx`;
  `room12` holds the boss `Gate` and a player spawn). No new test room.
- The owner approved `docs/CONTRACT.md` with both recommendations (verbatim: "yes"). The overlap note in
  `docs/NOTES.md` now names `mega-refactor-051a-camera-read-interface` (no overlap known).
- Draft `docs/PLAN.md` written (10 steps: branch start, baseline with the `master` half of P1, code-0 fix,
  keyboard aliases, every gamepad with the index-shift fix and the removal of the index-0 overload, left stick,
  triggers with the capture listener, contract checks, the owner's play test with the branch half of P1, merge).
- The owner approved `docs/PLAN.md` with the 3 recommendations (verbatim: "approved"), and asked for a comment at the
  test subclass of `ControllerMapping` (`docs/NOTES.md`, decisions). `docs/CONTRACT.md` P1: "up to 4 native calls"
  corrected to 2 (a reading note, not a check). Trigger label added to `docs/TODO.md` as a backlog candidate.
- Step 1 (branch start): `ITEMS.md` row and `ITEM.md` set to active; worktree
  `.worktrees/mega-refactor-031d-devices` created from `master`.

## 2026-10-30 (implementation, steps 2 to 8)

- Step 2 (baseline), in the worktree on `2307c5414`, with the build command: the build passes. Engine 575 tests, core
  272 tests, 0 failures, 81 unique warning lines. These are the same as the contract baseline on `742abf63f`, so
  A2 to A4 use the contract numbers. The sorted warning list is in `build-baseline-warnings.txt` next to `build.log`
  in the worktree (both files are not committed).
- Step 2: the `master` half of P1 (captures 1 and 2) is pending with the owner.
- Step 3 (code-0 fix): tests first. New file `ControllerUtilsExtensionsTest.kt` (T21, T22) and one new case in
  `GamepadInputTest` (T23). Before the code: T21 and T23 failed (code `0`), T22 passed, as the plan says. After the
  code (`getPreferredControllerCode` returns `null` when `contains` is `false`): core 275 (272 + 3), 0 failures; the
  3 prefs cases of `GamepadInputTest` pass; the warning list is the same as the baseline (81 lines).
- Note: Kotest here does not accept the Gradle `--tests` filter ("No tests found"), so each test-first run is a full
  `core:test` run.
- Step 4 (keyboard aliases): tests first, 7 new cases in `KeyboardInputTest` (T1 to T7). Before the code: T2, T3, T4
  and T7 failed; T1, T5, T6 passed. Plan change: the plan said T7 passes before the code. It fails at tick 3, because
  only the up arrow is down and the old code gives no `UP` for it. The test follows the ticks of the plan, so only the
  prediction was wrong. After the code (a private `IntArray`/`LongArray` alias table in the companion, one `TODO` that
  names the edge case and `031e`): core 282 (275 + 7), 0 failures; the warning list is the same as the baseline.
  `grep -n "TODO"` on `KeyboardInput.kt` shows one line (line 11).
- Step 4 smoke launch: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`, stopped by `timeout 25`, no
  exception in the console or in `assets/logs.txt`. Pass. The first launch with `timeout 20` ended while Gradle still
  compiled, so the game did not start; later launches compile first (`./gradlew lwjgl3:classes`).
- Step 5 (every connected gamepad, index-shift fix): the seam `ControllerUtils.getControllers()` first. Tests first:
  the 8 old cases and T23 moved to the seam (a gdx `Array` of pad mocks; a held button is `getButton(code)` on the
  pad mock; the `isControllerKeyPressed(any<Int>())` stub in `beforeEach` removed), plus 6 new cases (T8 to T13).
  Before the code: 12 cases failed (the 5 moved cases that need a connect, the moved "sets the bit" case, T23 and
  T8 to T13). 3 moved cases passed, because they expect unchanged codes or no bits ("does not re-apply the mapping",
  "gives no bits while disconnected", "leaves 'controllerCodes' untouched"). Plan change: the plan did not name T13
  as failing before the code; it fails because the old code never connects through the seam.
- Step 5 code: `GamepadInput` tracks `primary: Controller?` by identity at the start of `held()` (before the read,
  R4), and reads each pad in an index loop. The index-0 overload `isControllerKeyPressed(key: Int)` is removed.
  After the code: core 288 (282 + 6), 0 failures; the warning list is the same as the baseline. A8 `grep` gives no
  output. A9 `grep` gives only the definition of the two-argument overload (`ControllerUtils.kt:12`), no call.
- Step 5 smoke launch, as in step 4: stopped by `timeout 25`, the map loaded, no exception. Pass. No gamepad was
  connected to this machine during the agent's launch, so the run with the PS3 pad is part of the owner's step 9.
- Step 6 (left stick): the pad mocks now use a test subclass of `ControllerMapping` (an anonymous `object` built
  through the `protected` constructor, `axisLeftX = 0`, `axisLeftY = 1`, the button fields from
  `defaultMappingByButton`), with the comment that the owner asked for. So the real `getMapping` gives the test
  codes, and the per-mapping `getMapping` stubs and their import are gone. Tests first, 5 new cases (T14 to T17,
  T20). Before the code: T14, T15, T16 and T20 failed. T17 passed, because the bound d-pad `RIGHT` alone already
  gives the `RIGHT` bit; it guards the "one bit" rule after the code.
- Step 6 code: in the pad loop, `pad.mapping` is read once, then the two stick axes against the private
  `STICK_THRESHOLD = 0.5f`. After the code: core 293 (288 + 5), 0 failures; the warning list is the same as the
  baseline. Smoke launch as in step 5: stopped by `timeout 25`, the map loaded, no exception. Pass.
- Step 7 (triggers): the public constants in the `GamepadInput` companion came first, so that the tests compile.
  Tests first, 2 new cases (T18, T19). Before the read code: T18 and T19 failed, as the plan says.
- Step 7 code, read: in the button loop, a trigger code reads `getAxis(<trigger axis>) >= TRIGGER_THRESHOLD`, every
  other code reads `getButton(code)`. Code, capture: in the `buttonListener` object of `ControllerSettingsScreen`,
  `axisMoved` maps the 2 trigger axes to the 2 trigger codes and returns `false` at once for any other axis or a
  value below `0.5`; `buttonDown` and `axisMoved` both call one private `setControllerCode(controller, code)` of the
  listener object (the old body of `buttonDown`, with `buttonIndex` renamed to `code`).
- Plan change: the plan check says that the screen diff is only inside the `buttonListener` object. The diff also
  has one import line (`import com.megaman.maverick.game.input.GamepadInput`), which the constants need. This is the
  smallest change that keeps the intent of R3.
- Step 7 check: core 295 (293 + 2), 0 failures; the warning list is the same as the baseline. Smoke launch as in
  step 5: stopped by `timeout 25`, the map loaded, no exception. Pass.
- Plan change: the agent cannot drive the pause menu without key presses on the owner's desktop. In place of "open
  the gamepad settings from the pause menu and back out", it ran `./run-game-alpha.sh --screen
  CONTROLLER_SETTINGS_SCREEN`: stopped by `timeout 25`, no exception. The log has one
  `ERROR | ControllerSettingsScreen | No controller found` line; it comes from old code
  (`ControllerSettingsScreen.kt:381` on `2307c5414`) when no pad is connected. Pass. The menu path stays in the
  owner's step 9 (G7).
- Step 8 (contract checks), on the branch at `f1e597f03`, clean build with the build command:
  - A1: pass (the build passes).
  - A2: pass (engine 575, 0 failures).
  - A3: pass (core 295 = 272 + 23, 0 failures; the 3 old `KeyboardInputTest` cases and the 8 old `GamepadInputTest`
    cases are there, the 8 on the pad-list seam).
  - A4: pass (81 unique warning lines; `diff` of the sorted list against `build-baseline-warnings.txt` gives no
    output).
  - Note for A5 to A7 and R1 to R3: `master` moved to `1eaef3415` after the branch start (see `docs/NOTES.md`). The
    commands as written (`git diff master`) also show the reverse of `master`'s new change: A5 shows
    `MegamanMaverickGame.kt` (8 lines), A7 lists 7 files (also `MegamanMaverickGame.kt` and `DesktopLauncher.kt`).
    These lines are not branch changes. The checks below compare with the merge base `2307c5414` (the branch's own
    changes, the same as `git diff master...HEAD`). After the merge of `master` into the branch at step 10.3, the
    commands as written give the expected output.
  - A5: pass (no output against `2307c5414`).
  - A6: pass (no output, also against `master`).
  - A7: pass (against `2307c5414`: exactly the 5 files).
  - A8: pass (no output).
  - A9: pass (only the definition of the two-argument overload, `ControllerUtils.kt:12`; no call with one argument).
  - A10: pass (one `TODO`, `KeyboardInput.kt:11`, names the edge case and `031e`).
  - A11: pass (`ScreenController.kt` has no diff; its one `TODO` was there on `2307c5414`).
  - T1 to T23: pass. Each case is in `core/build/test-results/test/` and passed. Names: T1 "gives START for Enter";
    T2 "gives START for numpad Enter"; T3 "gives START for Enter and for the new key when START is bound to another
    key"; T4 "gives the d-pad button for each arrow key"; T5 "gives one UP bit for W and the up arrow together"; T6
    "gives no bit for Esc"; T7 "keeps UP pressed with no second press when the up arrow joins and then replaces W";
    T8 "sets the bit of a bound button that is down on the second pad"; T9 "ORs the buttons of both pads into one
    mask, with one bit for a button down on both"; T10 "reloads the codes from the prefs of the remaining pad when
    the first pad goes, with 'loadPrefsOnConnect'"; T11 "reloads the codes from the mapping when the first pad goes,
    without 'loadPrefsOnConnect'"; T12 "clears the codes when the list becomes empty and loads them again for a new
    pad object"; T13 "keeps the codes while the primary pad is the same object over many ticks"; T14 "gives LEFT and
    RIGHT on the X axis from 0.5"; T15 "gives UP and DOWN on the Y axis from 0.5"; T16 "gives RIGHT and UP for a
    diagonal"; T17 "gives one RIGHT bit for the stick and the bound d-pad RIGHT together"; T18 "holds a button bound
    to a trigger code when that trigger axis is at 0.5, not at 0.49"; T19 "never gives a trigger code to
    'getButton'"; T20 "gives the stick bits for a pad with no bound codes"; T21 "gives null when the prefs file has
    no key for the button"; T22 "gives the saved code of the button, also the code 0"; T23 "loads the mapping, not
    code 0, when 'loadPrefsOnConnect' is true and the prefs file has no keys".
  - R1: pass. Of the 13 index-0 call sites, only `ControllerUtils.kt:11` (removed) and the 4 in `GamepadInput`
    changed. The `getController()` calls in `ControllerUtilsExtensions` (4), `ControllerSettingsScreen` (the
    `controller` getter) and `GameSettingsPanel` have no diff.
  - R2: pass. In `ControllerUtilsExtensions.kt` the only diff is one line in `getPreferredControllerCode`.
  - R3: pass, with the one import line of step 7. The other changes are in the `buttonListener` object (`axisMoved`
    next to `buttonDown`, and the shared private `setControllerCode`).
  - R4: pass. In `held()`, the primary check runs before the read loop, in the same call.
  - Overlap: none with `051a` or `084a`; `master`'s `1eaef3415` shares 2 files (no conflict in `git merge-tree`).
    Details in `docs/NOTES.md`.
  - Code comments: the only new comment in `core/src/main` is the `TODO` of A10. In the tests, the new comment is
    the one at the `ControllerMapping` subclass (owner's request); the other test comments are old lines in the moved
    cases.
- Not done by the agent: P1 (captures 1 to 5) and the manual checks K1 to K7, G1 to G10, D1 to D4. They are the
  owner's work (step 2 `master` half and step 9). The step 9 command list is in `docs/PLAN.md`.
- Merged `master` (`1eaef3415`, the `--loadKeyboardPrefs` flag, which changes `ControllerUtilsExtensions.kt` and
  `ControllerSettingsScreen.kt`) into the branch before the play test, with no conflict, so that the play test covers
  the combined code. Clean build passes; 575 engine and 295 core tests, 0 failures; the same 81 unique warnings.
- Play test, `D3` failed: the game crashed with `IndexOutOfBoundsException: index can't be >= size: 1 >= 1` at
  `GamepadInput.kt:44`. Cause: `held()` looped over the live `Controllers.getControllers()` list, and the size of the
  loop range was fixed at the start. A pad that was unplugged left the list during a read in the same loop. New test
  first: "reads every pad of the tick when a pad leaves the list during the read" failed with the same exception and
  line. Fix: `held()` copies the list into a reused array (`padsOfTick`) once per tick and loops over the copy. After
  the fix: clean build passes; 575 engine and 296 core tests (272 + 24), 0 failures; the same 81 unique warnings. The
  contract's core count is "272 plus the new tests", so it still holds.
- Play test, `G6`: the owner rebound 3 buttons, without "LOAD SAVED SETTINGS" before the unplug, because the play-test
  list that the owner received left that step out (`docs/CONTRACT.md` has it). After a replug the codes went back to
  the mapping. "LOAD SAVED SETTINGS" then restored them. This is the same as `master`: `loadPrefsOnConnect` is `false`
  until that button sets it (`ControllerSettingsScreen.kt:221`). Not a bug of this branch.
- Play test: the owner ran K1 to K7, G1 to G10 and D1 to D4, with the PS3 pad and a NES-style pad as pad B, on the
  fixed code; G6, G8, D2 and D3 ran again after the fix. Owner (verbatim): "all tests pass".
- P1: not run. The owner saw no stutters or FPS drops and wrote "I'll skip these for now". Captures 1 to 5 are open;
  the owner decides at the merge review.
- Merge review. P1: the owner skips all captures (verbatim: "skip the captures"), so P1 was not done and has no
  measurement. Merge approval (verbatim: "approved").
- `master` changed `MegamanMaverickGame.kt` (`051a`), which plan step 10.3 lists, so `master` was merged into the branch
  before the squash, with no conflict. Clean build passes; 575 engine and 298 core tests, 0 failures (296 here plus 2
  from `master`); the same 81 unique warnings. A5, A6 and A7 pass against `master`: only the 5 expected production
  files differ.
