# Plan: mega-refactor-031e-bindings

How to do the work in `SCOPE.md`. This was written ahead of time on `master` (`6ee5859e6`), before the branch existed.
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
- One test case per `T` ID of the contract. Each step gives the core count that it must reach. The counts assume the
  baseline `301`. If the baseline changes in step 2, add the difference to each count.
- Test-driven order where it fits. Kotest here does not accept the Gradle `--tests` filter, so each test-first run is
  a full `core:test`. A test does not compile against an API that does not exist. So first add the signature with an
  empty body or today's rule, run the tests, and see the new tests fail. Then write the code. Do not commit that first
  body. Write in the process log which tests failed before the code. The lists below are predictions (`031d` had 2
  wrong predictions); a difference goes in the log.
- No code comments. The only exception is a `TODO` that names an accepted edge case: when it happens, and where the
  follow-up is tracked. Step 5 adds the one `TODO` that this plan knows of.
- Write code only when production code calls it. A test alone is not a caller. Each new API below has its first
  production caller in the same step.
- Smoke launches are about 15 to 20 seconds, and always after a compile. Lesson from `031d`: a launch with `timeout 20`
  ended during the Gradle compile, so the game did not start. So run these 2 commands from the worktree:
  - `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew lwjgl3:classes`
  - `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 timeout 20 ./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`

  A clean exit is a pass; do not investigate a clean exit. An exit by `timeout` with no exception in the console or in
  `assets/logs.txt` is also a pass. The agent cannot press keys in the game, so a smoke launch never drives a menu.
- Never run a bare `git stash`. In the main checkout, stage only named paths (`git add <path> ...`), never `git add -A`
  or `git add .`. Do the same in the worktree: `build.log` and `build-baseline-warnings.txt` are not git-ignored
  (`git check-ignore` gives no output), and they are not committed.
- On the branch, write docs only inside the branch folder.
- Only the paths in contract checks A6, A7 and A8 change.

### `~/.prefs`

The agent must never read, write or remove anything in `~/.prefs` or `~/.prefs.bak-*`. Only the owner runs the commands
that touch these folders (contract S2, S3, the restores, B1 and X1).

The game process itself can touch `~/.prefs`. These facts were checked on `6ee5859e6`:

- **Writes on `master`:** a `grep` for `.flush()` in `core/src/main`, `engine/src/main` and `lwjgl3/src` gives only 4
  writes of a prefs file: `ControllerUtilsExtensions.kt:55` and `:64` (a rebind), `:100` (the select row), and
  `MegamanMaverickGame.kt:846` (`saveState`, whose only caller is `SaveGameScreen.kt:75`). The other 2 `flush()` lines are
  the diagnostics file writer in `RuntimeDiagnostics.kt`. No `dispose()` path saves. So a `--level` launch with no input
  writes nothing to `~/.prefs`.
- **Writes on this branch:** `saveKeyboard`, `saveGamepad`, `saveSelectButtonAction`, `clearKeyboard` and `clearGamepad` run
  only from a menu action on the bindings screen, so they need input. A launch of `--screen KEYBOARD_SETTINGS_SCREEN` with
  no input saves nothing. `clear()` runs at boot with `--resetBindings`, and it writes the file. So **no smoke launch
  passes `--resetBindings`**.
- **Reads:** all 3 run scripts pass `--loadKeyboardPrefs` (`run-game-alpha.sh:3`), so a smoke launch through a script makes
  the game process open a prefs file. From step 8, every launch opens the slot file, with or without that flag, because
  the saved `selectButtonAction` applies at boot. `Lwjgl3Preferences` only reads on open, and it creates no file. This is
  the game's own read: no agent command opens these folders.
- **Tests:** `core:test` uses a fake behind a mock `Gdx.app`, so it does not touch `~/.prefs`.

### Paths

`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`,
`T/` = `core/src/test/kotlin/com/megaman/maverick/game/`, `L/` = `lwjgl3/src/main/kotlin/com/megaman/maverick/game/lwjgl3/`,
`CSS` = `C/screens/menus/ControllerSettingsScreen.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`.

### New APIs and their first production callers

| API | Step | First production callers |
|---|---|---|
| `PlayerSettings` with `selectButtonAction` (`C/players/PlayerSettings.kt`) | 3 | `ControllerComponent`, `AirDashBehavior` (2 lines), `MLS:926`, `CSS` |
| `MegaPlayers.settings(slot)` | 3 | `MegamanMaverickGame.create()`, `postCreate()`, `ControllerComponent`, `AirDashBehavior`, `MLS:926` |
| `PlayerSettingsStore(slot)` (`C/players/PlayerSettingsStore.kt`) | 4 | `create()`, `CSS` |
| `PreferenceFiles.MEGAMAN_MAVERICK_PLAYER_SETTINGS` | 4 | `PlayerSettingsStore` |
| `saveSelectButtonAction(settings)`, `loadSelectButtonAction()` | 4 | `CSS` select row and load row; `create()` |
| `PlayerSettings.keyCodes` (`IntArray` by `button.index`) | 5 | `KeyboardInput.held()`, `CSS` |
| `saveKeyboard(settings)`, `loadKeyboard(settings)` | 5 | `CSS` key capture; `CSS` load row and `create()` |
| `PlayerSettings.padCodes`, `PlayerSettings.NO_CODE` | 6 | `GamepadInput.held()`, `CSS` |
| `saveGamepad(padName, settings)`, `loadGamepad(padName, settings)`, `readGamepadCode(padName, button)` | 6 | `CSS` pad capture; `CSS` load row; `GamepadInput.held()` on a change of primary |
| `applyBootSettings(...)` (next to `PlayerSettingsStore`) | 8 | `create()` |
| `clear()`, `--resetBindings`, `params.resetBindings` | 9 | `applyBootSettings`; `create()` |
| `clearKeyboard()`, `clearGamepad(padName)` | 10 | `CSS` reset row |
| `KeyboardInput.isReservedKey(keycode)` | 11 | `PlayerSettings.captureKey` |
| `PlayerSettings.captureKey(button, keycode)`, `enum KeyCapture { ACCEPT, KEEP, REFUSE }` | 11 | `CSS` key capture |
| `keyLabel(code)`, `padLabel(code)` (next to `captureKey`) | 13 | `CSS` row text |
| Private `endCapture()`, private `capturePad` in `CSS` | 14 | `CSS` |
| `showOverPause(key, props)`, `closeOverPause()`, private held-level fields, on `MegamanMaverickGame` | 15 | `GameSettingsPanel` (2 sites); the back action (2 sites) |

