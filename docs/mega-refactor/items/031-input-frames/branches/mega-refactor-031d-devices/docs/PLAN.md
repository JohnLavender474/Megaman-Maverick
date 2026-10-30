# Plan: mega-refactor-031d-devices

How to do the work in `SCOPE.md`. This was written ahead of time on `master` (`a5deca7ce`), before the branch existed.
It follows the approved `docs/SCOPE.md` and `docs/CONTRACT.md` (both 2026-10-30).

- Status: approved
- Approved: 2026-10-30

## Rules for every step

- Build and test command, from the worktree, on a clean build:
  `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar 2>&1 | tee build.log`.
  Read the test counts from `engine/build/test-results/test/` and `core/build/test-results/test/`. Count the warnings
  as in contract check A4.
- Each code step is one commit. After each code step, the build passes and all tests pass. Each commit message ends
  with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. The same commit adds a dated entry to the branch
  `process-log.md` and updates its status block.
- One test case per `T` ID of the contract, so the core count grows by the number of `T` IDs in the step.
- Test-driven order where it fits: write the new tests first, run them and see the expected ones fail, then write the
  code. Write in the process log which tests failed before the code.
- No code comments. There are two exceptions: the `TODO` at the keyboard alias table (step 4), which names an accepted
  edge case and `031e`, and the comment at the test subclass of `ControllerMapping` (step 6), which the owner asked
  for.
- Write code only when production code calls it. A test alone is not a caller. Each new API below has its first
  production caller in the same step.
- Smoke launches are short (about 15 to 20 seconds). The owner closes the window. A clean exit is a pass; do not
  investigate a clean exit.
- Never run a bare `git stash`. The main checkout can hold uncommitted owner edits (for example `Met.kt`). In the main
  checkout, stage only named paths (`git add <path> ...`), never `git add -A` or `git add .`.
- On the branch, write docs only inside the branch folder.
- The engine and `MegamanMaverickGame.kt` do not change (contract A5). Only the 5 files in contract A7 change in
  `core/src/main`.

`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`.

New APIs and their first production callers in this branch:

| API | Step | First production callers |
|---|---|---|
| Alias table in `KeyboardInput` (private companion arrays) | 4 | `KeyboardInput.held()` |
| `ControllerUtils.getControllers(): Array<Controller>` (the pad-list seam; returns `Controllers.getControllers()`) | 5 | `GamepadInput.held()` |
| `private var primary: Controller?` in `GamepadInput` (replaces `connected`) | 5 | `GamepadInput.held()` |
| `STICK_THRESHOLD = 0.5f` (private in the `GamepadInput` companion) | 6 | `GamepadInput.held()` |
| `TRIGGER_LEFT_AXIS = 4`, `TRIGGER_RIGHT_AXIS = 5`, `TRIGGER_LEFT_CODE`, `TRIGGER_RIGHT_CODE`, `TRIGGER_THRESHOLD = 0.5f` (public in the `GamepadInput` companion) | 7 | `GamepadInput.held()`; the capture listener in `ControllerSettingsScreen` |

Removed: `ControllerUtils.isControllerKeyPressed(key: Int)` (the index-0 overload, `C/controllers/ControllerUtils.kt:11`),
in step 5, the step that removes its only caller (`C/input/GamepadInput.kt:41`).

## Steps

### 1. Branch start

- Changes: in the main checkout, on `master`, one docs-only commit (stage only these paths):
  - this `docs/PLAN.md`: `- Status: approved` and `- Approved: <date>`;
  - `docs/mega-refactor/ITEMS.md`: the `mega-refactor-031d-devices` row to `active`, with the start date;
  - `docs/mega-refactor/items/031-input-frames/ITEM.md`: in the "Branches" line, `mega-refactor-031d-devices`
    (planned) becomes (active);
  - the branch `process-log.md`: state `active`, a dated entry for the start.

  Item `031` merges each branch directly into `master` (squash), as `031a` to `031c` did. Then, from the main
  checkout:
  `git worktree add -b mega-refactor-031d-devices .worktrees/mega-refactor-031d-devices master`.
- Check: `git worktree list` shows `.worktrees/mega-refactor-031d-devices` on `mega-refactor-031d-devices`, next to
  the main checkout and `.worktrees/mega-refactor-051a-camera-read-interface`. `git status` in the main checkout shows
  no staged change left.
- Contract checks: none.
- Done: yes

