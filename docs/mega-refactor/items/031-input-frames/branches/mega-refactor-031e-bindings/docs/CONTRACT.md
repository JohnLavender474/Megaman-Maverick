# Contract: mega-refactor-031e-bindings

The gameplay that this branch must not change, the gameplay that it changes on purpose, and how to check both. This
was written ahead of time on `master` (`87a7b648c`), before the branch existed. It follows the approved
`docs/SCOPE.md` (2026-10-30).

- Status: approved
- Approved: 2026-10-30

Bindings and `selectButtonAction` move to one settings object per player slot, with one new prefs file per slot. The
keyboard bindings screen refuses the reserved keys. `BACK` from a bindings screen in a level goes back to pause and
keeps the level. A capture ends when the player leaves the screen or when the pad goes. Everything else about input
must feel the same: the replug decision, the aliases, every pad, the stick, the triggers, and the menus.

The largest risk is "Level under another screen" in `docs/SCOPE.md`, so back to pause has its own check group (H1 to
H10).

## Where a real library differs from a mock

In `031d` a unit-test mock of the controller list hid a crash. These are the cases for `031e`. Each case names its
check. The source is `gdx` `1.14.2`, `gdx-backend-lwjgl3` `1.14.2` and `gdx-controllers` `2.2.3`.

- **`Lwjgl3Preferences` keeps the file in memory.** `put` and `clear()` change memory only, and `flush()` writes the
  file. A mock of `Preferences` shows none of this. [T3, T16, T17, K7, B5]
- **`getPreferences` keeps one object per name for the whole run.** A change to the file during a run is not read. So
  each check that edits a file does it with the game closed. [B7]
- **`getInteger(key)` gives `0` for a missing key, and `getInteger(key, default)` calls `Integer.parseInt` with no
  guard.** A value that is not a number throws. [T1, T2, B7, open question 3]
- **A file with broken XML does not throw.** The constructor prints the stack trace and continues with no keys. [B7]
- **`flush()` creates a missing `~/.prefs` folder, because `file.write(false)` makes the parent folder.** A clean start
  has no folder. [B1, T17]
- **`storeToXML` must escape a pad name in a key.** A mock never does this. [T20]
- **`Input.Keys.toString` throws `IllegalArgumentException` for a code below `0` or above `255`.** It returns `null`
  only for a code in that range with no name, and `"Unknown"` for `0`. The tests use the real class. [T44, T45, B7]
- **`isKeyPressed(-1)` is `ANY_KEY`, so it is true for any key.** A hand-edited `-1` makes that button held by any key.
  This branch does not change it. `--resetBindings` is the answer. [B7]
- **A real key has a real code.** `Enter` is `66`, numpad `Enter` is `160`. A test can pass the right constant for the
  wrong key. [K3, K4]
- **The controller list is live, and it can lose 2 pads at one time.** [D2, D3, D8]
- **`addListener` and `removeListener` act on a real pad object, and the object of an unplugged pad stays dead.** The
  screen has no tests. [G9, H6, D2, D8]
- **A pad name is the name that the library gives.** The saved table of a pad depends on it, also after a replug. [G2,
  D5]
- **Window focus and resize come from GLFW.** [H7, H8]
- **`pauseAllSound()` pauses each `Sound`.** On this path the bindings screen plays its menu sounds while the level is
  paused. Today the screen does not run in that state. [H1, H2]
- **`game.render` keeps the old screen in a local value for the draw of that frame.** So after an open or a close, the
  old screen draws one more frame with the moved UI camera. [H1, H2, H9]

## Automated checks

Run from the worktree, on a clean build:

```
JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar 2>&1 | tee build.log
```

Read the test counts from `engine/build/test-results/test/` and `core/build/test-results/test/`. Baseline on `master`
`87a7b648c`: the build passes; engine 575 tests, core 301 tests, 0 failures; 76 unique warning lines (96 `^w:` lines
before the `sed`). The `084a` entry in `docs/mega-refactor/process-log.md` gives 876 tests and no warning count, so
the warning count was measured with a clean build of an export of `87a7b648c`. The older number `81` is from before
`084a`. Plan step 1 writes the sorted list in the worktree, because the lines hold absolute paths.

Kotest here does not accept the Gradle `--tests` filter, so each run of one test is a full `core:test` run.

### Build and counts

- [ ] A1. The command above passes.
- [ ] A2. Engine: 575 tests, 0 failures. The engine has no test of the deleted types: `grep -rnwE 'ControllerButtons?'
      engine/src/test` gives no output on `87a7b648c`. So the count does not change. Engine files only get deleted, so
      no new engine test exists (A6 proves the deletion).
- [ ] A3. Core: 301 tests plus the new tests, 0 failures. The minimum is 354: 301 plus 53, because T1 to T56 are 56
      tests, and T1, T2 and T55 take the place of old tests. No old case is lost: the 2 `ControllerUtilsExtensionsTest`
      cases become T1 and T2, the 10 `KeyboardInputTest` cases and the 23 `GamepadInputTest` cases stay with the same
      meaning, and the 9 `MegaPlayersTest` cases stay.
- [ ] A4. Warnings: no new line against the sorted list of plan step 1. Count with
      `grep '^w:' build.log | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u | wc -l`. Compare the sorted lists, not only the
      counts. A line can leave the list (for example the unchecked cast in `ControllerSettingsScreen.kt`).
- [ ] A5. `git diff master...HEAD --stat -- core/src/test/resources/contact-rule-selection.txt` gives no output.

### Scope greps (from the worktree)

The commands use `master...HEAD`, because `031d` found that `git diff master` also shows the reverse of new `master`
changes.

- [ ] A6. `git diff master...HEAD --name-status -- engine/` lists 2 lines, both `D`: `ControllerButton.kt` and
      `ControllerButtons.kt`. `ls engine/src/main/kotlin/com/maverick/game/engine/controller/buttons/` lists
      `ButtonActuator.kt`, `ButtonStatus.kt` and `IButtonActuator.kt`. This also proves that `InputFrameBuilder`,
      `ControllerSystem` and the rest of the engine do not change.