`T/players/FakePreferences.kt` is test code. It is the fake that the contract describes (a real `java.util.Properties`,
memory changes only until `flush()`, reads with `Integer.parseInt`, writes and reads with `storeToXML` and
`loadFromXML`), with a helper that puts it behind a mock `Gdx.app` and records each opened name (T8).

Removed: `E/controller/buttons/ControllerButton.kt`, `E/controller/buttons/ControllerButtons.kt`,
`C/controllers/ControllerUtilsExtensions.kt`, `T/controllers/ControllerUtilsExtensionsTest.kt`, `game.buttons`,
`MegaPlayers.selectButtonAction`, the 2 old `PreferenceFiles` names, the `TODO` at `C/input/KeyboardInput.kt:11`.

## Steps

### 1. Branch start

- Changes: in the main checkout, on `master`, one docs-only commit. Stage only these 4 paths:
  - this `docs/PLAN.md`: `- Status: approved` and `- Approved: <date>`;
  - `docs/mega-refactor/ITEMS.md`: the `mega-refactor-031e-bindings` row to `active`, with the start date;
  - `docs/mega-refactor/items/031-input-frames/ITEM.md`: in the "Branches" line, `mega-refactor-031e-bindings`
    (planned) becomes (active);
  - the branch `process-log.md`: state `active`, a dated entry for the start.

  Item `031` merges each branch directly into `master` (squash), as `031a` to `031d` did. Then, from the main checkout:
  `git worktree add -b mega-refactor-031e-bindings .worktrees/mega-refactor-031e-bindings master`.
- Check: `git worktree list` shows `.worktrees/mega-refactor-031e-bindings` on `mega-refactor-031e-bindings`. `git status`
  in the main checkout shows no staged change left.
- Contract checks: none.
- Done: yes

### 2. Baseline

- Changes: none in the code. In the worktree, run the build command. Read the folders of the other active branches
  again (`git worktree list`; `051b` was active at the scan), and write the result in `docs/NOTES.md`.
- Check: the build passes. Write in the process log the engine and core test counts, the failures, and the number of
  unique warning lines. Keep the sorted list next to `build.log`:
  `grep '^w:' build.log | sed -E 's/:[0-9]+:[0-9]+ / /' | sort -u > build-baseline-warnings.txt`. The contract says "plan
  step 1" for this list (lines 62 and 76); in this plan it is step 2.

  The contract baseline on `87a7b648c` is engine 575, core 301, 0 failures, 76 unique warning lines. `master` at
  `6ee5859e6` has only docs commits after it. If `master` moved and a number differs, write the new baseline and the
  reason in the process log, and use it for A2 to A4 and for the counts below. If `051b` merged first, the line numbers
  in `MegamanMaverickGame.kt` and `MLS` in this plan are old: find each place by its content.
- Commit: none. The log entry and `docs/NOTES.md` go in the commit of step 3.
- Contract checks: baseline for A1 to A4.
- Done: yes

### 3. `PlayerSettings` holds `selectButtonAction`

The first storage step moves the value with the fewest readers, so that `PlayerSettings` has callers at once. The
save still goes to the old files.

- Changes:
  - New `C/players/PlayerSettings.kt`: `class PlayerSettings { var selectButtonAction = SelectButtonAction.NEXT_WEAPON }`.
    No other field yet.
  - `C/players/MegaPlayers.kt`: remove `selectButtonAction` (`:18`). Add one `PlayerSettings` per slot, built from
    `capacity`, as `PlayerInputs` does, and `fun settings(slot: PlayerSlot)`.
  - Readers:
    - `C/entities/megaman/components/ControllerComponent.kt:99` and `C/entities/megaman/behaviors/AirDashBehavior.kt:33`,
      `:38`: `services.players.settings(playerSlot!!).selectButtonAction`.
    - `MLS:926`: `game.players.settings(player.playerSlot!!).selectButtonAction`.
    - `C/MegamanMaverickGame.kt:306`: through `players.settings(PlayerSlot(0))`.
    - `CSS`: a new constructor parameter `settings: PlayerSettings`, passed at `:453` and `:454` of the game. The 7 lines
      `:224`, `:252`, `:323`, `:345`, `:346`, `:351`, `:352` read it.
  - Tests: the `MegaPlayersTest` case moves to `settings(slot)` (T55). New T56. This is a move, so no test-first run for
    T55; T56 needs the new function.