### 2. Baseline

- Changes: none. In the worktree, run the build command.
- Check:
  - The build passes. Write in the process log: the engine and core test counts and failures, and the number of
    unique warning lines. Keep the sorted warning list (`grep '^w:' build.log | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u`)
    in the process log or next to `build.log`, for A4 in step 8. The contract baseline on `742abf63f` is engine 575,
    core 272, 81 unique warnings. `master` changed one code file since then (`b10065119`, one import removed from
    `Met.kt`); if a count differs, write the new baseline and the reason in the process log, and use it for A2 to A4.
  - The `master` half of P1. The owner makes captures 1 and 2 on `master` (stand, run, jump, open and close the pause
    menu once; quit the game cleanly so that the file is flushed). From
    `cd /home/johnlavender/IdeaProjects/Megaman-Maverick`:
    - `./run-game-debug.sh --level REFACTOR_TEST_029 --room movement`: capture 1, no gamepad.
    - `./run-game-debug.sh --level REFACTOR_TEST_029 --room movement`: capture 2, one gamepad (the PS3 pad).

    Run `utils/diagnostics/run.sh <capture file>` on each `assets/<epoch-millis>-diagnostics.txt` file. Write the
    `controllerPoller` row of "Process Summary" in `summary.txt` (mean, p50, p95, p99, `avg%`) and the
    `=== Dropped N frames ===` count of each capture in the process log.
- Contract checks: baseline for A1 to A4 and P1.
- Done: no

### 3. Code-0 fix

- Changes:
  - Tests first. New file `T/controllers/ControllerUtilsExtensionsTest.kt` (Kotest, mocks of `Gdx.app`,
    `Preferences` and `Controller`):
    - T21: a prefs file without the button's key gives `null`.
    - T22: a saved key gives its value, also the value `0`.
  - In `T/input/GamepadInputTest.kt`, with today's mocks (`ControllerUtils.getController()` and
    `isControllerConnected()`):
    - T23: `loadPrefsOnConnect` true, the real `getPreferredControllerCode` (no stub), and a mock `Gdx.app` whose
      prefs file has no keys (`contains` gives `false`, `getInteger` gives `0`): the codes come from the mapping, not
      `0`.
  - Run the tests: T21 and T23 fail (code `0`); T22 passes.
  - Code: in `C/controllers/ControllerUtilsExtensions.kt`, `getPreferredControllerCode` returns `null` when
    `controllerPreferences.contains(button.name)` is `false`, else `getInteger(button.name)`. No other function in the
    file changes (R2).
- Check: the build command passes. Core count = baseline + 3. The 3 existing `GamepadInputTest` cases about prefs
  still pass.
- Commit: `031d: getPreferredControllerCode gives null for a missing key (code-0 fix)`
- Contract checks: T21, T22, T23, R2; intended change 6.
- Done: no

### 4. Keyboard aliases

- Changes:
  - Tests first, in `T/input/KeyboardInputTest.kt` (keep the 3 cases):
    - T1: `Enter` gives `START`.
    - T2: numpad `Enter` gives `START`.
    - T3: `START` rebound to `Input.Keys.I`: `Enter` gives `START`, and `I` gives `START`.
    - T4: each of `Keys.UP`, `Keys.DOWN`, `Keys.LEFT`, `Keys.RIGHT` gives `UP`, `DOWN`, `LEFT`, `RIGHT`.
    - T5: `W` and `Keys.UP` together give one `UP` bit (the mask equals `1L shl UP.index`).
    - T6: `Keys.ESCAPE` gives no bit.
    - T7: a `KeyboardInput` feeds a real engine `InputFrameBuilder(MegaControllerButton.entries)`. Tick 1: `W` down:
      `UP` is `JUST_PRESSED`. Tick 2: `W` and `Keys.UP` down: `UP` is `PRESSED`. Tick 3: only `Keys.UP` down: `UP`
      is `PRESSED`.
  - Run the tests: T2, T3 and T4 fail. T1, T5, T6 and T7 already pass with the default bindings; they guard the code.
  - Code: in `C/input/KeyboardInput.kt`, a private fixed table in the companion: one `IntArray` of keys (`ENTER`,
    `NUMPAD_ENTER`, `UP`, `DOWN`, `LEFT`, `RIGHT`) and one parallel `LongArray` of bits (`START`, `START`, `UP`,
    `DOWN`, `LEFT`, `RIGHT`). After the loop over the bound keys, an index loop ORs the bit of each alias key that is
    down. No allocation per tick. The table is not in `ControllerButtons`, so the bindings screen does not show or
    change it. `Esc` is not in the table. At the table, one `TODO`, for example:
    `// TODO: a key that is bound to a button and is also an alias gives both buttons (for example a button bound to the up arrow also gives UP); 031e refuses the reserved keys on the bindings screen`
