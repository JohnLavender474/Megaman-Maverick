# Process log: mega-refactor-031e-bindings

## Status

- State: active
- Goal: per-slot bindings: one settings object per slot replaces `ControllerButtons`, the bindings screen rules, `--resetBindings`, `selectButtonAction` per slot
- Done: steps 2 to 17, and the forced merge of `master` of step 18.3
- Next: the owner's merge approval, then the rest of step 18
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-30

- No branch exists yet. The folder was prepared on `master`. The goal is from the `031` decisions of 2026-10-25 and
  2026-10-26 (item `ITEM.md`).
- Scan on `5d4a37eac`: the root and `docs/mega-refactor/` `CLAUDE.md`, the item `ITEM.md` and `NOTES.md`, the `031d`
  folder, the 6 input issues, `C/input/`, `C/controllers/`, `E/controller/`, `E/input/`, `MegamanMaverickGame`,
  `MegaPlayers`, `MegaLevelScreen` (pause and `reset`), `LevelPauseScreen`, `ControllerSettingsScreen`,
  `GameSettingsPanel`, `DesktopLauncher`, the input tests, the `Lwjgl3Preferences` source, `~/.prefs/`, and the `051b` and
  `084a` scopes. Draft `docs/SCOPE.md` written.
- The owner approved `docs/SCOPE.md` with all 10 recommendations (verbatim: "yes"). Answers in `docs/SCOPE.md` and
  the item `ITEM.md`. One fact in the scope was corrected before the review: the keyboard prefs file exists, in the
  `031d` backup.
- Draft `docs/CONTRACT.md` written. Test rooms used (all checked in the live TMX `game_rooms` layers, each with a player
  spawn): `REFACTOR_TEST_029` `movement`, `enemy_aim` and `player_death` (`RefactorTest029.tmx`), `INTRO_STAGE` `room3`
  (`IntroStage_v3.tmx`). No new test room.
- The owner approved `docs/CONTRACT.md` ("contract approved"), with the recommendations for questions 2 to 5. Question 1:
  the owner has no prefs to protect ("I deleted the prefs, so nothing needs to be backed up"), so S1 is removed, and S2,
  S3 and X1 now only give a known start state. `docs/SCOPE.md`: the 2 text fixes, and the 2 test seams in the table of
  new APIs. Note: at this point `~/.prefs` holds 1 file and `~/.prefs.bak-031d` still exists; neither was changed.