- Check: the build command passes. Core 302. `grep -rn 'players\.selectButtonAction' core/src lwjgl3/src` gives no output.
  `grep -n 'capacity' C/players/MegaPlayers.kt` shows the default `1`. Smoke launch.
- Commit: `031e: PlayerSettings holds selectButtonAction, one per slot`
- Contract checks: T55, T56, R8. Manual proof later: K8.
- Done: yes

### 4. `PlayerSettingsStore` and the new file, for `selectButtonAction`

- Changes:
  - `C/Constants.kt`: add `MEGAMAN_MAVERICK_PLAYER_SETTINGS = "Megaman Maverick - Player Settings"`. The 2 old names stay
    until step 5 and step 7.
  - New `C/players/PlayerSettingsStore.kt`: `class PlayerSettingsStore(slot: PlayerSlot)`. It does not open the file in
    its constructor. Each function gets `Gdx.app.getPreferences("<name> - Slot <index>")`; the library keeps one object
    per name. Add `saveSelectButtonAction(settings)` (`putString`, `flush`) and `loadSelectButtonAction(): SelectButtonAction?`
    (`null` for a missing key or an unknown name). The key stays `SELECT_BUTTON_ACTION`.
  - `C/MegamanMaverickGame.kt`: a private field `settingsStore`, built in `create()` with `PlayerSlot(0)` before the
    `--loadKeyboardPrefs` block. `:306` calls the store. This read stays in the flag block until step 8.
  - `CSS`: a new constructor parameter for the store. The load row (`:224`) and the select row (`:346`, `:352`) use it.
    Both rows now use one value, for both screens.
  - `C/controllers/ControllerUtilsExtensions.kt`: remove `SELECT_BUTTON_ACTION_KEY`, `selectButtonActionPrefs`,
    `saveSelectButtonActionToPrefs` and `loadSelectButtonActionFromPrefs`.
  - Test first: new `T/players/FakePreferences.kt` and `T/players/PlayerSettingsStoreTest.kt` with T7, T12, T13, T14, T15.
    T15 calls `saveSelectButtonAction` 2 times and counts the key; the screen half is G8. Prediction with an empty
    save: T7, T12 and T15 fail; T13 and T14 pass.
- Check: the build command passes. Core 307. Smoke launch.
- Commit: `031e: PlayerSettingsStore saves selectButtonAction to one file per slot`
- Contract checks: T7, T12 to T15; intended change 2. Manual proof later: G8, K8.
- Done: yes

### 5. Keyboard codes move to `PlayerSettings`

- Changes:
  - `PlayerSettings`: `val keyCodes = IntArray(MegaControllerButton.entries.size)`, filled with `defaultKeyboardKey` by
    `button.index`.
  - `PlayerSettingsStore`: `saveKeyboard(settings)` (8 keys `KEYBOARD.<BUTTON>`, 1 `flush`), `loadKeyboard(settings)` (a
    missing key keeps the current code), and a private `readCode(key): Int?` (`contains`, then `getInteger(key)`).
  - The `TODO` at `readCode`, for contract question 3. It also names the same risk of a hand-edited file that the
    `KeyboardInput` `TODO` named, because that `TODO` goes in step 12 and such a file still loads as written
    (`docs/SCOPE.md`, "Bindings on a hand-edited file"). For example:
    `// TODO: a saved code that is not a number throws here, as on master, so with --loadKeyboardPrefs the game does not start until --resetBindings runs; a hand-edited duplicate or reserved key loads as written and an alias then gives 2 buttons; follow-up in item 034 (see 031e CONTRACT.md, question 3)`
  - `C/input/KeyboardInput.kt`: the constructor takes `PlayerSettings`. `held()` is an index loop over `keyCodes`, with no
    map lookup. The aliases do not change.
  - `C/MegamanMaverickGame.kt`: `KeyboardInput(players.settings(PlayerSlot(0)))`. `:305` calls `loadKeyboard`.
  - `CSS`: the key capture swap and save (`:74-91`), the keyboard branch of the load row (`:195`), the keyboard branch of the
    reset row, and the keyboard row text use `keyCodes` and the store. The reset row sets `keyCodes` from
    `defaultKeyboardKey` in an inline loop; no new API.
  - `C/Constants.kt`: remove `MEGAMAN_MAVERICK_KEYBOARD_PREFERENCES`.
  - `C/controllers/ControllerUtilsExtensions.kt`: remove `getKeyboardPreferences`, `loadKeyboardSettingsFromPrefs` and the
    keyboard branches. `loadControllerButtons` stays until step 6 and still fills the unused `keyboardCode`.
  - Test first: T3, T9 in `PlayerSettingsStoreTest`. The 4 lines of `KeyboardInputTest` move to `PlayerSettings` (10 cases,
    same meaning). Prediction with empty bodies: T3 and T9 fail.
- Check: the build command passes. Core 309. `grep -n 'Keyboard Preferences' C/Constants.kt` gives no output. Smoke launch.
- Commit: `031e: keyboard codes move to PlayerSettings and the slot file`
- Contract checks: T3, T9; intended change 1 (keyboard half). Manual proof later: K1, K2, K5, B1, B2, B3.
- Done: yes

### 6. Gamepad codes move to `PlayerSettings`