- [ ] A7. `git diff master...HEAD --name-status -- core/src/main lwjgl3/` lists only these 14 paths (`C` =
      `core/src/main/kotlin/com/megaman/maverick/game/`). With the 2 engine files of A6, these are the 16 production
      files of `docs/SCOPE.md`:
  - Added: `C/players/PlayerSettings.kt`, `C/players/PlayerSettingsStore.kt`.
  - Deleted: `C/controllers/ControllerUtilsExtensions.kt`.
  - Changed: `C/input/KeyboardInput.kt`, `C/input/GamepadInput.kt`, `C/screens/menus/ControllerSettingsScreen.kt`,
    `C/screens/utils/GameSettingsPanel.kt`, `C/MegamanMaverickGame.kt`, `C/players/MegaPlayers.kt`, `C/Constants.kt`,
    `C/entities/megaman/components/ControllerComponent.kt`, `C/entities/megaman/behaviors/AirDashBehavior.kt`,
    `C/screens/levels/MegaLevelScreen.kt`, and
    `lwjgl3/src/main/kotlin/com/megaman/maverick/game/lwjgl3/DesktopLauncher.kt`.
  - So `LevelPauseScreen.kt`, `MegaMenuScreen.kt` and `ScreenController.kt` do not change.
- [ ] A8. `git diff master...HEAD --name-status -- core/src/test engine/src/test` lists only: `KeyboardInputTest.kt`,
      `GamepadInputTest.kt`, `MegaPlayersTest.kt` (changed), `ControllerUtilsExtensionsTest.kt` (deleted),
      `PlayerSettingsStoreTest.kt` and the capture decision test (added), and the fake of T1, if it has its own file.
- [ ] A9. `grep -rnwE 'ControllerButtons?' engine/src core/src lwjgl3/src` gives no output.
- [ ] A10. `grep -rn 'ControllerUtilsExtensions' core/src lwjgl3/src` gives no output.
- [ ] A11. `grep -n 'TODO' core/src/main/kotlin/com/megaman/maverick/game/input/KeyboardInput.kt` gives no output. The
      `031e` `TODO` at the alias table is gone.
- [ ] A12. `grep -rnE 'Controller Preferences|Keyboard Preferences|MEGAMAN_MAVERICK_(CONTROLLER|KEYBOARD)_PREFERENCES'
      core/src/main lwjgl3/src` gives no output.
- [ ] A13. `grep -rn 'getPreferences(' core/src/main lwjgl3/src` lists only `PlayerSettingsStore.kt` and the 3 save
      file lines in `MegamanMaverickGame.kt`.
- [ ] A14. `grep -rnE 'game\.buttons|players\.selectButtonAction|loadControllerButtons' core/src lwjgl3/src` gives no
      output, and `grep -n 'selectButtonAction' core/src/main/kotlin/com/megaman/maverick/game/players/MegaPlayers.kt`
      gives no output.
- [ ] A15. `grep -n 'startLevel' core/src/main/kotlin/com/megaman/maverick/game/screens/utils/GameSettingsPanel.kt`
      gives no output.
- [ ] A16. In `MegamanMaverickGame.kt`, `grep -c 'slot0FrameBuilder.build'` gives `1`, and
      `grep -n 'beginEntry("controllerPoller")'` gives one line.

### Named tests (core)

`docs/SCOPE.md` says "mocked `Gdx.app` prefs". The prefs in these tests are one fake, not a relaxed mock, because of
the first cases of "Where a real library differs from a mock". The fake is behind a mocked `Gdx.app`. It holds a real
`java.util.Properties`, changes memory only until `flush()`, reads with `Integer.parseInt`, and writes and reads with
`storeToXML` and `loadFromXML`. The key tests use the real `Input.Keys`.

`PlayerSettingsStoreTest` (new; it takes over `ControllerUtilsExtensionsTest`):

- [ ] T1. A missing key gives `null`.
- [ ] T2. A saved code gives its value, also `0`.
- [ ] T3. `saveKeyboard` writes the 8 keys `KEYBOARD.<BUTTON>` and flushes once.
- [ ] T4. `saveGamepad(padName)` writes the 8 keys `GAMEPAD.<pad name>.<BUTTON>` and flushes once.
- [ ] T5. The keyboard table and a gamepad table do not mix: the same button has 2 values.
- [ ] T6. Two pad names do not mix.
- [ ] T7. Slot 0 and slot 1 open 2 files, `... - Slot 0` and `... - Slot 1`, and their values do not mix.
- [ ] T8. Over all store functions, `Gdx.app.getPreferences` gets only `Megaman Maverick - Player Settings - Slot 0`.
      The 2 old names are never opened.
- [ ] T9. `loadKeyboard` with a partial table: the saved keys change, and each missing key keeps the current code.
- [ ] T10. `loadGamepad(padName)` with a partial table: the same. A pad name with no table changes no code.
- [ ] T11. The trigger codes `1004` and `1005` round-trip through a gamepad table.
- [ ] T12. `selectButtonAction` round-trips, for each of the 3 values.
- [ ] T13. A missing `SELECT_BUTTON_ACTION` key gives `null`.
- [ ] T14. An unknown name in `SELECT_BUTTON_ACTION` gives `null`.
- [ ] T15. The file has one `SELECT_BUTTON_ACTION` key, also after a save from each bindings screen.
- [ ] T16. `clear()` removes all keys (the keyboard table, 2 pad tables, `SELECT_BUTTON_ACTION`) and flushes.
- [ ] T17. `clear()` with no file on disk gives no exception and an empty file.
- [ ] T18. The keyboard reset removes the keyboard table and `SELECT_BUTTON_ACTION`, keeps every pad table, and flushes.
- [ ] T19. The gamepad reset removes the table of that pad name and `SELECT_BUTTON_ACTION`, keeps the keyboard table and
      the other pad table, and flushes.
- [ ] T20. A round trip through XML keeps the pad names `A&B <Pad>`, `PS3 Controller` and `Pad.With.Dots`.

Boot (see open question 2, for the function that these tests call):

- [ ] T21. No flag: default keys, gamepad codes `NO_CODE`, and the saved `selectButtonAction` applies.
- [ ] T22. `--loadKeyboardPrefs` only: the saved keys and the saved `selectButtonAction` apply.
- [ ] T23. `--resetBindings` only: default keys, `NEXT_WEAPON`, and the file has no keys.
- [ ] T24. `--resetBindings` with `--loadKeyboardPrefs`: the result of T23. The clear runs first.
- [ ] T25. No saved `selectButtonAction`: `NEXT_WEAPON`, with each flag combination.
- [ ] T26. A saved gamepad table does not apply at boot, with each flag combination.

`KeyboardInputTest` (the 10 cases stay, moved to `PlayerSettings`):