- Draft `docs/PLAN.md` written (18 steps: branch start, baseline, `selectButtonAction` to `PlayerSettings`, the store and
  the slot file, keyboard codes, gamepad codes, deletion of the old storage, `selectButtonAction` at boot,
  `--resetBindings`, "RESET TO DEFAULTS", reserved keys, the `Enter` swap, labels, capture end, back to pause, contract
  checks, the owner's play test, merge).
- The owner approved `docs/PLAN.md` ("plan approved"), with the 3 recommendations. `docs/CONTRACT.md` Performance: the
  4 `playerSlot` reader lookups are added (a reading note, not a check).
- Step 1 (branch start): `ITEMS.md` row and `ITEM.md` set to active; worktree `.worktrees/mega-refactor-031e-bindings`
  created from `master`.
- Step 2 (baseline, on `3e4433744`): the build passes. Engine 575, core 301, 0 failures, 76 unique warning lines (96 `^w:` lines). This is the contract baseline: `master` has one docs commit after the branch start (`393d9be81`). `build-baseline-warnings.txt` written. Other active branches: `051b` and `084d` (new since the plan); see `docs/NOTES.md`.
- Step 3: `PlayerSettings` holds `selectButtonAction`, and `MegaPlayers.settings(slot)` gives one object per slot. The `MegaPlayersTest` case moved (T55); new T56. A check of the plan: `capacity` stays `1`. Core 302, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch: pass (`timeout`, no exception).
- Step 4: `PlayerSettingsStore` and `FakePreferences`, for `selectButtonAction`. Before the code (empty save): T7, T12 and T15 failed; T13 and T14 passed, as predicted. Core 307, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch: pass.
- Step 5: keyboard codes moved to `PlayerSettings.keyCodes` and the slot file. `KeyboardInput.held()` reads an index loop. The `TODO` at `readCode` is in (wrapped to 3 lines). Before the code (empty bodies): T3 and T9 failed, as predicted. Core 309, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch: pass.
- Step 6: gamepad codes moved to `PlayerSettings.padCodes` and the slot file; `game.buttons` goes. The 23 `GamepadInputTest` cases moved to `PlayerSettings` and the fake. Before the code (empty bodies): 13 failed: T2, T4, T5, T6, T10, T11, T20, T47, T49, T50, T51, and the 2 moved cases that read a saved table ("loads preferred codes", "reloads the codes from the prefs of the remaining pad"). T1, T48 and T52 passed, as predicted. Core 323, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch: pass.
- Step 7: deleted `ControllerButton`, `ControllerButtons`, `ControllerUtilsExtensions` and its test (its 2 cases are T1 and T2). Removed the old controller prefs name. Core 321, engine 575, 0 failures, 76 warning lines, no new line. A9, A10, A12, A13 and the `game.buttons` and `loadControllerButtons` halves of A14 pass; A6 passes for the working tree (`ls` of `E/controller/buttons/` gives 3 files). Smoke launch: pass.
- Step 8: `applyBootSettings` in `create()`. The saved `selectButtonAction` applies with or without `--loadKeyboardPrefs`. Before the code (today's rule): T21 failed; T22, T25 and T26 passed, as predicted. Core 325, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch: pass (`timeout`, no exception). One line in that launch was new: `Megaman is below game bounds, kill him` (an `ERROR` log, no exception). No agent input reached the game, and this step changes only the boot read. Not investigated, by the smoke rule.
- Step 9: `--resetBindings`, `params.resetBindings`, `clear()`. `applyBootSettings` calls `clear()` first. Before the code (empty `clear()`): T16, T17, T23, T24 and T54 failed. Prediction difference: the plan said T17 passes. The T17 case also asserts that the file exists on disk after `clear()`, so an empty body fails it. Core 330, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch, with no `--resetBindings`: pass. The `below game bounds` line of step 8 did not come back.
- Step 10: `clearKeyboard()`, `clearGamepad(padName)`, and the reset row calls them. On the gamepad screen with no pad, the row changes no code, as before, and `selectButtonAction` still goes to `NEXT_WEAPON` in memory, as on `master`. Before the code (empty bodies): T18, T19 and T53 failed; T8 passed, as predicted. Core 334, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch: pass. The `below game bounds` log line came back in this launch (no exception). It comes in some launches only (steps 8 and 10). Not investigated, by the smoke rule.
- Step 11: `KeyboardInput.isReservedKey`, `PlayerSettings.captureKey` and `KeyCapture`; the key capture of the screen calls them, and the hint adds `Refused: reserved key` while the flag is set. T29 reads the alias table through `held()` (all 256 codes, 6 hits), because the table is private. Before the code (`false` and today's swap): T27, T29, T30, T33, T34, T35 and T36 failed; T28, T31, T32, T38, T39 and T41 passed, as predicted. Core 347, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch and `--screen KEYBOARD_SETTINGS_SCREEN` launch: pass.
- Step 12: `captureKey` refuses a key that another button has when the current key of the selected button is reserved. The `TODO` at the alias table is gone (A11 gives no output). Before the code: T37 and T40 failed; T42 passed, as predicted. Core 350, engine 575, 0 failures, 76 warning lines, no new line.
- Step 13: `keyLabel` and `padLabel`; the rows use them. Plan change: the gamepad hint breaks into 2 lines (`Press any controller` and `button or trigger`). In one line it has 38 characters at the 16-pixel font, which is more than the 512-pixel screen width (inference from the font size; the owner sees it in G5). The words stay as the contract says. Before the code (raw-number bodies): T43 and T46 failed; T44 and T45 passed, as predicted. Core 354 (the minimum of A3), engine 575, 0 failures, 76 warning lines, no new line. Smoke launch and `--screen KEYBOARD_SETTINGS_SCREEN` launch: pass, no exception from the rows.
- Step 14: private `capturePad` and `endCapture()` in `ControllerSettingsScreen`. Both capture ends and `reset()` call it. `render` ends a capture when the primary pad is not the pad of the capture (`ERROR_SOUND`, `delayOnChangeTimer.reset()`). R4 by reading the diff: pass. Core 354, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch, `--screen KEYBOARD_SETTINGS_SCREEN` and `--screen CONTROLLER_SETTINGS_SCREEN`: pass, no exception. Difference from the plan: the last launch stayed on the gamepad screen and the log has no `No controller found` line, because a pad was connected to the machine (`/dev/input/js0`, `usb_gamepad`). So this launch did not exercise the no-pad path; K11 covers it.
- Step 15: `showOverPause` and `closeOverPause` on the game, 2 private held-level fields, and the guard in `resume()`. The 2 `GameSettingsPanel` sites use them. `ControllerSettingsScreen.show(props)` stops the music only with no back action. R1, R2 and R3 by reading the diff: pass. A15 gives no output. Core 354, engine 575, 0 failures, 76 warning lines, no new line. Smoke launch: pass (it does not reach this path; the H checks are the owner's).
- Step 16 (contract checks, on `e29094367`, clean build):
  - A1: pass. A2: engine 575, 0 failures: pass. A3: core 354 (minimum 354), 0 failures: pass. A4: 76 unique warning lines, `comm` against `build-baseline-warnings.txt` gives no new line: pass. A5: no output: pass.
  - A6: 2 `D` lines; `ls` gives the 3 files: pass. A7: the 14 paths and no other: pass. A8: only the listed test files (the fake is `FakePreferences.kt`; the capture decision test is `PlayerSettingsTest.kt`): pass. A9, A10, A11, A12: no output: pass. A13: `PlayerSettingsStore.kt` and the 3 save file lines: pass. A14, A15: no output: pass. A16: `1` and one line: pass.
  - T1 to T56: all present and pass. `PlayerSettingsStoreTest` 26 cases, `PlayerSettingsTest` 17, `KeyboardInputTest` 13 (10 old), `GamepadInputTest` 31 (23 old), `MegaPlayersTest` 10 (9 old, 1 new). Names:
    - T1 "gives null for a missing key" (gamepad tables); T2 "gives a saved code, also 0"; T3 "writes the 8 keys of the table and flushes once"; T4 "writes the 8 keys of the pad name and flushes once"; T5 "does not mix the keyboard table and a gamepad table"; T6 "does not mix 2 pad names"; T7 "opens one file per slot, and the values of 2 slots do not mix"; T8 "opens only the slot file over all store functions, never an old file"; T9 "loads a partial table: each saved key changes, and each missing key keeps the current code"; T10 "loads a partial table, and a pad name with no table changes no code"; T11 "round-trips the trigger codes"; T12 "round-trips each value"; T13 "gives null for a missing key" (selectButtonAction); T14 "gives null for an unknown name"; T15 "keeps one key after a save from each bindings screen"; T16 "removes the keyboard table, 2 pad tables and the select action, and flushes"; T17 "gives no exception with no file on disk, and leaves an empty file"; T18 "the keyboard reset removes its table and the select action, keeps every pad table, and flushes"; T19 "the gamepad reset removes its table and the select action, keeps the others, and flushes"; T20 "keeps pad names through a round trip of the XML".
    - T21 "with no flag, gives the default keys and no pad codes, and applies the saved select action"; T22 "with --loadKeyboardPrefs, applies the saved keys and the saved select action"; T23 "with --resetBindings, gives the defaults and leaves the file with no keys"; T24 "with --resetBindings and --loadKeyboardPrefs, clears first and gives the same result"; T25 "gives NEXT_WEAPON with no saved select action, with each flag combination"; T26 "does not apply a saved gamepad table, with each flag combination".
    - T27 "is true for Esc, Enter, numpad Enter and the 4 arrow keys"; T28 "is false for W, K and P"; T29 "is true for each key of the alias table".
    - T30 "refuses each reserved key for A and changes no code"; T31 "accepts a free key for A and changes only A"; T32 "accepts the key of B for A, with the swap"; T33 "keeps the key that A already has"; T34 "keeps Enter for START on Enter, and does not refuse it"; T35 "refuses numpad Enter for START on Enter"; T36 "refuses Enter for START on I"; T37 "refuses the key of A for START on Enter, because the swap would move Enter to A"; T38 "accepts a free key for START on Enter, and no other button gets Enter"; T39 "accepts the key of A for START on I, with the swap"; T40 "does not move a reserved key from a hand-edited table"; T41 "accepts P for A, because P is not reserved"; T42 "leaves 8 different codes after every accept".
    - T43 "gives the key names of the real Input.Keys"; T44 "gives the raw number for a keyboard code out of range, with no exception"; T45 "gives the raw number for a keyboard code in range with no name"; T46 "gives L2 and R2 for the trigger codes and the number for other gamepad codes".
    - T47 "gives the mapping, then the saved codes after the flag, and the mapping for a name with no table"; T48 "starts with 'loadPrefsOnConnect' false"; T49 "gives the saved code of each saved key and the mapping of each missing key"; T50 "keeps a saved code 0"; T51 "reads the axis, not 'getButton', for a saved trigger code"; T52 "has every code as NO_CODE with no pad, and a NO_CODE never reaches 'getButton'"; T53 "gives the mapping for a new pad object after the gamepad reset, with the flag"; T54 "gives the mapping for a new pad object after clear(), with the flag"; T55 "starts as NEXT_WEAPON and keeps a value that is set"; T56 "has one settings object per slot, and a change to one does not change the other".
  - R1 to R9 (read `git diff master...HEAD -- core/src/main lwjgl3/`): all pass. R8: `capacity` default `1`; a `grep` for `PlayerSlot([1-9]` gives no output. R9: each API in the plan table has its production caller; no other public API is added.
  - Code comments: the only new comment in `core/src/main` and `lwjgl3/` is the `TODO` of step 5.
  - Overlap: `051b` merged into `master` during this work. Result and method in `docs/NOTES.md`. Plan change: `git merge-tree --write-tree` needs git `2.38`, the local git is `2.34.1`, so the check used the old 3-argument form. No conflict marker against `master` or `084d`. Step 18 needs the forced merge.
  - The backlog candidate is in `docs/TODO.md`. The S, K, G, D, H, B and X checks are not run: they are the owner's.
  - Note for the owner: the `ERROR` line `Megaman is below game bounds, kill him` came in 2 of the smoke launches of `--room movement` (steps 8 and 10), with no agent input and no exception. Not investigated, by the smoke rule. The owner sees in K1 if it means anything.
- Merged `master` (`051b`, `6b0097aa1`) into the branch before the play test, with no conflict, so that the play test
  covers the combined code (plan step 18.3 forces this merge). Clean build passes; 585 engine and 363 core tests, 0
  failures (+10 and +9 from `051b`); the same 76 unique warnings.

## 2026-11-01

- Step 17: the owner ran the manual play test on `804253ca3`. Result, verbatim: "all tests passed". The owner gave no
  per-check detail and named no skipped check, so this log records S, K, G, D, H, B and X as one pass.
- The 1-frame pause grid (plan question 2): the owner reported no problem, so it is accepted. `showOverPause` has the
  `TODO`, with item `013` as the follow-up.
- `master` moved after the play test (`051` parent, `084b` to `084f`). Step 18.3 forces the merge: `MegamanMaverickGame.kt`,
  `MegaLevelScreen.kt`, `Constants.kt`, `ControllerComponent.kt` and `AirDashBehavior.kt` are in its list. Merge commit
  `8ed39c993`, done before the merge approval, so that the approval covers the combined code. Result in `docs/NOTES.md`.