- Changes:
  - `PlayerSettings`: `val padCodes = IntArray(...)` filled with `NO_CODE`. `NO_CODE = Int.MIN_VALUE`, so that it differs
    from `ControllerMapping.UNDEFINED` (`-1`), which a mapping can give and which today reaches `getButton`.
  - `PlayerSettingsStore`: `saveGamepad(padName, settings)` (8 keys `GAMEPAD.<pad name>.<BUTTON>`, 1 `flush`),
    `loadGamepad(padName, settings)` (a missing key keeps the current code; a pad name with no table changes no code),
    `readGamepadCode(padName, button): Int?` (through `readCode`).
  - `C/input/GamepadInput.kt`: the constructor takes `PlayerSettings` and the store. On a change of primary: each code
    comes from `readGamepadCode` if `loadPrefsOnConnect` is `true`, else or on `null` from `first.mapping.getMapping(button)`.
    With no pad, every code goes to `NO_CODE`. `held()` is an index loop over `padCodes`, and a `NO_CODE` code is skipped.
    `loadPrefsOnConnect` stays here, starts `false`, and is not saved. The stick, the triggers and `padsOfTick` do not
    change.
  - `CSS`: the pad capture (`setControllerCode`, the swap on `padCodes`), the gamepad branch of the load row (`:204-219`, with
    `controller!!.name`), the gamepad branch of the reset row (`mapping.getMapping` in an inline loop), and the row text use
    the new storage. The pad capture keeps today's rule: a press of the code that the button already has still saves.
    The constructor loses `controllerButtons`.
  - `C/MegamanMaverickGame.kt`: remove `buttons` (`:216`), `:303`, and the import. `GamepadInput(...)` and the 2 screens
    at `:453`, `:454` take the settings and the store.
  - Tests: T1, T2, T4, T5, T6, T10, T11, T20 in `PlayerSettingsStoreTest`; T47 to T52 in `GamepadInputTest`. The 23
    `GamepadInputTest` cases move to `PlayerSettings` and the fake (about 120 lines). The `mockkStatic` of
    `ControllerUtilsExtensionsKt` and each `getPreferredControllerCode` stub go. Prediction with empty bodies: T1, T48
    and T52 pass; the others fail. Write the actual list.
- Check: the build command passes. Core 323: 309 + 14. `grep -n 'loadPrefsOnConnect' C/players/PlayerSettings.kt` gives no
  output. Smoke launch.
- Commit: `031e: gamepad codes move to PlayerSettings and the slot file; game.buttons goes`
- Contract checks: T1, T2, T4 to T6, T10, T11, T20, T47 to T52, R6; intended change 1 (gamepad half). Manual proof
  later: G1 to G4, G6, D1, D4 to D7.
- Done: yes

### 7. Delete the old storage

- Changes: delete `E/controller/buttons/ControllerButton.kt`, `E/controller/buttons/ControllerButtons.kt`,
  `C/controllers/ControllerUtilsExtensions.kt` and `T/controllers/ControllerUtilsExtensionsTest.kt` (its 2 cases are now T1
  and T2). Remove `MEGAMAN_MAVERICK_CONTROLLER_PREFERENCES` from `C/Constants.kt`.
- Check: the build command passes. Engine 575. Core 321: 323 - 2. A6, A9, A10, A12 and A13 pass as the contract writes
  them. Smoke launch.
- Commit: `031e: delete ControllerButtons, ControllerButton and ControllerUtilsExtensions`
- Contract checks: A6, A9, A10, A12, A13; the `game.buttons` and `loadControllerButtons` halves of A14.
- Done: yes

### 8. The saved `selectButtonAction` applies at boot

- Changes:
  - `C/players/PlayerSettingsStore.kt`: a top-level `applyBootSettings(store, settings, loadKeyboardPrefs)`. It always
    applies `loadSelectButtonAction()` if it is not `null`, and calls `loadKeyboard` only with the flag. It never reads a
    gamepad table.
  - `C/MegamanMaverickGame.kt`: `create()` calls it in place of the block at `:304-308`. The log line stays, only with the
    flag.
  - Test first: T21, T22, T25, T26 in `PlayerSettingsStoreTest`. Prediction with today's rule (the select read only with
    the flag): T21 fails; T22, T25 and T26 pass. T25 and T26 get their `--resetBindings` combinations in step 9, because
    that parameter does not exist here.
- Check: the build command passes. Core 325. Smoke launch.
- Commit: `031e: the saved selectButtonAction applies at boot`
- Contract checks: T21, T22, T25, T26, the read half of R5; intended change 12. Manual proof later: B3, B4.
- Done: yes

### 9. `--resetBindings`

- Changes:
  - `L/DesktopLauncher.kt`: `DEFAULT_RESET_BINDINGS = false`, a `@Parameter(names = ["--resetBindings"], ...)` next to
    `--loadKeyboardPrefs`, whose description says that it clears the saved bindings at every launch that passes it; one
    `println` next to `:108`; `params.resetBindings = appArgs.resetBindings` next to `:157`. It is not in `LaunchOptions`.
  - `C/MegamanMaverickGame.kt`: `var resetBindings: Boolean = false` next to `:148`. `create()` passes it, and logs one line
    when it is `true`. `MegaPlayers.capacity` is `1`, so there is one store and one call; `047` adds a loop.
  - `PlayerSettingsStore`: `clear()` (`Preferences.clear()`, `flush()`). `applyBootSettings` gets `resetBindings` and calls
    `clear()` first, before every read. The old files are not touched.
  - Test first: T16, T17, T23, T24 in `PlayerSettingsStoreTest`; T54 in `GamepadInputTest`. Add the `--resetBindings`
    combinations to T25 and T26. Prediction with an empty `clear()`: T16, T23, T24 and T54 fail; T17 passes.