- [ ] T27. `isReservedKey` is true for `Esc`, `Enter`, numpad `Enter` and the 4 arrow keys.
- [ ] T28. `isReservedKey` is false for `W`, `K` and `P`.
- [ ] T29. Each key of the alias table is reserved (one source for the list).

The capture decision (new test; the pure function next to `PlayerSettings`, with the outcomes accept, keep and refuse):

- [ ] T30. `A` selected, each of the 7 reserved keys: refuse. No code changes.
- [ ] T31. `A` selected, the free key `I`: accept. Only `A` changes.
- [ ] T32. `A` selected, `J` (the key of `B`): accept with the swap. `A` has `J`, `B` has `K`.
- [ ] T33. `A` selected, `K` (its own key): keep. No code changes.
- [ ] T34. `START` on `Enter` selected, `Enter`: keep. This is not a refusal.
- [ ] T35. `START` on `Enter` selected, numpad `Enter`: refuse.
- [ ] T36. `START` on `I` selected, `Enter`: refuse. It is reserved and it is not the current key.
- [ ] T37. `START` on `Enter` selected, `K` (the key of `A`): refuse. The swap would move `Enter` to `A` (question 4).
- [ ] T38. `START` on `Enter` selected, the free key `I`: accept. No other button gets `Enter`.
- [ ] T39. `START` on `I` selected, `K`: accept with the swap. `A` has `I`.
- [ ] T40. A hand-edited table with `UP` on the up arrow: `UP` selected and `S` (the key of `DOWN`) gives refuse;
      `DOWN` selected and the up arrow gives refuse; `UP` selected and the up arrow gives keep.
- [ ] T41. `A` selected, `P`: accept. `P` is not reserved.
- [ ] T42. After every accept (each button with each free key and each key of another button), the 8 codes are
      different.

Labels (see open question 2):

- [ ] T43. Keyboard names from the real `Input.Keys`: `K` gives `K`, `Enter` gives `Enter`, left shift gives `L-Shift`.
- [ ] T44. Keyboard codes `-1`, `256` and `999` give the raw number, with no exception.
- [ ] T45. A keyboard code in `1..255` with no name gives the raw number.
- [ ] T46. Gamepad codes: `1004` gives `L2`, `1005` gives `R2`, `3` gives `3`. The keyboard label of `1004` is the raw
      number, not `L2`.

`GamepadInputTest` (the 23 cases stay, moved to `PlayerSettings` and the store):

- [ ] T47. The replug sequence, as one test: a saved table and the flag `false` give the mapping; the flag `true` and a
      new pad object give the saved codes; one more new object gives the saved codes; a pad name with no table gives
      the mapping.