- Check: the build command passes. Core count = step 3 + 7. `grep -n "TODO" C/input/KeyboardInput.kt` shows the one
  `TODO`. Smoke launch: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement` from the worktree.
- Commit: `031d: keyboard aliases: Enter and numpad Enter give START, the arrow keys give the d-pad`
- Contract checks: T1 to T7, A10; intended changes 1, 2, 3.
- Done: no

### 5. Every connected gamepad and the index-shift fix

- Changes:
  - Seam: add `fun getControllers(): Array<Controller> = Controllers.getControllers()` to `C/controllers/ControllerUtils.kt`
    (`com.badlogic.gdx.utils.Array`; the library returns its own list, so no allocation).
  - Tests first, in `T/input/GamepadInputTest.kt`:
    - Move the 8 existing cases and T23 to the seam, with the same meaning: `every { ControllerUtils.getControllers() }`
      returns a gdx `Array` with the mock pad, or an empty `Array` for "disconnected". A held button is
      `every { controller.getButton(code) } returns true` on the pad mock. Remove the stub of
      `ControllerUtils.isControllerKeyPressed(any<Int>())` in `beforeEach` (the overload goes in this step). The
      case "when controller stays disconnected leaves 'controllerCodes' untouched" stays: an empty list with no
      primary before does not touch the codes.
    - T8: list `[A, B]`: a bound button down on B sets its bit.
    - T9: list `[A, B]`: a button on A and another button on B give both bits; the same button down on both gives
      one bit.
    - T10: `loadPrefsOnConnect` true; list `[A, B]`, then `[B]`: the codes reload from B's prefs (stub
      `getPreferredControllerCode(B, button)`).
    - T11: `loadPrefsOnConnect` false; list `[A, B]` with A's codes changed after connect, then `[B]`: the codes
      reload from the mapping.
    - T12: list `[A]`, then `[]`, then `[A']` (a new mock object, same name): after `[]` every code is `null`; after
      `[A']` the codes are loaded again.
    - T13: the same primary object over 10 ticks, with a code changed between ticks: no reload.
  - Run the tests: the moved cases and T8 to T12 fail (the adapter does not read the seam yet).
  - Code: in `C/input/GamepadInput.kt`:
    - `private var primary: Controller? = null` replaces `connected`.
    - At the start of `held()`, before the read (R4): `val pads = ControllerUtils.getControllers()`;
      `val first = if (pads.size > 0) pads[0] else null`. If `first !== primary` (identity): if `first != null`, run
      today's connect logic for `first` (its prefs if `loadPrefsOnConnect`, else or on `null` the mapping); if
      `first == null` (the list became empty), set every `controllerCode` to `null`. Then `primary = first`.
    - The read: an index loop over `pads`, and inside it the loop over the bound buttons: a button with a
      non-`null` code sets its bit if `pad.getButton(code)` is `true`. One read per pad inside the loop, so `047`
      can split the pads later; no API for it.
  - Remove `ControllerUtils.isControllerKeyPressed(key: Int)` (`C/controllers/ControllerUtils.kt:11`). The other
    functions in `ControllerUtils` stay.
- Check: the build command passes. Core count = step 4 + 6. `grep -rn "fun isControllerKeyPressed(key: Int)" core/src/main`
  gives no output. `grep -rn "isControllerKeyPressed(" core/src/main` gives no call with one argument. Smoke launch
  as in step 4, with the PS3 pad connected.
- Commit: `031d: gamepad adapter reads every connected pad; tracks the primary pad by object (index-shift fix)`
- Contract checks: T8 to T13, A3 (moved cases), A8, A9, R1, R4; intended changes 4, 5.
- Done: no

### 6. Left stick

- Changes:
  - Test support: `ControllerMapping.axisLeftX` and `axisLeftY` are public final Java fields, not getters, so MockK
    cannot stub them; a relaxed mock gives `0` for both. In `GamepadInputTest`, build the pad mapping from a small
    test subclass of `ControllerMapping` (its constructor is `protected`), with `axisLeftX = 0`, `axisLeftY = 1`, and
    the button fields set to the values of `defaultMappingByButton`. Pad mocks give `getAxis(any())` = `0f` unless a
    test sets it. The subclass has a short comment that says why it is needed: the axis fields are public final Java
    fields, so MockK cannot stub them (owner, 2026-10-30).
  - Tests first:
    - T14: X `0.49f` gives no bit, `0.5f` gives `RIGHT`, `-0.49f` gives no bit, `-0.5f` gives `LEFT`.
    - T15: Y `-0.5f` gives `UP`, `0.5f` gives `DOWN`, `0.49f` and `-0.49f` give no bit.
    - T16: X `0.5f` and Y `-0.5f` give `RIGHT` and `UP`.
    - T17: stick X `0.5f` and the bound d-pad `RIGHT` down give one `RIGHT` bit.
    - T20: a pad with all codes `null` (for example after `buttons` were cleared and the pad is the same primary)
      still gives the stick bits.
  - Run the tests: T14 to T17 and T20 fail where they expect a stick bit.
  - Code: in the pad loop of `GamepadInput.held()`, read `pad.mapping` once; `x = pad.getAxis(mapping.axisLeftX)`:
    `x <= -STICK_THRESHOLD` gives `LEFT`, `x >= STICK_THRESHOLD` gives `RIGHT`; `y = pad.getAxis(mapping.axisLeftY)`:
    `y <= -STICK_THRESHOLD` gives `UP`, `y >= STICK_THRESHOLD` gives `DOWN`. Each axis alone. The stick read does not
    depend on the bound codes. No binding, no revert timer. `STICK_THRESHOLD = 0.5f` is private in the companion.
- Check: the build command passes. Core count = step 5 + 5. Smoke launch as in step 5.
- Commit: `031d: left stick gives the d-pad at 0.5 per axis`
- Contract checks: T14 to T17, T20; intended change 7.
- Done: no

### 7. Triggers and their capture

- Changes:
  - Constants, public in the `GamepadInput` companion: `TRIGGER_LEFT_AXIS = 4`, `TRIGGER_RIGHT_AXIS = 5`
    (`ControllerAxis.TRIGGERLEFT` and `TRIGGERRIGHT` in Jamepad), `TRIGGER_THRESHOLD = 0.5f` (scope question 6),
    and `TRIGGER_LEFT_CODE = 1004`, `TRIGGER_RIGHT_CODE = 1005`. The codes are far outside the button codes of every
    backend (Jamepad gives `0` to `20`), positive, and not `ControllerMapping.UNDEFINED` (`-1`).
  - Tests first:
    - T18: a button bound to `TRIGGER_LEFT_CODE` is held at left trigger axis `0.5f` and not at `0.49f`; the same
      for `TRIGGER_RIGHT_CODE` and the right trigger axis.
    - T19: with a button bound to each trigger code, `verify(exactly = 0) { pad.getButton(TRIGGER_LEFT_CODE) }` and
      the same for `TRIGGER_RIGHT_CODE`.
  - Run the tests: T18 and T19 fail.
  - Code, read: in the button loop of `GamepadInput.held()`, a code equal to a trigger code reads
    `pad.getAxis(<that trigger axis>) >= TRIGGER_THRESHOLD`; every other code reads `pad.getButton(code)`.
  - Code, capture (the only change in `C/screens/menus/ControllerSettingsScreen.kt`, R3): in the `ControllerAdapter`
    of `buttonListener` (`:101-135`), add `axisMoved(controller, axisCode, value)`. It maps `TRIGGER_LEFT_AXIS` to
    `TRIGGER_LEFT_CODE` and `TRIGGER_RIGHT_AXIS` to `TRIGGER_RIGHT_CODE`; any other axis, or a value below
    `TRIGGER_THRESHOLD`, returns `false` at once (before the `selectedMegaButton` check, so stick moves log no error).
    Then it does what `buttonDown` does with the code: swap a duplicate code, set the code, save the prefs, remove
    the listener, clear `selectedMegaButton`, reset `delayOnChangeTimer`. The shared part goes into a private function
    of the listener object, which `buttonDown` also calls, so that the screen keeps one copy.
- Check: the build command passes. Core count = step 6 + 2. `git diff master -- C/screens/menus/ControllerSettingsScreen.kt`
  shows changes only inside the `buttonListener` object. Smoke launch as in step 5; then
  `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`, open the gamepad settings from the pause menu and
  back out with no exception.
- Commit: `031d: triggers can be bound on the gamepad bindings screen and act as buttons at 0.5`
- Contract checks: T18, T19, R3; intended change 8.
- Done: no

### 8. Contract checks

- Changes: none (fixes only if a check fails; a fix is its own commit).
- Check, in the worktree, on a clean build with the build command:
  - A1 to A4: the build passes; engine count as the baseline; core count = baseline + 23 (T1 to T23), 0 failures; the
    3 `KeyboardInputTest` and 8 `GamepadInputTest` cases are still there; the sorted warning list equals the baseline
    list from step 2 (compare the lists, not only the counts).
  - A5 to A11: the `git diff` and `grep` commands in `docs/CONTRACT.md`, as written there.
  - T1 to T23: each case is present by name and passes (read `core/build/test-results/test/`).
  - R1 to R4: read `git diff master -- core/src/main`. R1: of the 13 index-0 call sites, only
    `ControllerUtils.kt:11` and the 4 in `GamepadInput` changed. R2: only `getPreferredControllerCode` changed in
    `ControllerUtilsExtensions.kt`. R3: only the capture listener changed in `ControllerSettingsScreen.kt`. R4: the
    primary check runs before the read in `held()`.
  - Read the folder of `mega-refactor-051a-camera-read-interface` in its worktree again for overlap; write the result
    in `docs/NOTES.md`.

  Write each result in the process log. Then give the owner the manual checks of step 9 as a list of commands.
- Commit: `031d: contract checks` (branch docs only).
- Contract checks: A1 to A11, T1 to T23, R1 to R4.
- Done: no

### 9. Owner's play test and the branch half of P1

- Changes: none, unless the play test finds a problem. A fix is its own commit, and the affected automated checks
  run again.
- Check: the owner plays the manual checks in `docs/CONTRACT.md` once, with the PS3 pad for K and G, and the SNES pad
  as pad B for D1 to D4 (contract question 1). Before the bindings checks (K3, K7, G5, G6, G7, D2, D4), back up the
  prefs, and restore them after the checks, with the game closed:
  - `cp -a ~/.prefs ~/.prefs.bak-031d` (before)
  - `rm -rf ~/.prefs && mv ~/.prefs.bak-031d ~/.prefs` (after)

  All commands run from:

  `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-031d-devices`

  Keyboard only (no gamepad connected):

  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: K1 (default keys, every move, pause menu).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: K2 (arrow keys, `Enter` and numpad `Enter`, no
    double toggle, `D` then the right arrow, `W` then the up arrow on the ladder).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: K3 (bind `START` to `I`; `I` and `Enter` both
    open the pause menu).
  - `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`: K4 (arrows and `WASD` move the cursor; `Enter`, numpad `Enter`
    and `K` confirm).
  - `./run-game-alpha.sh --screen OPENING_SCREEN`: K5 (`START` skips, with numpad `Enter`, then `Enter`).
  - `./run-game-alpha.sh --screen CREDITS_SCREEN`: K5 (an exit button ends the credits early).
  - `./run-game-alpha.sh --screen GAME_OVER_SCREEN`: K5 (`START` continues).
  - `./run-game-alpha.sh --level INTRO_STAGE --room room1`: K6 (the right arrow held through the spawn and a room
    transition).
  - `./run-game-alpha.sh --level INTRO_STAGE --room room12`: K6 (the right arrow and `J` held through the boss intro
    and the defeat).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: K6 (an arrow held through the respawn).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: K7 (`Esc` does nothing; the up arrow is captured
    on the keyboard settings).

  One gamepad (the PS3 pad):

  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: G1 (default mapping, the K1 moves).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: G2 (the left stick: run, ladder up and down,
    crouch, small push does nothing, diagonal slide, the right stick does nothing).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: G3 (no drift at rest).
  - `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`: G4 (one stick push moves the cursor one step).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: G5 (code 0; move the pad's prefs file out of
    `~/.prefs/` first, as the contract says).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: G6 (a saved rebind is used after a replug).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: G7 (bind `B` to the right trigger and `A` to the
    left trigger; half pull does not capture).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: G8 (unplug while held: no pause, the keyboard
    works; replug works).
  - `./run-game-alpha.sh --level INTRO_STAGE --room room12`: G9 (stick and shoot held through the boss intro).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: G10 (keyboard and pad together; d-pad then stick
    on the ladder).

  Two gamepads (PS3 pad as A, SNES pad as B):

  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: D1 (both pads give input; one press for the same
    button on both).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: D2 (index shift: after A is unplugged, B is on the
    mapping).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: D3 (unplug B, then A; replug B).
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: D4 (the settings screen edits A's codes; the new
    code works on A and B).

  The branch half of P1 (from the same worktree; quit cleanly):

  - `./run-game-debug.sh --level REFACTOR_TEST_029 --room movement`: capture 3, no gamepad.
  - `./run-game-debug.sh --level REFACTOR_TEST_029 --room movement`: capture 4, one gamepad (the PS3 pad).
  - `./run-game-debug.sh --level REFACTOR_TEST_029 --room movement`: capture 5, two gamepads (optional).

  Run `utils/diagnostics/run.sh` on each capture. Write the 4 (or 5) `controllerPoller` rows side by side in the
  process log, with the dropped-frame counts. The no-pad branch row must be at the `master` level. The one-pad
  difference goes in the log; the owner accepts or rejects it at the merge review (contract question 2). Write the
  result of each K, G and D check in the process log, and which checks were skipped and why.
- Commit: `031d: play test results` (branch docs only).
- Contract checks: K1 to K7, G1 to G10, D1 to D4, P1.
- Done: no

### 10. Merge

Follow "Branch merge" in `docs/mega-refactor/CLAUDE.md`:

- Changes and checks, in this order:
  1. The results of steps 8 and 9 are in the branch process log.
  2. Get the owner's explicit approval to merge `mega-refactor-031d-devices`. Approval for `051a` or any other branch
     does not count. Merge one branch at a time.
  3. Overlap: read the folder of `mega-refactor-051a-camera-read-interface` in
     `.worktrees/mega-refactor-051a-camera-read-interface` again, and run
     `git diff --stat mega-refactor-031d-devices...master`. If `master`'s new changes touch the same files or systems
     (the 5 files of A7, their tests, `MegamanMaverickGame.kt`, `engine/input/`, the build files), merge `master` into
     the branch in its worktree, run the automated checks of step 8 again, and tell the owner the result. Else skip
     that merge. Write the result in `docs/NOTES.md`.
  4. From the main checkout: `git merge --squash mega-refactor-031d-devices`, then one commit named after the branch
     goal, for example `Devices: keyboard aliases, every connected gamepad, the left stick and the triggers
     (mega-refactor-031d)`. The message lists the main changes (the aliases with the `031e` `TODO`, the pad loop and
     the primary-pad tracking, the code-0 fix, the stick, the triggers and their capture, the removed overload) and
     ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Do not stage owner edits in the main
     checkout; no bare `git stash`.
  5. No commit of this branch is in `.git-blame-ignore-revs` (no mechanical commit), so that file does not change.
  6. On `master`, in a docs-only commit: an entry in `docs/mega-refactor/process-log.md` (the format in that file);
     in `ITEMS.md`, the `031d` row to `merged` with the finish date (item `031` stays `active`: `031e` and `031f`
     remain); in `ITEM.md`, `mega-refactor-031d-devices` (merged); each "Backlog candidate" in the branch
     `docs/TODO.md` becomes a new item, and each "Bug candidate" a file in `docs/issues/`. Mark issues
     `2026-10-25-gamepad-index-shift-keeps-old-codes.md` and `2026-10-25-preferred-controller-code-zero.md`: set
     `status: closed`, add a `## Resolution` section with the squash commit, and update the index row in
     `docs/issues/README.md`.
  7. `git worktree remove .worktrees/mega-refactor-031d-devices`, then `git branch -D mega-refactor-031d-devices`. If
     an empty folder stays, `rmdir .worktrees/mega-refactor-031d-devices/.gradle .worktrees/mega-refactor-031d-devices`.
  8. On `master`, run the build command. It passes, with the test counts of step 8 and the same warning list. If it
     fails, fix `master` at once, before any other merge.
- Check: `git worktree list` no longer shows the branch; the build on `master` passes; `ITEMS.md` shows `031d` as
  `merged`.
- Contract checks: all, as recorded in steps 8 and 9; the automated checks again on `master`.
- Done: no