- Check: the build command passes. Core 330. Smoke launch, with no `--resetBindings` (see "`~/.prefs`"). T23 and T24 prove
  the order in the agent's checks, and the owner proves the flag in B5 to B7.
- Commit: `031e: --resetBindings clears the slot file before any read`
- Contract checks: T16, T17, T23, T24, T54, R5; intended change 10. Manual proof later: B5, B6, B7.
- Done: yes

### 10. "RESET TO DEFAULTS" clears the saved values

- Changes:
  - `PlayerSettingsStore`: `clearKeyboard()` removes the 8 `KEYBOARD.<BUTTON>` keys and `SELECT_BUTTON_ACTION`, then
    flushes. `clearGamepad(padName)` removes the 8 keys of that pad name and `SELECT_BUTTON_ACTION`, then flushes.
  - `CSS` reset row: after the in-memory reset, it calls the function of that screen. On the gamepad screen it needs a pad;
    with no pad, the row changes nothing, as `resetSettingsToDefaults` did. Wrap the call as the capture save is
    wrapped (`GameLogger.error` on an exception).
  - `loadPrefsOnConnect` does not change, as today. After a reset the table is gone, so a replug gives the mapping (T53).
  - Test first: T18, T19 and T8 in `PlayerSettingsStoreTest`; T53 in `GamepadInputTest`. T8 goes here because this step adds
    the last store functions. Prediction with empty bodies: T18, T19 and T53 fail; T8 passes.
- Check: the build command passes. Core 334. Smoke launch.
- Commit: `031e: RESET TO DEFAULTS also clears the saved table and the select action`
- Contract checks: T8, T18, T19, T53; intended change 11. Manual proof later: K4, K6, K7, G7.
- Done: yes

### 11. Reserved keys in a key capture

- Changes:
  - `C/input/KeyboardInput.kt`: `isReservedKey(keycode)` in the companion: true for `Esc` and each key in `ALIAS_KEYS`, so
    the list has one source.
  - `C/players/PlayerSettings.kt`: `enum class KeyCapture { ACCEPT, KEEP, REFUSE }` and
    `fun captureKey(button, keycode): KeyCapture`. The rule: the key of the button gives `KEEP`, with no change; a reserved
    key gives `REFUSE`, with no change; any other key gives `ACCEPT`, and the swap of today happens in this function.
  - `CSS` `keyboardListener.keyDown` (`:71-99`) calls it:
    - `ACCEPT`: save, end the capture as today.
    - `KEEP`: end the capture as today, with no save.
    - `REFUSE`: `ERROR_SOUND`, set a private `captureRefused` flag, and the capture stays open. No save.

    The hint adds one line while the flag is `true`, for example `Refused: reserved key` (21 characters, the length of the
    longest hint line today). The flag clears when a capture starts.
  - Test first: T27, T28, T29 in `KeyboardInputTest`; new `T/players/PlayerSettingsTest.kt` with T30 to T36, T38, T39, T41.
    Prediction with `isReservedKey` giving `false` and `captureKey` as today's swap: T27, T29, T30, T33, T34, T35 and T36
    fail; the others pass.
- Check: the build command passes. Core 347. Smoke launch, and a launch of `--screen KEYBOARD_SETTINGS_SCREEN`.
- Commit: `031e: the key capture refuses the reserved keys`
- Contract checks: T27 to T36, T38, T39, T41, R7; intended change 3. Manual proof later: K3, K4.
- Done: yes

### 12. A swap cannot move `Enter`

- Changes:
  - `PlayerSettings.captureKey`: a key that another button has gives `REFUSE` when the current key of the selected button
    is reserved, because the swap would move that key to the other button. `KEEP` and the reserved-key rule stay first.
  - `C/input/KeyboardInput.kt`: remove the `TODO` at `:11`. Bindings made on the screen can no longer reach that case. The
    `TODO` of step 5 keeps the case of a hand-edited file.
  - Test first: T37, T40, T42 in `PlayerSettingsTest`. Prediction: T37 and T40 fail; T42 passes and guards the rule.
- Check: the build command passes. Core 350. A11 gives no output.
- Commit: `031e: a rebind cannot swap a reserved key to another button`
- Contract checks: T37, T40, T42, A11; intended change 4. Manual proof later: K4.
- Done: yes

### 13. Labels

- Changes:
  - `C/players/PlayerSettings.kt`: `keyLabel(code)`: the raw number if `code` is not in `0..255` (so that
    `Input.Keys.toString` cannot throw), else `Input.Keys.toString(code)`, else on `null` the raw number. `padLabel(code)`:
    `L2` for `GamepadInput.TRIGGER_LEFT_CODE`, `R2` for `TRIGGER_RIGHT_CODE`, else the number.
  - `CSS` `codeHintSupplier` (`:268-272`) uses them. The gamepad hint says "controller button or trigger" (`:152`).
  - Test first: T43 to T46 in `PlayerSettingsTest`, with the real `Input.Keys`. Prediction with a raw-number body: T43 and
    T46 fail; T44 and T45 pass.
- Check: the build command passes. Core 354: this is the minimum of A3. Smoke launch, and a launch of
  `--screen KEYBOARD_SETTINGS_SCREEN`: no exception from the render of the rows.
