# Scope: mega-refactor-031e-bindings

What this branch changes and why. Write this file after the scan, before `CONTRACT.md` and
`PLAN.md`. The user approves it. If the scope changes later, update this file and get approval
again.

- Status: approved
- Approved: 2026-10-30

## Goal

Bindings and `selectButtonAction` belong to a player slot:

- One settings object per slot holds the key codes, the gamepad codes and `selectButtonAction`. It replaces the storage
  in `ControllerButtons`. Slot 0 is the only production slot (`MegaPlayers` has capacity 1 until `047`).
- The settings save to one new prefs file per slot. The old prefs files stay on disk, unused. There is no migration.
- The keyboard bindings screen refuses the reserved keys.
- Leaving the bindings screen during a level goes back to pause, with no restart.
- A capture ends when the player leaves the screen or when the pad goes.
- A bound trigger shows a readable label.
- `--resetBindings` is the last-resort launcher flag.

The owner's replug decision does not change: saved bindings do not load by default. This branch changes behavior on
purpose. Each change is listed in "Intended changes". The engine loses 2 unused classes and has no other change.

## Item

`031`: Input frames per player (`InputFrame`), devices and bindings. This branch takes these decisions, copied from
`ITEM.md`:

- 2026-10-25 (bindings): "two players with the same gamepad model must be able to have different maps, so a binding
  belongs to the player, not only to the device model."
- 2026-10-25 (bindings): "`selectButtonAction` is one setting per player."
- 2026-10-25 (bindings): "one key per button (1:1). The stick reaches the d-pad through the fixed threshold, not through
  a binding."
- 2026-10-25 (bindings): "leaving the bindings screen during a level goes back to pause, with no restart (issue
  `2026-10-25-bindings-screen-restarts-level.md`)."
- 2026-10-25 (bindings): "bindings and `selectButtonAction` are per slot now, kept in one settings object per slot.
  Profiles (a named copy of that object) wait for a feature branch during or after co-op (`047`)."
- 2026-10-25 (bindings): "`Esc` is a reserved system key and can never be bound."
- 2026-10-25 (bindings): "reserved keyboard keys: `Esc`, `Enter`, numpad `Enter` and the four arrow keys. The bindings
  screen refuses them. [...] (the bindings stay 1:1; defaults stay `WASD` and `Enter`)."
- 2026-10-25 (bindings): "gamepads get no fixed aliases. Default codes come from the library mapping on connect, and
  the player customizes them, as today."
- 2026-10-25 (bindings): "add a `--resetBindings` launcher flag (the last-resort escape hatch)."
- 2026-10-25 (bindings): "gamepad triggers can be bound, and act like any other button (through a threshold, because
  they are axes; see `NOTES.md`)."
- 2026-10-25 (bindings): "`selectButtonAction` defaults to `NEXT_WEAPON` when a slot (or later a profile) has no saved
  value. This is today's default."
- 2026-10-25 (bindings): "no migration of the old binding prefs. The new settings start from the defaults, and players
  set their bindings again once. The old prefs files are left on disk, unused."
- 2026-10-25: "`P` stays the screenshot trigger for now (with `--allowScreenshots`). It can be changed or removed later."
- 2026-10-26: "`031e`: bindings (the content planned before as `031d`)."
- 2026-10-26 (for `031c`): "`ControllerButtons` stays as the storage of the key and gamepad codes only, so the bindings
  screen does not change (`031e` replaces the storage)."
- 2026-10-30 (on the `031d` scope): "every connected gamepad reads one code set, from the primary gamepad (the one at
  index 0); the refusal of reserved keys on the bindings screen stays in `031e` (a `TODO` at the alias table until
  then); [...] the capture-state issue (`2026-10-25-capture-state-left-after-gamepad-disconnect.md`) goes to `031e`."
- 2026-10-30 (owner, at the `031d` play test, and for `031e`): "keep today's behavior for a replug. The player sets
  bindings, unplugs and plugs in the gamepad: it starts with the default bindings, and the player goes to the settings
  and selects "LOAD SAVED SETTINGS" [...]. Saved bindings do not load by default. After "LOAD SAVED SETTINGS", later
  replugs in the same session use the saved bindings, as today (`loadPrefsOnConnect`)."
- "Open questions" of `ITEM.md`: "a readable label for a bound trigger on the gamepad bindings screen."
- Issues: `2026-10-25-bindings-screen-restarts-level.md`, `2026-10-25-capture-state-left-after-gamepad-disconnect.md`.
  See "Issues" below for the others.

## Current state