- [ ] T48. A new `GamepadInput` starts with `loadPrefsOnConnect` `false`.
- [ ] T49. A partial table with the flag `true`: each saved key gives its code, and each missing key gives the mapping.
- [ ] T50. A saved code `0` with the flag `true` stays `0` (the owner's PS3 table has `A` on `0`).
- [ ] T51. A saved trigger code with the flag `true` reads `getAxis`, not `getButton`.
- [ ] T52. With no pad every code is `NO_CODE`. A code equal to `NO_CODE` never reaches `Controller.getButton`.
- [ ] T53. After the gamepad reset, with the flag `true`, a new pad object gives the mapping.
- [ ] T54. After `clear()`, with the flag `true`, a new pad object gives the mapping.

`MegaPlayersTest` (the 9 cases stay):

- [ ] T55. The `selectButtonAction` case moves to `settings(slot)`. The default is `NEXT_WEAPON`.
- [ ] T56. With capacity 2, `settings(PlayerSlot(0))` and `settings(PlayerSlot(1))` are 2 objects, and a change to one
      does not change the other.

The engine has no new test: it only loses 2 classes that nothing reads. `ControllerSettingsScreen`, `GameSettingsPanel`
and the 2 new functions on the game need a running game, so the manual checks cover them. No temporary contract suite:
the parts that stay the same are the kept test cases.

### Review (read the diff, no play needed)

- [ ] R1. The open function calls no `hide()`, no `reset()`, no `resume()` and no `setCurrentScreen`. The close function
      calls no `show()`, no `resume()` and no `setCurrentScreen`.
- [ ] R2. `resume()` returns before it changes `clock.paused` while the level is held, and only then.
- [ ] R3. `ControllerSettingsScreen.show` stops the music on every path except this one.
- [ ] R4. `reset()` of the screen calls `endCapture()`. `render` ends a gamepad capture when the primary pad is not the
      pad of the capture.
- [ ] R5. In `create()`, `--resetBindings` runs before each read of the file.
- [ ] R6. `loadPrefsOnConnect` stays on `GamepadInput`, starts `false`, and is not saved. `PlayerSettings` holds no
      session flag.
- [ ] R7. The key capture saves after an accept. It does not save after a refuse or a keep.
- [ ] R8. `MegaPlayers.capacity` stays `1`. No code names a slot above 0, except loops to `capacity`.
- [ ] R9. Each new API has a caller from the table in `docs/SCOPE.md`. The only additions are the ones that open
      question 2 approves.

## Performance

No performance captures: the scope calls for none. The branch adds no native call per tick. `held()` reads an
`IntArray` in place of an `OrderedMap` lookup, so it has less work per tick. The new per-tick work is one reference
compare in `ControllerSettingsScreen.render`, only while that screen is current, and one `playerSlot` component lookup
in each of 4 reader lines when they run (`AirDashBehavior.kt:33` and `:38`, `ControllerComponent.kt:99` on a select
release, `MegaLevelScreen.kt:926`). These lookups are not measured (corrected on 2026-10-30 at the plan review). The `"controllerPoller"` entry keeps
its name (A16).

## Intended changes

Each entry is a gameplay change from `docs/SCOPE.md` "Intended changes". The manual check that proves it is named in
brackets.

1. Bindings save to and load from `Megaman Maverick - Player Settings - Slot 0`. The old files are not read, so "LOAD
   SAVED SETTINGS" and `--loadKeyboardPrefs` find nothing until the player rebinds. [B1, B2, B3, K7, G2]
2. `selectButtonAction` has one saved value for the slot. Today the value that loads depends on the screen. [G8]
3. A reserved key in a keyboard capture is refused, with a sound and a reason. Today it is accepted. [K3]
4. A rebind that would move a reserved key to another button is refused. Today it swaps. [K4]
5. `BACK` from a bindings screen in a level goes back to the settings panel of the pause menu, in the same level state.
   Today the level restarts. Fixes issue `docs/issues/2026-10-25-bindings-screen-restarts-level.md`. [H1, H2, H3, H4, H9]
6. The level music keeps playing on the bindings screen in a level. Today it stops. [H1, H2, H9]
7. Leaving the screen ends the capture. The next visit shows the menu, and no listener stays on the old pad. Fixes issue
   `docs/issues/2026-10-25-capture-state-left-after-gamepad-disconnect.md`. [G9, H6]
8. If the primary pad goes during a capture and another pad stays, the capture ends and the menu works. Today the screen
   stays on the hint with no way out. This is the worse form of the same issue. [D2]
9. A bound trigger shows `L2` or `R2`, and the gamepad hint names triggers. Keyboard rows show key names, with the raw
   code as fallback. [G5, K5, K6, B7]
10. `--resetBindings` exists. [B5, B6, B7]
11. "RESET TO DEFAULTS" also clears the saved values of that table and `SELECT_BUTTON_ACTION`. Fixes issue
    `docs/issues/2026-10-25-reset-to-defaults-not-saved.md`. [K7, G7]
12. The saved `selectButtonAction` applies at boot, with or without `--loadKeyboardPrefs`. Fixes issue
    `docs/issues/2026-10-22-select-button-action-resets.md`. [B4]

Issue `docs/issues/2026-10-22-saved-key-bindings-not-applied.md` is not fixed here. It closes as "on purpose" at the
merge (`docs/SCOPE.md`, question 9). Issue `docs/issues/2026-10-25-p-key-binding-takes-screenshots.md` stays open: `P`
is not a reserved key (K6, T41).

## Must stay the same

- **The replug decision, exactly.** A replug gives the default bindings until "LOAD SAVED SETTINGS". After that button,
  later replugs in the session use the saved bindings. A new session starts with the defaults. A pad name with no saved
  table gets the mapping, also after that button. [G2, G3, G4, D6, T47, T48]
- **The `031d` behavior.** The aliases (`Enter`, numpad `Enter` and the arrows always work, with one bit per button)
  [K2]. Every connected pad gives input, and every pad reads one code set, from the primary [D1, D4]. The left stick
  and the triggers read as in `031d` [G1, G5]. A capture listens to the primary pad only [D4].
- **The input feel.** The default keys (`WASD`, `K`, `J`, `Enter`, `L`), the default mapping, and all moves. [K1, G1]
- Keyboard codes at boot are the defaults, unless `--loadKeyboardPrefs` is given. Saved bindings never apply at boot
  without that flag. Gamepad bindings never apply at boot. [B3, G3, T21, T26]
- 1:1: each button has one code in each table, and the swap rule stays, except for intended change 4. [K5, G6, T42]
- A rebind saves at once. The select row saves at once and cycles in the same order. [K5, K8]
- The default `selectButtonAction` is `NEXT_WEAPON`, and the 3 actions behave the same. [K8, T25]
- An unplugged gamepad does not pause the game. The keyboard and any other pad keep working. [G1, D3]
- From the main menu, `BACK` returns to the main menu, and the music stops as today. [K9]
- The gamepad row refuses to open with no pad. The screen backs out when the last pad goes. [K10, K11, G9, H5]
- The menu input of the bindings screen and of all other menus (A7: `MegaMenuScreen.kt` does not change). [K9, H1]
- The pause menu itself, and a level that the player does not leave through this path. [H1, H2, A7]
- The slot-0 frame builds at one place, and the diagnostics entry keeps its name. [A16]
- `InputFrameBuilder`, `ControllerSystem`, `TURN_CONTROLLER_*` and the rest of the engine. [A6]

## Manual play test

The owner plays these once, after the implementation, from the worktree. The owner knows today's behavior, so there is
no baseline play-through. No new test room is needed. The rooms are in the live TMX files: `REFACTOR_TEST_029`
(`RefactorTest029.tmx`) `movement`, `enemy_aim` and `player_death`; `INTRO_STAGE` (`IntroStage_v3.tmx`) `room3`.

### Prefs safety

The owner has no real files to protect in `~/.prefs` (open question 1). The checks still need a known start state,
because each check marked `[prefs]` writes to this folder or removes it. Do these 2 steps once, with the game closed,
before the first check. Do not touch `~/.prefs.bak-031d`.

- [ ] S2. Make the start state. The first test stops the command if it exists:
      `test ! -e ~/.prefs.bak-031e && cp -a ~/.prefs ~/.prefs.bak-031e`
- [ ] S3. Check the start state: `diff -r ~/.prefs ~/.prefs.bak-031e` prints nothing.

The last step of each `[prefs]` check is the restore command. Run it with the game closed, also when the check fails:
`rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`. It keeps the start state, and the next check starts from
it. Do not skip it: most checks expect no `Player Settings` file at the start. X1, at the end of this section, is the
last restore.

The checks use these words:

- "Select" a row: move the cursor to the row, then press and release `Enter` (or the pad's `A` button). The menus act on
  the release.
- "The file" is `~/.prefs/"Megaman Maverick - Player Settings - Slot 0"`.
- "Go to pause, gear, `KEYBOARD SETTINGS`": press `Enter` (or the pad's `START` button) in the level, select the gear in
  the pause menu, select `KEYBOARD SETTINGS` in the panel. `CONTROLLER SETTINGS` is the row for the gamepad screen.
- "Resume": in the pause menu, select the panel `BACK` if the panel shows, move the cursor to a weapon, and select it.

All commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-031e-bindings`

### Keyboard only

Unplug every gamepad before the launch.

- [ ] K1. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  - With `WASD`, `K`, `J`, `Enter`, `L`: run, stop, hold `A` and `D` together, jump (short and full), wall slide and
    wall jump, air dash, crouch and ground slide, climb the ladder, swim, shoot, charge and release `J`.
  - See: `L` changes the weapon. `Enter` opens the pause menu. All moves are as today.
- [ ] K2. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  - The arrow keys move Mega, climb the ladder up and down, and crouch.
  - `Enter` and numpad `Enter` each open and close the pause menu. One press opens it, and it stays open.
  - Hold `D`, also press the right arrow, then release `D`. See: Mega keeps running with no stop.
- [ ] K3. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Select row `A`. The hint shows.
  2. Press these 7 keys, one at a time: `Esc`, `Enter`, numpad `Enter`, up arrow, down arrow, left arrow, right arrow.
     See for each: the error sound, a line in the hint that says why, and the hint stays open.
  3. Press and release `K` (the key that `A` has). See: the hint closes, and the row shows `A: K`.
  4. Select `BACK`. Resume. See: `K` jumps.
  5. Close the game. `ls ~/.prefs | grep -c "Player Settings"` prints `0`: nothing was saved.
  6. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] K4. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Select row `START`. Press `K` (the key of `A`). See: the error sound, the
     reason in the hint, and the hint stays open.
  2. Press `I`. See: `START: I`, and `A: K` does not change.
  3. Select row `START`. Press `Enter`. See: the error sound, and the hint stays open. Press `K`. See: `START: K` and
     `A: I` (a normal swap, because `Enter` does not move).
  4. Select `BACK`. Resume. See: `K` and `Enter` both open the pause menu, and `I` jumps.
  5. Select `RESET TO DEFAULTS` on the same screen (pause, gear, `KEYBOARD SETTINGS`). See: `START: Enter`, `A: K`.
  6. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] K5. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. See: the rows show names (`UP: W`, `A: K`, `START: Enter`), not numbers.
  2. Select row `A`. Press `J`. See: `A: J` and `B: K` (the swap).
  3. Close the game with no `BACK`. `grep -c "KEYBOARD\." ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints
     `8`: the rebind saved at once.
  4. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] K6. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`.
  2. Bind these keys, one row each: `UP` to left shift, `DOWN` to `Space`, `LEFT` to `F1`, `RIGHT` to `/`, `A` to `P`.
  3. See: each row shows a readable name, and every character draws (for example `UP: L-Shift`). `A: P` is accepted.
     Write any character that does not draw in the process log.
  4. Select `RESET TO DEFAULTS`. See: all rows are back on the defaults.
  5. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] K7. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Select row `A`. Press `I`. See: `A: I`.
  2. Select row `A` again. Press `K`. Select row `B`. Press `I`. See: `A: K`, `B: I`.
  3. Select `LOAD SAVED SETTINGS`. See: no row changes.
  4. Select `RESET TO DEFAULTS`. See: `B: J`.
  5. Select `LOAD SAVED SETTINGS`. See: `B: J` stays. The saved `I` is gone.
  6. Close the game. `grep -c "KEYBOARD\." ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints `0`.
  7. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] K8. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Move to row `SELECT ACTION`. See: `NEXT WEAPON`.
  2. Press right 3 times. See: `AIR DASH`, `START`, `NEXT WEAPON`, in this order. Press left 1 time. See: `START`.
  3. Select `BACK`. Resume. Press `L`. See: the pause menu opens.
  4. In the open pause menu, go to the gear, `KEYBOARD SETTINGS`. Set `SELECT ACTION` to `AIR DASH`. Select `BACK`.
     Resume. Jump and press `L`. See: Mega air dashes.
  5. Go to pause, gear, `KEYBOARD SETTINGS`. Set `SELECT ACTION` to `NEXT WEAPON`. Select `BACK`. Resume. Press `L`. See:
     the weapon changes.
  6. Close the game. `grep -c "SELECT_BUTTON_ACTION" ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints `1`.
  7. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] K9. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`
  1. Select `SETTINGS`. Select `KEYBOARD SETTINGS`. See: the music stops, as today.
  2. Move the cursor with `WASD` and with the arrow keys. Select `BACK`. See: the main menu, and it works, as today.
- [ ] K10. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  - Go to pause, gear. Select `CONTROLLER SETTINGS`. See: the error sound, and the panel stays, as today.
- [ ] K11. `./run-game-alpha.sh --screen CONTROLLER_SETTINGS_SCREEN`
  - See: the error sound, and the game goes to the main menu, as today.

### One gamepad

Use the PS3 pad. Connect it before the launch. A swap of rows `A` and `B` works on every pad, so the checks use it: select
row `A`, then press the pad button that row `B` has.

- [ ] G1. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. With the d-pad and the default buttons: do the moves of K1. See: all as today.
  2. Push the left stick left, right, up on the ladder, and down. See: Mega moves as with the d-pad. A push of less
     than half does nothing.
  3. Hold the right arrow on the keyboard and press jump on the pad. See: both work together.
  4. Hold right on the d-pad and unplug the pad. See: Mega stops, the game does not pause, and the keyboard works.
  5. Plug the pad in. See: the d-pad, the buttons and the stick work.
- [ ] G2. `[prefs]` Replug decision. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Write down the numbers of rows `A` and `B`.
  2. Swap rows `A` and `B`. See: the 2 numbers exchange. Select `BACK`. Resume. See: jump is on the old shoot button.
  3. Unplug the pad and plug it in. See: jump is on the old jump button again (the default bindings).
  4. Close no window. In a terminal: `grep -c "GAMEPAD\.PS3 Controller\." ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"`
     prints `8`: the table stays saved.
  5. Go to pause, gear, `CONTROLLER SETTINGS`. See: the default numbers. Select `LOAD SAVED SETTINGS`. See: rows `A` and
     `B` exchange. Select `BACK`. Resume. See: jump is on the old shoot button.
  6. Unplug the pad and plug it in. See: jump stays on the old shoot button. Do it 1 more time. See: the same.
  7. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] G3. `[prefs]` New session. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Swap rows `A` and `B`. Select `LOAD SAVED SETTINGS`. Select `BACK`. Close
     the game.
  2. Run the same command again. See: jump is on the default button.
  3. Unplug the pad and plug it in. See: jump stays on the default button. A new session starts with no load.
  4. Go to pause, gear, `CONTROLLER SETTINGS`. Select `LOAD SAVED SETTINGS`. Select `BACK`. Resume. See: jump is on the
     old shoot button.
  5. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] G4. `[prefs]` No saved table. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Select `LOAD SAVED SETTINGS`. See: no row changes. Select `BACK`. Resume.
  2. Unplug the pad and plug it in. See: jump, shoot, `START` and the d-pad are on the default buttons, as in G1. No two
     rows have the same number.
  3. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] G5. `[prefs]` Triggers. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Select row `B`. See: the hint says "controller button or trigger".
  2. Pull the right trigger halfway. See: no change. Pull it fully. See: `B: R2`.
  3. Select row `A`. Pull the left trigger fully. See: `A: L2`. Select `BACK`. Resume.
  4. See: the right trigger shoots, a held right trigger charges, a release fires. The left trigger jumps.
  5. Go to pause, gear, `CONTROLLER SETTINGS`. See: the rows show `L2` and `R2`, and other rows show numbers. Select
     `LOAD SAVED SETTINGS`. Select `BACK`. Resume.
  6. Unplug the pad and plug it in. See: the triggers still work.
  7. Close the game. `grep -E "100[45]" ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints 2 lines.
  8. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] G6. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Select row `A`. Press the pad button that `A` has. See: the hint closes,
     and no row changes.
  2. Select row `A`. Press a key on the keyboard. See: the hint stays. Press the pad button of row `B`. See: the swap.
  3. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] G7. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Swap rows `A` and `B`. Move to `SELECT ACTION` and press right 1 time. See:
     `AIR DASH`.
  2. Select `RESET TO DEFAULTS`. See: the default numbers and `NEXT WEAPON`.
  3. Select `LOAD SAVED SETTINGS`. See: no row changes.
  4. `grep -c "GAMEPAD\." ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints `0`.
  5. Select `BACK`. Resume. Unplug the pad and plug it in. See: the default buttons.
  6. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] G8. `[prefs]` One `selectButtonAction`. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Set `SELECT ACTION` to `AIR DASH`. Select `BACK`.
  2. In the same panel, select `KEYBOARD SETTINGS`. See: `SELECT ACTION: AIR DASH`. Set it to `START`. Select `BACK`.
  3. Select `CONTROLLER SETTINGS`. See: `SELECT ACTION: START`.
  4. Select `LOAD SAVED SETTINGS`. See: `START` stays. Select `BACK`.
  5. Select `KEYBOARD SETTINGS`. Select `LOAD SAVED SETTINGS`. See: `START` stays. Select `BACK`.
  6. Close the game. `grep -c "SELECT_BUTTON_ACTION" ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints `1`.
  7. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] G9. `[prefs]` Last pad goes during a capture. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`
  1. Select `SETTINGS`. Select `CONTROLLER SETTINGS`. Select row `A`. The hint shows.
  2. Unplug the pad. See: the error sound, and the main menu. The keyboard moves the cursor.
  3. Plug the pad in. Select `SETTINGS`. Select `CONTROLLER SETTINGS`. See: the rows and the arrow, not the hint.
  4. Press a pad button that is not a menu button. See: no row changes (no listener stayed).
  5. Select row `A`. Press the pad button of row `B`. See: the swap, with 1 press.
  6. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`

### Two gamepads

Use the PS3 pad and the NES-style pad, on the hub. Unplug a pad at the hub. Plug in the PS3 pad first, then the
NES-style pad, so that the PS3 pad is the primary (index 0). The NES-style pad has no stick and no triggers.

- [ ] D1. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  - See: the d-pad and the buttons of each pad move Mega. Hold right on one pad and jump on the other: both work. Hold
    the same button on both pads and release it on one: Mega still sees it held.
- [ ] D2. `[prefs]` Primary pad goes during a capture. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Select row `A`. The hint shows.
  2. Unplug the PS3 pad at the hub. See: the error sound, the hint closes, and the rows and the arrow show. The screen
     stays open.
  3. Move the cursor with the NES-style pad. See: the menu works. The rows show the default numbers of that pad.
  4. Swap rows `A` and `B` on the NES-style pad. Select `BACK`. See: the settings panel of the pause menu. Resume.
  5. See: on the NES-style pad, jump is on the old shoot button.
  6. Close the game. `grep -o 'GAMEPAD\.[^"]*\.A"' ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints 1 line,
     with the name of the NES-style pad.
  7. Plug in the PS3 pad. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] D3. `[prefs]` Other pad goes during a capture. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Select row `A`. The hint shows.
  2. Unplug the NES-style pad at the hub. See: no error sound, and the hint stays.
  3. Press the PS3 button of row `B`. See: the swap. Select `BACK`. Resume. See: the PS3 pad and the keyboard work, and
     the game does not pause.
  4. Close the game. Plug in the NES-style pad. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] D4. `[prefs]` `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. See: the numbers of the PS3 pad.
  2. Select row `A`. Press a button on the NES-style pad. See: the hint stays.
  3. Press the PS3 button of row `B`. See: the swap. Select `BACK`. Resume.
  4. See: jump is on the old shoot button of the PS3 pad, and the same numbers apply to the NES-style pad (one code set).
  5. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] D5. `[prefs]` One table per pad name. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Swap rows `A` and `B` on the PS3 pad. Select `BACK`. Resume.
  2. Unplug the PS3 pad at the hub. Go to pause, gear, `CONTROLLER SETTINGS` with the NES-style pad.
  3. Select row `START`. Press the NES-style button of row `SELECT`. See: the swap. Select `LOAD SAVED SETTINGS`. Select
     `BACK`. Resume.
  4. Plug in the PS3 pad. Then unplug the NES-style pad. The PS3 pad is now the primary.
  5. See: on the PS3 pad, jump is on the old shoot button, and `START` and `SELECT` are on their default buttons. This
     is the table of the PS3 pad, not the table of the NES-style pad.
  6. Close the game. `grep -c "GAMEPAD\." ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints `16`.
  7. Plug in the NES-style pad. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] D6. `[prefs]` A pad name with no table, after the load. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Swap rows `A` and `B` on the PS3 pad. Select `LOAD SAVED SETTINGS`. Select
     `BACK`. Resume.
  2. Unplug the PS3 pad at the hub. See: on the NES-style pad, jump, shoot, `START` and the d-pad are on the default
     buttons. It does not get the PS3 table.
  3. Close the game. Plug in the PS3 pad. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] D7. `[prefs]` Index shift, no load. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Swap rows `A` and `B` on the PS3 pad. Select `BACK`. Resume.
  2. Unplug the PS3 pad at the hub. See: on the NES-style pad, jump is on the default button.
  3. Close the game. Plug in the PS3 pad. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] D8. `[prefs]` Both pads go at one time. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `CONTROLLER SETTINGS`. Select row `A`. The hint shows.
  2. Pull the hub from the computer, so that both pads go at one time. If the hub is not free to move, unplug both pads at
     the hub, as near to the same time as you can.
  3. See: the error sound, no crash, and the settings panel of the pause menu.
  4. Resume with the keyboard. See: the level is the same, and the keyboard works.
  5. Plug in the hub. See: both pads give input.
  6. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`

### Back to pause (the largest risk)

Connect a pad only if the check says so. These checks write no prefs, except where marked.

- [ ] H1. From the keyboard screen, no pad connected. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Let a `SniperJoe` shot or a `Met` shot hit Mega once, so that the health bar is below full.
  2. Press `Enter` while a shot is in flight and while the `SniperJoe` is in the open. See: the pause sound, 1 time.
     Note Mega's position, the health bar, the shot and the enemies.
  3. Select the gear. Select `KEYBOARD SETTINGS`. See: no pause sound. The level music plays with no break. The rows of
     the bindings screen are in the right place, as today.
  4. Stay on the screen for 10 seconds. Move the cursor. See: the menu sounds play, and the music plays with no restart.
  5. Move the cursor to `BACK` and select it. See: the settings panel of the pause menu, on the same row, in the right
     place. No pause sound. No `READY`, and no level start.
  6. Select the panel `BACK`. See: the pause grid, with the same bars. The cursor moves.
  7. Move the cursor to a weapon and select it. See: the pause sound, 1 time.
  8. See: Mega, the health bar, the shot and the enemies are where they were at step 2. The shot continues from that
     place. `SniperJoe` and `Met` go on with their pattern. The music did not restart. The controls work at once.
- [ ] H2. From the gamepad screen, PS3 pad connected. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Do H1 step 1 and step 2 with the pad.
  2. Select the gear. Select `CONTROLLER SETTINGS`. See: the results of H1 step 3.
  3. Do H1 step 4 to step 8 with the pad. See: the same results as H1.
- [ ] H3. Real stage, not the first room. `./run-game-alpha.sh --level INTRO_STAGE --room room3`
  1. Climb the ladder that goes up into the room above. Wait for the room transition to end.
  2. Go to pause, gear, `KEYBOARD SETTINGS`. Stay for 5 seconds. Select `BACK`. Resume.
  3. See: Mega is in the same room, at the same place. The stage did not restart, and the music did not restart.
- [ ] H4. Death after back. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Select `BACK`. Resume.
  2. Fall into the pit. See: Mega dies, 1 life goes, and Mega respawns in this room, with the controls working.
  3. Go to pause, gear, `KEYBOARD SETTINGS`. Select `BACK`. Resume. See: the same state, and the controls work.
- [ ] H5. Last pad goes on the screen, PS3 pad connected. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Note Mega's position. Go to pause, gear, `CONTROLLER SETTINGS`.
  2. Unplug the pad. See: the error sound, then the settings panel of the pause menu, in the right place.
  3. Resume with the keyboard. See: Mega is at the same position, and the level is the same.
  4. Plug in the pad. See: the pad gives input.
- [ ] H6. Last pad goes during a capture, PS3 pad connected. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Note Mega's position. Go to pause, gear, `CONTROLLER SETTINGS`. Select row `A`. The hint shows.
  2. Unplug the pad. See: the error sound, then the settings panel of the pause menu.
  3. Plug in the pad. Select `CONTROLLER SETTINGS`. See: the rows and the arrow, not the hint.
  4. Press a pad button that is not a menu button. See: no row changes. Select `BACK`. Resume. See: the level is the same.
- [ ] H7. Resize. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Change the window size. See: the rows stay readable.
  2. Select `BACK`. See: the settings panel fits the new size, in the right place. Resume. See: the level and the
     backgrounds fit the new size.
- [ ] H8. Focus. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim --pauseOnFocusLost --resumeOnFocusGained`
  1. Go to pause, gear, `KEYBOARD SETTINGS`.
  2. Click another window, then click the game window. See: no pause sound, and the bindings screen stays.
  3. Select `BACK`. See: the settings panel of the pause menu. The level does not move. Select the panel `BACK`. See: the
     pause grid.
  4. Click another window, then click the game window. See: the game resumes, as today with these flags. The guard is
     off after `BACK`.
  5. Click another window. See: the game pauses. Click the game window. See: it resumes.