- Commit: `031e: readable labels for keys and triggers; the gamepad hint names triggers`
- Contract checks: T43 to T46; intended change 9. Manual proof later: K5, K6, G5, B7.
- Done: yes

### 14. A capture ends when the player leaves or the pad goes

- Changes, all in `CSS`:
  - `private var capturePad: Controller?`. `actionOnNextUpdate` (`:292`) adds the listener to the primary pad and keeps it
    in this field.
  - `private fun endCapture()`: remove the listener from `capturePad` and clear the field; restore `Gdx.input.inputProcessor`
    only if it is `keyboardListener`, so a call with no key capture does not write an old value; clear `selectedMegaButton`,
    `actionOnNextUpdate` and `captureRefused`. The ends in `keyDown` and `setControllerCode` call it.
  - `reset()` calls `endCapture()`. The pad-gone path (`:384-388`) reaches it, because `backAction` ends in `reset()`.
  - `render`, after the check for no pad: if `capturePad` is not `null` and it is not `controller`, then `endCapture()`,
    `ERROR_SOUND`, and `delayOnChangeTimer.reset()`, so that `undoSelection()` opens the menu, as after a normal capture.
  - No new test: this needs a running game (contract, "Named tests").
- Check: the build command passes. Core 354. R4 by reading the diff. Smoke launch, and launches of
  `--screen KEYBOARD_SETTINGS_SCREEN` and `--screen CONTROLLER_SETTINGS_SCREEN` (with no pad, the second one goes to the main
  menu, and the log has one `No controller found` line, as on `master`).
- Commit: `031e: a capture ends when the screen goes or the primary pad changes`
- Contract checks: R4; intended changes 7, 8. Manual proof later: G9, H6, D2, D3, D8.
- Done: yes

### 15. Back to pause

This is the largest risk, so it is the last code step and one commit. If it fails at the play test, step 17 can take
it out and leave steps 3 to 14.

- Changes:
  - `C/MegamanMaverickGame.kt`:
    - Private fields `heldLevelScreenKey: String?` and `heldUiCameraPosition = Vector3()`.
    - `fun showOverPause(key: String, props: Properties)`: keep `currentScreenKey` and the UI camera position; set
      `currentScreenKey = key`; call `show(props)` and `resize(...)` on the new screen. No `hide()`, `reset()`, `resume()` or
      `setCurrentScreen`.
    - `fun closeOverPause()`: call `hide()` and `reset()` on the bindings screen; set `currentScreenKey` back and clear
      `heldLevelScreenKey`; restore the UI camera position; call `resize(...)` on the level screen, because its backgrounds
      did not get a resize while it was held. No `show()`, `resume()` or `setCurrentScreen`.
    - `resume()` (`:741`): return before it changes `clock.paused` while `heldLevelScreenKey` is not `null`. `pause()` needs
      no change: it returns early, because `clock.paused` is `true`.
  - `C/screens/utils/GameSettingsPanel.kt:316-325` and `:352-361`: the back action calls `game.closeOverPause()` and returns
    `true`. The open calls `game.showOverPause(<screen>, props(...))`. The `else` branches for the main menu do not change.
  - `CSS`: `show(props)` stops the music only when the props have no back action; `show()` (`:369`) loses that line. Only the 2
    sites above pass that key, so it marks this path.
  - `LevelPauseScreen.kt`, `MegaMenuScreen.kt`, `MLS.reset()` do not change.
- Facts that the code depends on, checked on `6ee5859e6`:
  - The pause menu does not stay blocked. The panel's `onSelect` returns `true`, but `LevelPauseScreen.onAnySelection`
    (`:531-533`) calls `undoSelection()` while `showingSettings` is `true`. The close calls no `show()`, so the panel row
    (`currentKey`) stays, and H1 step 5 can see the same row.
  - **Call both functions directly, not through `runQueue`.** `CSS.render` uses `getUiCamera().combined` (`:416`) and never
    calls `viewport.apply()`, and only `apply()` calls `camera.update()` (read in the `gdx` `1.14.2` `Viewport` class). The
    open happens inside the level's `render`, so `MLS.draw` (`:1051`) applies the UI viewport in the same frame, and the
    bindings screen then has the right matrix. Through `runQueue`, the old screen would not draw after the move, and the
    rows would be off screen.
  - Effect of the direct call: in the frame of the open, the pause screen draws with the camera already moved, so the
    pause grid shows for 1 frame in place of the panel. At the close from `BACK`, the matrix is not updated yet, so the
    bindings screen draws correctly for its last frame. At the close from the pad-gone path, `render` returns before its
    draw, so that frame is black, as on `master`. The owner looks at this in H1, H2 and H9.
- Check: the build command passes. Core 354. R1, R2, R3 by reading the diff. A15 gives no output. Smoke launch (a level
  launch with no input does not reach this path, so it proves only that the level still starts). The H checks are the
  owner's.
- Commit: `031e: leaving a bindings screen in a level goes back to pause, with no restart`
- Contract checks: A15, R1, R2, R3; intended changes 5, 6. Manual proof later: H1 to H10, K9.
- Done: yes

### 16. Contract checks

- Changes: none, except docs. Add the backlog candidate from `docs/SCOPE.md` to the branch `docs/TODO.md`: the dead
  functions in `ControllerUtils` (`isKeyboardKeyPressed`, `isControllerKeyPressed`, `getController(index)`,
  `isControllerConnected(index)`). A fix for a failed check is its own commit.