Based on `master` at `5d4a37eac`. `E` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C` =
`core/src/main/kotlin/com/megaman/maverick/game/`, `L` = `lwjgl3/src/main/kotlin/com/megaman/maverick/game/lwjgl3/`, `MLS`
= `C/screens/levels/MegaLevelScreen.kt`, `CSS` = `C/screens/menus/ControllerSettingsScreen.kt`.

- **Storage:** `ControllerButton` (`E/controller/buttons/ControllerButton.kt:3`) holds `keyboardCode` and
  `controllerCode`. `ControllerButtons` (`E/controller/buttons/ControllerButtons.kt:5`) is an `OrderedMap<Any,
  ControllerButton>`. **The engine has no reader.** A `grep` of `engine/src/main` and `engine/src/test` gives only the 2
  definition lines. `E/input/` and `ControllerSystem` do not use them. So both classes can leave the engine.
- **Users of the storage** (all in `core`, not counting imports and the 2 definitions):
  - `ControllerButtons` or `ControllerButton`: 10 lines in 5 files (`ControllerUtilsExtensions` 6, and one each in
    `MegamanMaverickGame`, `KeyboardInput`, `GamepadInput`, `CSS`).
  - `keyboardCode`: 12 lines (`ControllerUtilsExtensions` 7, `CSS` 4, `KeyboardInput` 1).
  - `controllerCode`: 14 lines (`CSS` 6, `ControllerUtilsExtensions` 5, `GamepadInput` 3).
  - The game holds one instance: `C/MegamanMaverickGame.kt:216` (`buttons`), built at `:303`, given to the adapters at
    `:309-310` and to both screens at `:453-454`.
- **Boot:** `:303` builds the default keys. `loadControllerButtons` also sets the mapping for a pad that is connected
  (`C/controllers/ControllerUtilsExtensions.kt:24-31`), but `GamepadInput.primary` starts `null`, so the first `held()`
  sets the codes again (`C/input/GamepadInput.kt:31-41`). That boot step is dead since `031d`.
- **`--loadKeyboardPrefs`** (commit `1eaef3415`, `L/DesktopLauncher.kt:422-426`, `:157`): if given, `:304-308` loads the
  keyboard codes from the keyboard prefs file and `selectButtonAction` from that same file (scope question 6 moves the
  `selectButtonAction` read out of this flag). The default is `false`. All 3
  run scripts pass it (`run-game-alpha.sh:3`, `run-game-debug.sh:3`, `run-game-test.sh:3`). The packaged game passes no
  game arguments (`build-game.sh:83-92`). So a normal player gets defaults at boot, and the owner's dev runs load the
  keyboard prefs. It never loads gamepad prefs, so it does not touch the replug decision.
- **Replug flow today** (`031d` kept it):
  - `GamepadInput.loadPrefsOnConnect` starts `false` (`C/input/GamepadInput.kt:22`). It is session state and is not saved.
  - When the primary pad object changes, each code comes from the saved prefs of that pad name if `loadPrefsOnConnect`
    is `true`, else from `controller.mapping` (`:31-41`). A missing saved key falls back to the mapping (`:36-37`).
  - With no pad, every code goes to `null` (`:39`).
  - Only "LOAD SAVED SETTINGS" on the gamepad screen sets the flag to `true` (`CSS:221`). The keyboard row does not.
  - That row loads with the current code as fallback (`CSS:209-210`). "RESET TO DEFAULTS" does not clear the flag.
- **Persistence today:** LibGDX `Preferences`. On desktop each file is a Java `Properties` XML file in `~/.prefs/`
  (`Lwjgl3Preferences`).
  - Keyboard: `Megaman Maverick - Keyboard Preferences`, one integer per button name (`C/Constants.kt:8`).
  - Gamepad: `Megaman Maverick - Controller Preferences - <pad name>`, one file per pad name (`C/Constants.kt:7`,
    `ControllerUtilsExtensions.kt:13-14`).
  - `selectButtonAction`: key `SELECT_BUTTON_ACTION` in the keyboard file or the gamepad file, by the screen that changed
    it (`:92-112`, `CSS:346`, `:352`). The game has one value, so the value that loads depends on the screen.
  - The file `ControllerUtilsExtensions.kt` has 112 lines and 9 public functions.
- **`selectButtonAction`:** `C/players/MegaPlayers.kt:18`. 16 lines in 7 files read or write it: `CSS` 7,
  `ControllerUtilsExtensions` 3, `AirDashBehavior.kt:33`, `:38`, `C/entities/megaman/components/ControllerComponent.kt:99`,
  `MLS:926`, `MegamanMaverickGame.kt:306`, and the field. Tests: `MegaPlayersTest` 6 lines.
- **`MegaPlayers`:** `capacity` is 1 (`C/players/MegaPlayers.kt:15`). `PlayerInputs(capacity)` is the per-slot pattern
  (`C/input/PlayerInputs.kt:6`). An entity finds its slot with `playerSlot` (`E/players/PlayerComponent.kt:8`).
- **Reserved keys:** `keyboardListener.keyDown` (`CSS:71-99`) accepts any key. `KeyboardInput.kt:11` has the `TODO` that
  names `031e`. The alias keys are in `KeyboardInput.kt:12-19`.
- **The swap rule:** a key or code that another button has goes to that button in exchange (`CSS:81-83`, `:129-131`). So
  8 buttons always have 8 different codes. **With the reserved keys, the swap can move `Enter`.** `START` on `Enter` is the
  default. If the player sets `START` to `K` (the key of `A`), the swap gives `A` the key `Enter`. Then `Enter` gives `A`
  and `START`. See question 4.
- **Back in a level:** `C/screens/utils/GameSettingsPanel.kt:316-325` and `:352-361` pass a back action that calls
  `game.startLevel()`. `setCurrentScreen` (`C/MegamanMaverickGame.kt:998-1010`) always calls `hide()` and `reset()` on
  the old screen, and `MLS:1108-1141` (`reset`) runs `engine.reset()`, `spawnsMan.reset()` and `audioMan.unsetMusic()`. So
  the level is gone at the moment the bindings screen opens, not at the moment it closes. A new `BACK` target alone
  cannot fix this.
- **Pause and its panel:** `LevelPauseScreen` shows `GameSettingsPanel` by sliding the UI camera 16 tiles to the right
  (`settingsScreenSlide`, `C/screens/menus/level/LevelPauseScreen.kt:130-144`). `CSS.show()` moves the UI camera to its
  default position (`CSS:370`) and stops the music (`CSS:369`). The level music plays during pause
  (`LevelPauseScreen.kt:52`, `pauseMusicOnPause = false`). `stopMusic` stops the track (`C/audio/MegaAudioManager.kt:154`).
- **`game.render`** calls only `currentScreen.render` and `draw` (`:574-592`). The frame builds before that (`:567`), for
  every screen. The level screen updates the engine in its own `render`, so a level that is not current does not update.
- **Capture:**
  - Start: `CSS:284-296`. Keyboard: swaps `Gdx.input.inputProcessor`. Gamepad: `actionOnNextUpdate` adds the listener
    to the pad at index 0.
  - `onSelect` returns `true`, so `selectionMade` blocks all menu input until the capture ends (`delayOnChangeTimer`,
    `CSS:398-399`).
  - There is no cancel.
- **Triggers:** `031d` binds them through `axisMoved` (`CSS:106-114`). The codes are `1004` and `1005`
  (`GamepadInput.kt:16-17`). The row shows the raw code: `"${b.name}: $code"` (`CSS:268-272`).
- **Screens that open it:** `GameSettingsPanel` from `MainMenuScreen` and `LevelPauseScreen`. `--screen
  CONTROLLER_SETTINGS_SCREEN` also works. The gamepad row needs a pad (`GameSettingsPanel.kt:346`).
- **Tests:** `KeyboardInputTest` (10 cases, 4 lines use the old types), `GamepadInputTest` (23 cases, 27 lines use the old
  types or `loadPrefsOnConnect`), `ControllerUtilsExtensionsTest` (2), `MegaPlayersTest` (6 lines). `CSS` and
  `GameSettingsPanel` have no tests.

### Issues

| Issue | Result | Branch |
|---|---|---|
| `2026-10-22-saved-key-bindings-not-applied.md` | Confirmed, with a change. Boot uses defaults unless `--loadKeyboardPrefs` is given (`MegamanMaverickGame.kt:303-308`). Gamepad prefs never load at boot. The issue says "maybe on purpose". The owner's decision of 2026-10-30 answers it for bindings: saved bindings do not load by default. The flag is opt-in, so it does not break that decision. The line numbers in the issue are old. | Not `031e`. `031e` keeps this behavior. See question 9. |
| `2026-10-25-bindings-screen-restarts-level.md` | Confirmed (`GameSettingsPanel.kt:316-325`, `:352-361`). The restart is worse than the issue says: the level is reset when the screen opens (see above). | `031e` |
| `2026-10-25-p-key-binding-takes-screenshots.md` | Confirmed (`L/DesktopLauncher.kt:152`; `CSS:71` accepts `P`). `--allowScreenshots` is off by default, and no run script sets it. | `031e` does nothing. `P` is not a reserved key. The issue stays open. |
| `2026-10-25-capture-state-left-after-gamepad-disconnect.md` | Confirmed, and it has a worse form. `CSS:384-388` backs out with no cleanup, and `CSS:428-434` clears neither `selectedMegaButton`, the listener nor `actionOnNextUpdate`. New since `031d`: if the primary pad goes while another pad stays, `controller` is not `null`, so the back out does not run. The listener stays on the dead pad object, and `selectionMade` blocks the menu. The screen stays on the hint text with no way out. | `031e` |
| `2026-10-22-select-button-action-resets.md` | Confirmed, with a change. The field default is at `MegaPlayers.kt:18`, not at the line in the issue. With `--loadKeyboardPrefs`, `:306` loads the value from the keyboard file. Without the flag it resets at each boot. | See question 6. |
| `2026-10-25-reset-to-defaults-not-saved.md` (not in the brief, but in the same code) | Confirmed (`CSS:250-255`, `ControllerUtilsExtensions.kt:68-81`). | See question 5. |

## In scope

### 1. The settings object

`PlayerSettings` in `C/players/PlayerSettings.kt`. One instance per slot, owned by `MegaPlayers`, built with
`capacity`, like `PlayerInputs`.

- It holds: 8 key codes, 8 gamepad codes (the active set, `NO_CODE` when no pad is connected), and `selectButtonAction`
  (default `NEXT_WEAPON`).
- Codes are in `IntArray`s by `button.index`, so `held()` reads them with no map lookup and no boxing.
- It goes in `core`, not in the engine, because it names `MegaControllerButton` and `SelectButtonAction`, which are game
  content.
- It holds no session flag. `loadPrefsOnConnect` stays on `GamepadInput`.
- A profile later is this object with a name. Slots 1 to 3 need only a higher `capacity`.

`ControllerButton` and `ControllerButtons` leave the engine (2 files, 8 lines). The engine has no reader, so no engine
API replaces them. `E/controller/buttons/` keeps `ButtonActuator`, `ButtonStatus` and `IButtonActuator`.

`KeyboardInput` and `GamepadInput` take a `PlayerSettings`. `game.buttons` goes. `create()` passes
`players.settings(PlayerSlot(0))`. The dead boot step of `loadControllerButtons` goes with it.

### 2. Persistence

`PlayerSettingsStore` in `C/players/PlayerSettingsStore.kt`. It is the only class that reads or writes the file, so item
`034` can move it to `settings.json` in one place. It replaces `ControllerUtilsExtensions.kt`, which is deleted.

- **File:** one prefs file per slot, `Megaman Maverick - Player Settings - Slot 0`, in `~/.prefs/`, in the same `Properties`
  XML format as today. `PreferenceFiles` loses the 2 old names and gets 1 new name.
- **Keys** (`<BUTTON>` is the `MegaControllerButton` name):
  - `KEYBOARD.<BUTTON>`: an integer key code.
  - `GAMEPAD.<pad name>.<BUTTON>`: an integer code (question 1).
  - `SELECT_BUTTON_ACTION`: the enum name.
- **Writes:** a rebind saves the 8 codes of that table at once, as today. The select row saves its value at once, as today.
- **Reads:** a missing key keeps today's fallback (the current code on the screen, the mapping on connect). There is no
  check of a whole table. `--resetBindings` is the answer to a hand-edited file.
- **No migration:** the code does not read the old files and does not change them.

Effect on the owner's existing prefs (`~/.prefs/` on this machine):

- `Megaman Maverick - Controller Preferences - PS3 Controller` has 8 codes and no `SELECT_BUTTON_ACTION`. It differs from
  the mapping in 3 buttons (`A` = `0`, `B` = `2`, `SELECT` = `1`). This file stays, unused. The owner rebinds these 3 once.
- `Megaman Maverick - Keyboard Preferences` has 8 keys and `SELECT_BUTTON_ACTION`, and
  `Megaman Maverick - Controller Preferences - NEXT SNES Controller` has 8 codes and `SELECT_BUTTON_ACTION`. Both files
  stay, unused. The owner sets these bindings again once, and `--loadKeyboardPrefs` loads nothing until then. At the
  scan these 2 files were only in the backup `~/.prefs.bak-031d` of the `031d` play test (the owner still had to restore
  it), so the line first written here ("no keyboard prefs file") was wrong and is corrected.

### 3. "LOAD SAVED SETTINGS" and the replug

The flow keeps the same steps, on the new storage:

- `GamepadInput` keeps `loadPrefsOnConnect` (starts `false`, not saved) and the primary tracking. On a change of primary:
  each code comes from `GAMEPAD.<pad name>.<BUTTON>` if the flag is `true`, else from the mapping. A missing key gives
  the mapping. With no pad, every code goes to `NO_CODE`.
- The gamepad row reads the table of the primary pad, with the current code as fallback, and sets the flag to `true`.
- The keyboard row reads the keyboard table and does not set the flag.
- Both rows also read the one `SELECT_BUTTON_ACTION` value.
- `--loadKeyboardPrefs` stays (the run scripts pass it). It reads the keyboard table and `SELECT_BUTTON_ACTION` of slot 0.

### 4. `--resetBindings`

- A new boolean flag in `DesktopAppArgs`, passed as `params.resetBindings` next to `params.loadKeyboardPrefs`. It is not
  in `LaunchOptions`, because it needs no check by `resolveLaunchOptions`, and it is for players too.
- `create()` acts on it, because `Gdx.app` does not exist before `Lwjgl3Application` starts. It clears the whole file of
  each slot (all keyboard keys, all pad tables, `SELECT_BUTTON_ACTION`), flushes, and logs one line.
- It runs before the `--loadKeyboardPrefs` block. So both flags together give defaults, and
  `./run-game-alpha.sh --resetBindings` works although the script passes `--loadKeyboardPrefs`.
- After it, "LOAD SAVED SETTINGS" finds no keys, so the codes stay, and it still sets `loadPrefsOnConnect`. A replug then
  gives the mapping, because no saved key exists.
- It does not touch the old prefs files.
- It changes no in-memory default: boot already uses defaults.

### 5. Reserved keys

- `KeyboardInput` gets `isReservedKey(keycode)`: the alias keys plus `Esc`. There is one source for the list. The `TODO` at
  `KeyboardInput.kt:11` goes.
- `keyboardListener.keyDown` refuses a reserved key: `ERROR_SOUND`, a line in the hint that says why, and the capture
  stays open. The codes and the prefs do not change.
- The default `START` on `Enter` still works: it is a default value, not a capture. "RESET TO DEFAULTS" restores it. The
  alias also gives `START`, so the two give one bit.
- A press of the key that the selected button already has ends the capture with no change. This includes `Enter` on
  `START`, so that case is not a refusal, and a player who opened a capture by mistake can leave it.
- The swap rule keeps `Enter` off every other button (question 4).
- Gamepads have no reserved code.

### 6. Back to pause

The bindings screen opens over the paused level. The level screen is not hidden or reset.

- Two new functions on `MegamanMaverickGame`, used only for this path. Open: it remembers the level screen key and the UI
  camera position, then sets the current screen and calls `show(props)` and `resize`. It calls no `hide()`, no `reset()` and
  no `resume()`. Close: it calls `hide()` and `reset()` on the bindings screen, sets the level screen back as current,
  restores the UI camera position, and calls `resize`. It calls no `show()`.
- `clock.paused` stays `true`. The systems stay off. The level does not update, because `game.render` calls only the
  current screen. So entities, the checkpoint and the room stay as they are.
- `GameSettingsPanel.kt:316-325` and `:352-361` open with it. Their back action calls close.
- `CSS.show` does not stop the music on this path (question 3).
- The player lands on the settings panel of the pause menu, where the player left it. The panel `BACK` then goes to the
  pause grid, as today.
- `resume()` returns early while the level is under the bindings screen. Without this, `--resumeOnFocusGained` (off by
  default) would clear `clock.paused` with the level hidden.

**Without `Esc`:** the player leaves through the `BACK` row, which runs `backAction`. The pad-gone path (`CSS:384-388`)
runs the same action. `031f` adds `Esc` as another input for that action and needs no new path. This branch does not
change how the screen reads its menu input.

### 7. Capture

- A private `endCapture()` in `CSS`: it removes the listener from the pad it was added to (kept in a field), restores
  `Gdx.input.inputProcessor`, and clears `selectedMegaButton` and `actionOnNextUpdate`. `reset()` calls it. The pad-gone path
  reaches it, because `backAction` ends in `reset()` on both paths.
- `render` also ends a gamepad capture when the primary pad is no longer the pad of the capture (the index-shift case). It
  plays `ERROR_SOUND`, and the menu works again, so the player stays on the screen.
- Capture still listens to one pad, the primary. A press on another pad does nothing, as today.
- The hint text says "controller button or trigger" on the gamepad screen.

### 8. Labels

A bound trigger shows `L2` or `R2` in place of `1004` or `1005` (names from `ControllerMapping.buttonL2` and `buttonR2`).
Other gamepad codes stay as numbers, because the library has no names for them. See question 7 for keyboard names.

### 9. `selectButtonAction` per slot

`MegaPlayers.selectButtonAction` goes. Each reader reads its slot: `AirDashBehavior` and `ControllerComponent` through
`playerSlot`, `MLS:926` through `player`, `CSS` through the settings object that it holds. The default stays `NEXT_WEAPON`.

### New APIs and their first callers

| API | First callers |
|---|---|
| `PlayerSettings` (codes, `NO_CODE`, `selectButtonAction`) | `KeyboardInput.held()`, `GamepadInput.held()`, `CSS`, the 4 reader lines |
| `MegaPlayers.settings(slot)` | `MegamanMaverickGame.create()`, `postCreate()`, `AirDashBehavior`, `ControllerComponent`, `MLS:926` |
| `PlayerSettingsStore(slot)` | `create()`, `postCreate()` |
| Boot-order function next to `PlayerSettingsStore`, and the label function next to the capture decision (test seams, contract question 2) | `create()`; `ControllerSettingsScreen` |
| `saveKeyboard` | `CSS` key capture |
| `loadKeyboard` | `CSS` load row; `create()` (`--loadKeyboardPrefs`) |
| `saveGamepad(padName)` | `CSS` pad capture |
| `loadGamepad(padName)` and the per-button read | `CSS` load row; `GamepadInput` on a change of primary |
| `saveSelectButtonAction` | `CSS` select row |
| `loadSelectButtonAction` | `CSS` load row; `create()` |
| `clear()` | `create()` (`--resetBindings`) |
| `clearKeyboard()`, `clearGamepad(padName)` | `CSS` reset row (only with question 5 option a) |
| `KeyboardInput.isReservedKey` | the capture decision function |
| Capture decision function (accept, keep, refuse), next to `PlayerSettings` | `CSS` key capture |
| `--resetBindings`, `params.resetBindings` | `create()` |
| 2 functions on `MegamanMaverickGame` (open and close over pause; the plan names them) | `GameSettingsPanel` (open, 2 sites); the back action (close, 2 sites) |
| `PreferenceFiles` entry for the new file | `PlayerSettingsStore` |
| Private `endCapture()`, capture pad field, label function in `CSS` | `CSS` |

Nothing here has no caller. No slot-specific code exists beyond `capacity` loops. No engine API.

### Removed

`E/controller/buttons/ControllerButton.kt`, `E/controller/buttons/ControllerButtons.kt`,
`C/controllers/ControllerUtilsExtensions.kt`, `game.buttons`, `MegaPlayers.selectButtonAction`, the 2 old
`PreferenceFiles` names, the `TODO` at `KeyboardInput.kt:11`.

## Out of scope

- `031f`: the bindings screen's menu input, `Esc` as back and pause, the menus on frames, the removal of
  `controllerPoller`. No `Esc` code in this branch, except that the capture refuses it.
- `047`: co-op flows, a join screen, device claims, slots above 0. Profiles (`047` or later).
- `034`: `settings.json`, volume, loading bindings at boot. The new prefs file is the input for that item.
- `013`: the pause overlay stack. Its stack replaces the 2 new functions later.
- Per-pad code sets for two pad models at the same time (decision of 2026-10-30): every pad reads the primary's set.
- A cancel key in a capture. `Esc` is refused, as the decision says. A player who opens a capture by mistake presses the
  key that the button already has.
- A check of the whole table on load, and the "keep these bindings?" timer (decision: none).
- The touch overlay.
- Moving `P`: decision. The `TODO` at `L/DesktopLauncher.kt:150` stays.
- Dead functions in `ControllerUtils` (`isKeyboardKeyPressed`, `isControllerKeyPressed`, `getController(index)`,
  `isControllerConnected(index)`: no caller outside the object). They are not part of the bindings work. A backlog candidate.

## Intended changes

Each item below is a change that `CONTRACT.md` names under "Intended changes":

1. Bindings save to and load from `Megaman Maverick - Player Settings - Slot 0`. The old files are not read, so after the
   merge "LOAD SAVED SETTINGS" and `--loadKeyboardPrefs` find nothing until the player rebinds.
2. `selectButtonAction` has one saved value for the slot. Today it has one value per file, so the value that loads depends
   on the screen.
3. A reserved key in a keyboard capture is refused, with a sound and a reason. Today it is accepted.
4. A rebind that would move a reserved key to another button is refused (question 4). Today it swaps.
5. Issue `2026-10-25-bindings-screen-restarts-level.md`: `BACK` from the bindings screen in a level goes back to the
   settings panel of the pause menu, in the same level state. Today the level restarts.
6. The level music keeps playing on the bindings screen in a level (question 3). Today it stops.
7. Issue `2026-10-25-capture-state-left-after-gamepad-disconnect.md`: leaving the screen ends the capture. The next visit
   shows the menu, and no listener stays on the old pad.
8. If the primary pad goes during a capture and another pad stays, the capture ends and the menu works. Today the screen
   stays on the hint with no way out.
9. A bound trigger shows `L2` or `R2`. The gamepad hint names triggers. (Keyboard names too, with question 7 option b.)
10. `--resetBindings` exists.
11. Issue `2026-10-25-reset-to-defaults-not-saved.md`: "RESET TO DEFAULTS" also clears the saved values of that table and
    `SELECT_BUTTON_ACTION` (question 5).
12. Issue `2026-10-22-select-button-action-resets.md`: the saved `selectButtonAction` applies at boot (question 6).

## What must stay the same

- **The replug decision, exactly:**
  - A player rebinds, unplugs and plugs in the pad: the pad has the mapping, and the saved table stays saved.
  - "LOAD SAVED SETTINGS" on the gamepad screen: the saved codes apply, and `loadPrefsOnConnect` becomes `true`.
  - Later replugs in the same session use the saved codes.
  - A new session starts with `loadPrefsOnConnect` `false`.
  - A pad name with no saved table gets the mapping, also with the flag on.
- Keyboard codes at boot are the defaults (`WASD`, `K`, `J`, `Enter`, `L`), unless `--loadKeyboardPrefs` is given.
- Saved bindings never apply at boot without that flag. Gamepad bindings never apply at boot.
- The aliases: `Enter`, numpad `Enter` and the arrows always work. The bound keys work with them, with one bit per button.
- 1:1: each button has one code in each table. The swap rule stays, except for question 4.
- A rebind saves at once. The select row saves at once and cycles in the same order.
- The default `selectButtonAction` is `NEXT_WEAPON`. The 3 actions behave the same.
- Every connected gamepad reads one code set, from the primary. The left stick and the triggers read as in `031d`.
- An unplugged gamepad does not pause the game.
- From the main menu, `BACK` returns to the main menu, and the music stops as today.
- The gamepad row refuses to open with no pad. The screen backs out when the last pad goes.
- The menu input of the screen and all other menus (`031f` owns it).
- The slot-0 frame builds only at `MegamanMaverickGame.kt:567`. The diagnostics entry keeps its name.
- `InputFrameBuilder`, `ControllerSystem`, `TURN_CONTROLLER_*`, and the rest of the engine.
- The pause menu itself. A level that is not left through this path.

## Tests

The engine only loses 2 classes that nothing reads. `./gradlew engine:test` must pass with the same count. No new engine
test is needed, because no engine behavior changes.

- `PlayerSettingsStoreTest` (new; it takes over `ControllerUtilsExtensionsTest`, with mocked `Gdx.app` prefs):
  - a missing key gives `null`, and a saved code gives its value, also `0`;
  - keyboard and gamepad tables do not mix; two pad names do not mix;
  - `selectButtonAction` round trip, a missing key, an unknown name;
  - `clear()` removes all keys and flushes;
  - only the new file name is opened (the old files are never read or written).
- `KeyboardInputTest` (keep the 10 cases, moved to `PlayerSettings`; add): `isReservedKey` is true for the 7 keys and
  false for `W`, `K` and `P`.
- `GamepadInputTest` (keep the 23 cases, moved to `PlayerSettings` and the store; add): the replug sequence of "What must
  stay the same", as one test: mapping, `loadPrefsOnConnect`, saved, replug saved, unknown name gives the mapping. A new
  `GamepadInput` starts with the flag `false`.
- `MegaPlayersTest`: the 6 lines move to `settings(slot)`; the default is `NEXT_WEAPON`.
- The refusal and swap rule: `CSS` has no tests. Put the decision (accept, refuse) in one pure function next to the
  settings object, so a test can cover it: the 7 reserved keys, a free key, a swap, the key that the
  button already has (also `Enter` on `START`), and question 4's case.
- The back-to-pause path, the music, the UI camera and the capture cleanup need a running game. They are manual checks in
  `CONTRACT.md`. No temporary contract suite: the parts that stay the same are the kept test cases.

## Measured size

| | Files | Detail |
|---|---|---|
| Engine, deleted | 2 | 8 lines, 0 readers in the engine |
| Core, new | 2 | `PlayerSettings`, `PlayerSettingsStore` |
| Core, deleted | 1 | `ControllerUtilsExtensions.kt`, 112 lines, 9 public functions |
| Core, changed | 10 | `KeyboardInput`, `GamepadInput`, `CSS`, `GameSettingsPanel`, `MegamanMaverickGame`, `MegaPlayers`, `Constants`, `ControllerComponent`, `AirDashBehavior`, `MLS` |
| Launcher, changed | 1 | `DesktopLauncher.kt` |
| Tests, changed or new | 6 | `KeyboardInputTest`, `GamepadInputTest`, `MegaPlayersTest`, the store test replaces `ControllerUtilsExtensionsTest`, one test for the capture decision |

16 production files. Call sites that change: 10 type references, 12 `keyboardCode`, 14 `controllerCode`, 15
`selectButtonAction` lines (16 minus the field). `LevelPauseScreen` does not change. `MLS` changes in one line.

**One branch or a split:** the storage work touches 15 of the 16 production files. The screen rules (reserved keys, capture,
labels, back to pause) touch 4: `CSS`, `KeyboardInput`, `GameSettingsPanel`, `MegamanMaverickGame`. 3 of these 4 are also in
the storage work, and `CSS` is the center of both. See question 10.

## Affected areas and risks

- **No migration:** the first run after the merge shows defaults on "LOAD SAVED SETTINGS". This is the decision. The owner
  rebinds 3 pad buttons.
- **Level under another screen:** a new state for the game. Things that could go wrong: the UI camera (the panel is off
  screen if the position is not restored), a window resize while the level is hidden, a `resume()` from the window
  listener, and one frame of the level drawn after `show()` moves the camera. All need a manual check. `setCurrentScreen`
  from the hidden state must not happen: only the back action leaves this state.
- **`clock.paused` on the bindings screen:** today it is `false` there, because `setCurrentScreen` resumes. With this path it
  is `true`. `CSS` and `MegaMenuScreen` do not read it. The 6 other screens that read it are not reached from here.
- **`TURN_CONTROLLER_ON`:** `MLS.reset()` submits it today on the way to the bindings screen. The new path does not. A pause
  needs an unlocked frame unless it comes from `--pauseOnFocusLost`. In that case the pause menu is already locked
  (candidate 5 in the item `NOTES.md`), so this branch adds no new lock.
- **Pad names in keys:** a name goes in a key, not in a file name. `Properties` XML escapes the name. Inference: this also
  removes a risk from a name with a path character, which the old file name had. Not checked on a real pad.
- **Two pads of different models:** the second pad still reads the primary's codes. `031d`'s scope said that `031e` ends
  this limit. With question 1 option a it does not end. See that question.
- **`--resetBindings` in a shortcut:** it clears the file at every launch. This is the meaning of the flag. The launcher
  description says so.
- **Bindings on a hand-edited file:** a duplicate or a reserved key in the file loads as written. `--resetBindings` fixes it.
- **`Enter` in a capture:** the frame still sees the key (the adapters read the device), but `selectionMade` blocks the menu
  during a capture, so the refused key does nothing else.
- **Keyboard names in the font (question 7):** names such as `L-Shift` need glyphs that the menu font may not have. Check
  on the screen. `Input.Keys.toString` can return `null` for an unknown code, and it throws `IllegalArgumentException` for a code
  outside `0..255`, so the raw code is the fallback in both cases.
- **The screen has no tests:** see "Tests".

## Overlap with other branches

See `docs/NOTES.md`. `051b-camera-behaviors` is active and shares `MegamanMaverickGame.kt` and `MLS`, in other lines.
`084a-player-state` merged into `master` during this scan and has no overlap.

## Open questions

Answered (owner, 2026-10-30, verbatim): "yes", to all 10 recommendations below.

1. **One gamepad table per slot, or one per pad name inside the slot?**
   - a) One table per pad name (`GAMEPAD.<pad name>.<BUTTON>`). The active set is still one set, from the primary. This is
     the item `NOTES.md` plan ("each table is still keyed by `keyboard` or `pad:<name>`"). A binding belongs to (slot, pad
     name), so two players with the same model can differ. A model with no table gets the mapping, as `031d` made it.
   - b) One table per slot (`GAMEPAD.<BUTTON>`). The `031d` scope assumed this ("a slot has one gamepad code set for all
     its pads"). Fewer keys. But after "LOAD SAVED SETTINGS", a new model gets a table made for another model. Inference:
     for a pad that SDL does not know, that table can be wrong. This goes against the reason for the replug decision.
   - Recommendation: a). It keeps the replug behavior exactly, also for a second model. One string in the key is the only
     extra code.

2. **How does the level stay alive under the bindings screen?**
   - a) Hold the level screen: 2 new functions on the game (see "Back to pause"). `LevelPauseScreen` and `MLS` do not change.
     The bindings screen stays one class.
   - b) Make the bindings a panel inside `LevelPauseScreen`, like `GameSettingsPanel`. No new game state. But `CSS` (435
     lines) must split into a panel and a screen, for the main menu path, and `031f` then has 2 hosts.
   - Recommendation: a). `013` replaces it with its overlay stack later.

3. **The level music on the bindings screen.**
   - a) It keeps playing, as in the pause menu. No restart of the track on return.
   - b) It stops, as today, and starts again on return. `stopMusic` rewinds the track, so it starts from the beginning.
   - Recommendation: a).

4. **The swap that would move a reserved key.** `START` is on `Enter`, and the player sets `START` to `K`, the key of `A`.
   - a) Refuse: `ERROR_SOUND`, a reason in the hint, and the capture stays open. The player first moves `A` to a free key.
   - b) Accept, and give `A` the default key of `START`: this is `Enter`, so it does not help. A rule that looks for a free
     key adds a case that the player cannot predict.
   - c) Accept the swap: `Enter` gives `A` and `START`. This breaks "the bindings screen refuses them".
   - Recommendation: a).

5. **Issue `2026-10-25-reset-to-defaults-not-saved.md`.**
   - a) Fix it here: the reset row also clears that table's saved keys and `SELECT_BUTTON_ACTION`. The row's code changes for
     the new storage anyway. `NOTES.md` lists this as layer 4 of the escape hatch. After a reset, "LOAD SAVED SETTINGS"
     gives defaults, and the old saved codes are gone.
   - b) Keep today's behavior: the reset is in memory only, so "LOAD SAVED SETTINGS" brings the old codes back.
   - Recommendation: a).

6. **Saved `selectButtonAction` at boot** (issue `2026-10-22-select-button-action-resets.md`).
   - a) Apply it at boot in this branch: the `create()` call moves out of the `--loadKeyboardPrefs` block. Your reason for
     not loading bindings (a bad mapping can make a controller unusable) does not apply: this value cannot lock a player
     out. The issue closes.
   - b) Leave it to `034`: it loads only with `--loadKeyboardPrefs` or "LOAD SAVED SETTINGS". `034` depends on `007`, which
     has not started.
   - Recommendation: a). The decision of 2026-10-30 names bindings only, so this needs your word.

7. **Labels.**
   - a) Triggers only (`L2`, `R2`).
   - b) Triggers, and keyboard key names through `Input.Keys.toString` (`A: K` in place of `A: 39`). The item `NOTES.md`
     plan has "key names on screen", and a refusal is easier to read with names. It is not in `ITEM.md`.
   - c) Also names for gamepad buttons. The library gives none, so the branch must keep a table for the SDL layout.
   - Recommendation: b), with the raw code as fallback.

8. **The format of the new file.**
   - a) A LibGDX prefs file per slot, as above. It works now, and a profile is later another file.
   - b) One prefs file with a slot prefix on each key.
   - c) A JSON file now. This is `034`'s `settings.json`, which needs `007`.
   - Recommendation: a).

9. **Issue `2026-10-22-saved-key-bindings-not-applied.md` and item `034`.** The decision of 2026-10-30 answers the issue's
   question and `034`'s open question ("Is the 'load saved settings' button on purpose"). But `034`'s proposed scope still
   says "Apply the saved bindings and select action at boot".
   - a) At the `031e` merge, on `master`: close the issue as "on purpose", and update `034`.
   - b) Leave both as they are.
   - Recommendation: a). `--loadKeyboardPrefs` stays as a dev opt-in. This needs no code in the branch.

10. **One branch or a split?**
    - a) One branch, with the storage steps first and the screen steps after, each with its own check in the plan. 3 of the
       4 files of the screen rules are also in the storage work. One play test covers both.
    - b) Two branches: storage first (15 files), then the screen rules (4 files). `031f` then waits for both.
    - c) a), but the back-to-pause step moves to a `fix/` branch if the play test fails. It is the only part that does not
       depend on the storage.
    - Recommendation: a), with c) as the fallback.