- [ ] H9. Repeat. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Select `BACK`. Do it 3 times in one pause.
  2. Connect the PS3 pad. Select `CONTROLLER SETTINGS`. Select `BACK`. Select `KEYBOARD SETTINGS`. Select `BACK`.
  3. Resume. Pause again. Go to the gear, `KEYBOARD SETTINGS`. Select `BACK`. Resume.
  4. See at each step: the right screen, in the right place. The music did not restart. The level did not move.
- [ ] H10. Close. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_aim`
  - Go to pause, gear, `KEYBOARD SETTINGS`. Close the game window. See: `BUILD SUCCESSFUL`, and no exception in the
    console or in `assets/logs.txt`.

### Boot

No gamepad connected, except where marked.

- [ ] B1. `[prefs]` Clean start. Remove the folder: `test -d ~/.prefs.bak-031e && rm -rf ~/.prefs`
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. See: no exception. `WASD`, `K`, `J`, `Enter`, `L` work.
  2. Go to pause, gear, `KEYBOARD SETTINGS`. See: the default rows, and `SELECT ACTION: NEXT WEAPON`.
  3. Select `LOAD SAVED SETTINGS`. See: no row changes, and no error.
  4. Select row `A`. Press `I`. See: `A: I`.
  5. Close the game. `ls ~/.prefs` lists `Megaman Maverick - Player Settings - Slot 0`.
  6. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] B2. `[prefs]` Old prefs files are ignored. The PS3 pad is connected. The old keyboard file has `S`, `X`, `Z`, `C` on the
      d-pad, and the run script passes `--loadKeyboardPrefs`.
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. See: `A` and `D` run. `Z` and `C` do nothing.
  2. Go to pause, gear, `KEYBOARD SETTINGS`. Select `LOAD SAVED SETTINGS`. See: `UP: W`, `LEFT: A`, not the old keys.
     Select `BACK`.
  3. Select `CONTROLLER SETTINGS`. Select `LOAD SAVED SETTINGS`. See: rows `A`, `B` and `SELECT` do not show `0`, `2` and `1`.
     Select `BACK`. Resume.
  4. Close the game. `diff -r ~/.prefs ~/.prefs.bak-031e` prints nothing: no file changed, and no new file exists.
  5. Run the same command again. Go to pause, gear, `KEYBOARD SETTINGS`. Select row `A`. Press `I`. Close the game.
  6. `diff -r ~/.prefs ~/.prefs.bak-031e` prints 1 line: `Only in`, with the name `Megaman Maverick - Player Settings - Slot 0`. The old files
     did not change.
  7. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] B3. `[prefs]` Saved keys and `--loadKeyboardPrefs`.
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Select row `A`. Press `I`. Close the game.
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  2. See: `I` jumps. The flag loads the new file.
  3. Close the game.
  - `./gradlew lwjgl3:run --args="--level REFACTOR_TEST_029 --room movement --pixelPerfect"`
  4. See: `K` jumps, and `I` does nothing. Without the flag, boot uses the defaults.
  5. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] B4. `[prefs]` `selectButtonAction` at boot.
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Set `SELECT ACTION` to `AIR DASH`. Close the game.
  - `./gradlew lwjgl3:run --args="--level REFACTOR_TEST_029 --room movement --pixelPerfect"`
  2. Jump and press `L`. See: Mega air dashes, with no flag and no "LOAD SAVED SETTINGS". `K` jumps (the default keys).
  3. Go to pause, gear, `KEYBOARD SETTINGS`. See: `SELECT ACTION: AIR DASH`. Close the game.
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  4. Jump and press `L`. See: Mega air dashes.
  5. Close the game. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] B5. `[prefs]` `--resetBindings` with `--loadKeyboardPrefs`. The PS3 pad is connected.
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  1. Go to pause, gear, `KEYBOARD SETTINGS`. Select row `A`. Press `I`. Set `SELECT ACTION` to `AIR DASH`. Select `BACK`.
  2. Select `CONTROLLER SETTINGS`. Swap rows `A` and `B`. Close the game.
  - `./run-game-alpha.sh --resetBindings --level REFACTOR_TEST_029 --room movement`
  3. See: `K` jumps. The run script passes `--loadKeyboardPrefs`, and the defaults still win.
  4. Go to pause, gear, `KEYBOARD SETTINGS`. See: `A: K`, `SELECT ACTION: NEXT WEAPON`. Select `LOAD SAVED SETTINGS`. See: no
     row changes. Select `BACK`.
  5. Select `CONTROLLER SETTINGS`. Select `LOAD SAVED SETTINGS`. See: no row changes. Close the game.
  6. `grep -c "<entry" ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints `0`.
  7. `diff -r ~/.prefs ~/.prefs.bak-031e` prints 1 line: `Only in`, with the name of the new file. The old files stay.
  8. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] B6. `[prefs]` `--resetBindings` with no flag, and with no file.
  - `./gradlew lwjgl3:run --args="--resetBindings --level REFACTOR_TEST_029 --room movement --pixelPerfect"`
  1. See: no exception, and the default keys. There was no file at the start. Close the game.
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  2. Go to pause, gear, `KEYBOARD SETTINGS`. Select row `A`. Press `I`. Close the game.
  - `./gradlew lwjgl3:run --args="--resetBindings --level REFACTOR_TEST_029 --room movement --pixelPerfect"`
  3. See: `K` jumps. Close the game.
  4. `grep -c "<entry" ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"` prints `0`.
  5. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`
- [ ] B7. `[prefs]` Bad files. Do each edit with the game closed.
  1. Broken XML: `printf 'not xml' > ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"`
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  2. See: a stack trace in the console, the game starts, and the default keys work. Close the game.
  3. A key code out of range:
     `printf '<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"><properties><entry key="KEYBOARD.A">999</entry></properties>' > ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"`
  - `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`
  4. Go to pause, gear, `KEYBOARD SETTINGS`. See: `A: 999`, and no crash. Close the game.
  5. A value that is not a number:
     `printf '<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"><properties><entry key="KEYBOARD.A">abc</entry></properties>' > ~/.prefs/"Megaman Maverick - Player Settings - Slot 0"`
  - `./run-game-alpha.sh --resetBindings --level REFACTOR_TEST_029 --room movement`
  6. See: the game starts with the default keys. This is the last-resort path. Close the game.
  7. Restore: `rm -rf ~/.prefs && cp -a ~/.prefs.bak-031e ~/.prefs`