- Check, in the worktree, on a clean build with the build command:
  - A1 to A5: the build passes; engine count as the baseline; core at least baseline + 53, 0 failures; the sorted warning
    list has no new line against `build-baseline-warnings.txt` (compare the lists, not only the counts).
  - A6 to A16: the `git diff master...HEAD` and `grep` commands in `docs/CONTRACT.md`, as written there.
  - T1 to T56: each case is present by name and passes (read `core/build/test-results/test/`). Write the 56 names in the
    process log, as `031d` did. The 10 `KeyboardInputTest`, 23 `GamepadInputTest` and 9 `MegaPlayersTest` cases are there.
  - R1 to R9: read `git diff master...HEAD -- core/src/main lwjgl3/`. R9: compare each new API with the table in this
    plan and in `docs/SCOPE.md`.
  - Code comments: the only new comment in `core/src/main` is the `TODO` of step 5.
  - Overlap: run `git worktree list`. Read the folder of each other active branch in its worktree (`051b` was active at the
    scan, with `MegamanMaverickGame.kt`, `MLS` and root `CLAUDE.md` in its diff). Run
    `git merge-tree --write-tree HEAD <branch>` for each one. Write the result in `docs/NOTES.md`.

  Write each result in the process log. Then give the owner the manual list of step 17.
- Commit: `031e: contract checks` (branch docs only).
- Contract checks: A1 to A16, T1 to T56, R1 to R9.
- Done: yes

### 17. The owner's play test

**The agent that runs these steps does not run these checks.** Only the owner plays them, and only the owner runs the
commands that touch `~/.prefs`. The full text of each check is in `docs/CONTRACT.md`, "Manual play test". Give it to the
owner as a list, in the contract's order, with the `cd` line first:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-031e-bindings`

1. S2, S3: the known start state, once, with the game closed.
2. K1 to K11: keyboard only.
3. G1 to G9: one gamepad (the PS3 pad).
4. D1 to D8: two gamepads (the PS3 pad and the NES-style pad, on the hub).
5. H1 to H10: back to pause.
6. B1 to B7: boot.
7. X1: the last restore.

Each check marked `[prefs]` ends with the restore command that the contract gives. Do not skip it.

- Changes: none, unless the play test finds a problem. A fix is its own commit, and the affected automated checks run
  again. Write the result of each check in the process log, and which checks were skipped and why.
- If the owner accepts the 1-frame draw of step 15, add a `TODO` at `showOverPause` that names it, with item `013` as the
  follow-up.
- **If back to pause fails.** The owner decided (`docs/SCOPE.md`, question 10): first try a fix commit. If the owner does
  not want that here, then:
  1. `git revert` the commit of step 15 in the worktree. Steps 3 to 14 stay. `BACK` in a level restarts the level, and
     the music stops, as on `master`.
  2. Update `docs/SCOPE.md` and `docs/CONTRACT.md` (intended changes 5 and 6, H1 to H10, A15, R1 to R3,
     `GameSettingsPanel.kt` in A7) and get the owner's approval again.
  3. Run the automated checks again.
  4. At the merge, issue `2026-10-25-bindings-screen-restarts-level.md` stays open, and the work goes to a `fix/` branch.
- Commit: `031e: play test results` (branch docs only).
- Contract checks: S2, S3, K1 to K11, G1 to G9, D1 to D8, H1 to H10, B1 to B7, X1.
- Done: no

### 18. Merge

Follow "Branch merge" in `docs/mega-refactor/CLAUDE.md`. Do these in this order:

1. The results of steps 16 and 17 are in the branch process log.
2. Get the owner's explicit approval to merge `mega-refactor-031e-bindings`. Approval for `051b` or any other branch does
   not count. Merge one branch at a time.
3. Overlap: run `git worktree list`, and read the folder of each other active branch again. Run
   `git diff --stat mega-refactor-031e-bindings...master`. **A merge of `master` into the branch is necessary if `master`'s
   new changes touch any of these paths.** Lesson from `031d`: this list is a rule, not a guide.
   - The production files of A6 and A7: `MegamanMaverickGame.kt`, `MLS`, `CSS`, `GameSettingsPanel.kt`, `KeyboardInput.kt`,
     `GamepadInput.kt`, `MegaPlayers.kt`, `Constants.kt`, `ControllerComponent.kt`, `AirDashBehavior.kt`, `DesktopLauncher.kt`,
     `E/controller/buttons/`.
   - The code that back to pause and the capture depend on: `LevelPauseScreen.kt`, `MegaMenuScreen.kt`,
     `E/screens/menus/`, `C/screens/utils/ScreenSlide.kt`, `C/audio/MegaAudioManager.kt`, `C/controllers/`, `C/input/`,
     `E/input/`.
   - The tests of A8, and `T/TestServices.kt`.
   - The build files: `build.gradle` (each module), `settings.gradle`, `gradle.properties`, `.github/workflows/ci.yml`, and the
     3 `run-game-*.sh` scripts.

   A merge of `051b` changes the first 2 paths of this list, so it forces this merge. For a forced merge: merge `master`
   into the branch in its worktree, run all automated checks of step 16 again, and tell the owner the result. If a
   conflict or a change touches back to pause or the capture, ask the owner which manual checks to run again. If no
   path matches (for example docs only), skip the merge, and step 18.8 catches problems. Write the result in
   `docs/NOTES.md`.
4. From the main checkout: `git merge --squash mega-refactor-031e-bindings`, then one commit named after the branch
   goal, for example `Per-slot bindings: PlayerSettings, the slot file, the screen rules, back to pause
   (mega-refactor-031e)`. The message lists the main changes (the storage swap and the removed classes, the new file with
   no migration, `--resetBindings`, the reserved keys, labels, the capture end, `selectButtonAction` at boot, "RESET TO
   DEFAULTS", back to pause) and ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Do not stage owner
   edits in the main checkout. No bare `git stash`.
5. No commit of this branch is in `.git-blame-ignore-revs` (no mechanical commit), so that file does not change.
6. On `master`, in one docs-only commit, staging only named paths:
   - `docs/mega-refactor/process-log.md`: an entry, in the format of that file.
   - `docs/mega-refactor/ITEMS.md`: the `031e` row to `merged` with the finish date; the `031f` row from `blocked` to
     `planned`. Item `031` stays `active`.
   - `docs/mega-refactor/items/031-input-frames/ITEM.md`: in the "Branches" line, `031e` (merged) and `031f` (planned), and
     the next branch. Remove the open question on a readable trigger label.
   - Each "Backlog candidate" in the branch `docs/TODO.md` becomes a new item. Each "Bug candidate" becomes a file in
     `docs/issues/`.
   - `docs/issues/`: for each file below, set `status: closed`, add a `## Resolution` section with the squash commit, and
     update its row in `docs/issues/README.md`:
     - `2026-10-25-bindings-screen-restarts-level.md` (fixed; leave it open if step 17 took back to pause out);
     - `2026-10-25-capture-state-left-after-gamepad-disconnect.md` (fixed);
     - `2026-10-25-reset-to-defaults-not-saved.md` (fixed);
     - `2026-10-22-select-button-action-resets.md` (fixed);
     - `2026-10-22-saved-key-bindings-not-applied.md` (closed as "on purpose": the owner's decision of 2026-10-30 is that
       saved bindings do not load by default; `--loadKeyboardPrefs` stays as a dev opt-in).

     `2026-10-25-p-key-binding-takes-screenshots.md` stays open.
   - `docs/mega-refactor/items/034-persisted-settings/ITEM.md`: change the line "Apply the saved bindings and select action
     at boot" (it conflicts with the owner's decision), answer the open question on the "load saved settings" button with
     the decision of 2026-10-30, and name `PlayerSettingsStore` and the slot file as the input of the item. The select
     action already applies at boot since this branch.
   - Root `CLAUDE.md`: replace `players.selectButtonAction` (line 50) with `players.settings(slot)` and `PlayerSettings`; add
     `--resetBindings` to the launcher flags (line 39). The line numbers are from `6ee5859e6`; `051b` also changes this file.
7. Copy `build-baseline-warnings.txt` from the worktree to the scratchpad, for step 18.8. In the worktree,
   `git status --short` must show only `build.log` and `build-baseline-warnings.txt`. These 2 untracked files make a plain
   `git worktree remove` refuse, so remove them with `rm`, then run
   `git worktree remove .worktrees/mega-refactor-031e-bindings` and `git branch -D mega-refactor-031e-bindings`. If an empty
   folder stays: `rmdir .worktrees/mega-refactor-031e-bindings/.gradle .worktrees/mega-refactor-031e-bindings`.
8. On `master`, run the build command. It passes, with the test counts of step 16 and no new warning line. If it fails,
   fix `master` at once, before any other merge. `build.log` is untracked on `master` too: do not stage it, and remove it
   after the check.

- Check: `git worktree list` no longer shows the branch. The build on `master` passes. `ITEMS.md` shows `031e` as `merged`.
  The 5 issues show `closed` in `docs/issues/README.md`.
- Contract checks: all, as recorded in steps 16 and 17; the automated checks again on `master`.
- Done: no

## Open questions

Answered (owner, 2026-10-30, verbatim): "plan approved", as recommended: 1 a), 2 a), 3 a).

1. **B2 and the old prefs files.** B2 (`docs/CONTRACT.md:597-603`) expects an old keyboard file with `S`, `X`, `Z`, `C` and an old
   PS3 file. The owner removed the prefs (contract question 1), and `~/.prefs` held 1 file at that time. With no old
   files, B2 passes but proves nothing.
   - a) Before S2, the owner writes one file named `Megaman Maverick - Keyboard Preferences` by hand, so that B2 has an old
     file to ignore.
   - b) Run B2 as it is, and write in the process log that it had no old file.
   - Recommendation: a). The code check for the same rule is T8 (the old names are never opened).
2. **The 1-frame draw of step 15.** This plan keeps the direct calls of `docs/SCOPE.md`, so the pause grid shows for 1 frame
   at the open.
   - a) Keep it. The owner judges it in H1, H2 and H9, and step 17 adds a `TODO` if the owner accepts it.
   - b) Remove it: `showOverPause` runs through `runQueue`, and `CSS.render` calls `game.viewports.get(ConstKeys.UI).apply()`.
     This changes one more line in `CSS` and needs its own check.
   - Recommendation: a).
3. **Reads of `~/.prefs` by the game process.** The rule says that the agent never reads this folder. A smoke launch makes the
   game read it (see "`~/.prefs`"). There is no launch option for another folder.
   - a) Accept this: it is the game's read, and it writes nothing.
   - b) No smoke launches from step 8 on. The owner's play test is then the first launch of those steps.
   - Recommendation: a).