### Last step

- [ ] X1. Close the game. Restore the start state and remove the copy:
      `rm -rf ~/.prefs && mv ~/.prefs.bak-031e ~/.prefs`
      Then `ls ~/.prefs | grep -c "Player Settings"` prints `0`.

## Open questions

1. **The state of `~/.prefs`.** Answered (owner, 2026-10-30, verbatim): "I deleted the prefs, so nothing needs to be
   backed up". So S1 (restore the `031d` backup) is removed. `~/.prefs.bak-031d` is left as it is.
2. **Two test seams.** T21 to T26 need the boot order, and T43 to T46 need the labels. `docs/SCOPE.md` has the boot order
   inside `create()`, and has the label function private in `ControllerSettingsScreen`. Neither can run in a test there.
   The label function matters: the `Input.Keys.toString` case throws from the render of the screen.
   - a) One function for the boot order, called from `create()`, next to `PlayerSettingsStore`. The label function goes
     next to the capture decision function. Both have a production caller. This adds 1 line to the table of new APIs.
   - b) Keep the scope as it is. T21 to T26 and T43 to T46 go, and only B3 to B7, K5, K6 and G5 cover them. The minimum of
     A3 falls by 10.
   - Recommendation: a).
3. **A saved value that is not a number.** `getInteger` throws, as today (see "Where a real library differs from a mock").
   With the run scripts, the flag `--loadKeyboardPrefs` reads the keys at boot, so the game does not start until
   `--resetBindings` runs. The scope says that `--resetBindings` is the answer to a hand-edited file.
   - a) Keep it. B7 proves the way out. The plan adds a `TODO` at the read in `PlayerSettingsStore` that names this edge
     case, as the owner's rule for an accepted edge case says.
   - b) Treat such a value as a missing key. This adds a case that the scope does not list.
   - Recommendation: a).
4. **2 text fixes in `docs/SCOPE.md`.** Neither changes an owner's answer.
   - Section 3 says that `--loadKeyboardPrefs` reads `SELECT_BUTTON_ACTION`. Question 6 moves that read out of the flag
     block. This contract follows question 6 (T21, B4).
   - "Affected areas and risks" says that `Input.Keys.toString` can return `null` for an unknown code. It also throws out
     of `0..255` (T44).
   - Recommendation: correct the 2 lines when this contract is approved, and write it in the process log.
5. **Root `CLAUDE.md`.** It names `players.selectButtonAction`, and its list of launcher flags has no `--resetBindings`. On
   a branch, docs stay in the branch folder.
   - Recommendation: the merge step of `docs/PLAN.md` updates these lines on `master`.

Questions 2 to 5: answered (owner, 2026-10-30, verbatim): "contract approved", as recommended.
