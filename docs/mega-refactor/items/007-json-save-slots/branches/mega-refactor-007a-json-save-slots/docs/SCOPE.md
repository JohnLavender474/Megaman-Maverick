# Scope: mega-refactor-007a-json-save-slots

What this branch changes and why, and its contract.

- Status: approved
- Approved: 2026-11-26, by owner decision (no backwards compatibility, exactly 5 slots, keep `version`; first pass
  started from the phone, brief in chat)

## Goal

Replace the single positional save string in the XML prefs file with 5 JSON save slots behind a small `SaveStorage`
interface. The player picks a slot when saving and when loading. The new logic is fresh: it does not read the old save.

## Item

`007` JSON save slots. From `ITEM.md` (proposed scope, with the decisions of 2026-11-26):

- A `SaveStorage` interface with desktop (files), Android (local) and web (Preferences) backends.
- 5 fixed slots `saves/slot-1.json` to `saves/slot-5.json` (decision 2026-11-26). A schema with a `version` field, so
  that a later change has a hook, and a safe write with `.tmp` and `.bak`. Refuse a `version` newer than the game.
- No legacy import, no `LegacySaveV0` parser, no migration chain and no compatibility fixtures. The old XML prefs file
  is not read.
- A slot picker in the UI for load and save.

## Current state

Scan of `master` at `2ebb704`.

- `MegamanMaverickGame.saveState()`, `hasSavedState()`, `loadSavedState()` (`MegamanMaverickGame.kt:880-922`) write and
  read `GameState.toString()` / `fromString()` under the key `ConstKeys.STATE` in `Gdx.app.getPreferences(saveFileName)`.
- `saveFileName` (`MegamanMaverickGame.kt:283,296`) is `launch.saveFile` (`--saveFile`) or `params.title.saveFileName`
  (`IGameTitle.saveFileName`: `PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE` for Maverick, `LostMemoriesTitle.SAVE_FILE_NAME`).
- `saveState()` writes nothing when `launch.savesAllowed` is false (`--heartTanks`, `--healthTanks`, `--difficulty`).
- Callers: `SaveGameScreen.kt:75` (SAVE row, plays a ping and stays) and `MainMenuScreen.kt:148-172` (LOAD SAVE FILE row:
  load, then the level select if `INTRO_STAGE` is completed, else start `INTRO_STAGE`; error sound when no save).
- `GameState.toString()` (`GameState.kt:333`) is the positional string; `fromString()` (`:357`) parses it. It drops
  unknown names with an error log, does not record an `EXTRA_LEVEL` as completed, puts health tanks back empty and uses
  `NORMAL` for an unknown difficulty.
- Tests: `GameStateSaveStringTest` (the string format), `GameStateLivesTest` (reads `toString()`).
- No JSON code exists in `core` or `engine`. LibGDX `JsonReader`/`JsonValue` are in `gdx` core and need no natives.

## In scope

- `SaveStorage` (core, package `state/saves/`): `read(name): String?` and `write(name, text)`. Two production
  backends: `FileSaveStorage(root: FileHandle)` (desktop `Gdx.files.external`, Android and iOS `Gdx.files.local`) with the
  safe write, and `PreferencesSaveStorage(prefs)` (web and any other platform). Test backend `InMemorySaveStorage` in
  `core/src/test` only.
- `SaveFile` (the data of one slot) and `SaveFileJson` (schema v1 text). `SaveSlots` (5 slots, `slot-N.json`, read
  result typed as `SaveSlotContent`: empty, used, newer version, unreadable).
- `IGameTitle.saveFileName` becomes `saveFolderName` (Maverick `.megaman-maverick`, Lost Memories
  `.mega-man-x-lost-memories`). The slots go in `<saveFolderName>/saves/`.
- `--saveFile <name>` keeps its flag name and maps to the subfolder `<saveFolderName>/saves/<name>/`.
- `MegamanMaverickGame`: `saveSlots`, `activeSaveSlot`, `saveState(slot)`, `loadSavedState(slot)`; `hasSavedState()`
  is removed.
- `SaveSlotScreen` (one class, two `ScreenEnum` entries `SAVE_SLOT_SCREEN` and `LOAD_SLOT_SCREEN`): 5 slot rows and
  BACK. `SaveGameScreen` SAVE and `MainMenuScreen` LOAD SAVE FILE open it.
- Remove `GameState.fromString()`, `GameStateSaveStringTest` and `PreferenceFiles.MEGAMAN_MAVERICK_SAVE_FILE`.
- Unit tests for the slots, the JSON and the file safe write.

## Out of scope

- Legacy import of the XML prefs save (owner, 2026-11-26).
- Copy and delete of slots in the UI, an overwrite confirmation, the health tank fill, lives and weapons in the save
  (open questions of the item).
- `settings.json` and the last used slot across restarts (item `034`).
- A web or Android launcher (none exists).

## Affected areas and risks

- `MegamanMaverickGame.kt`, `SaveGameScreen.kt`, `MainMenuScreen.kt`, new `SaveSlotScreen.kt`, `ScreenEnum.kt`,
  `IGameTitle.kt`, `MaverickTitle.kt`, `LostMemoriesTitle.kt`, `Constants.kt`, `GameState.kt`, `DesktopRunner.kt` (flag
  help text), `titles/README.md`.
- Risk: the slot screen is new UI that no test covers; it reuses the `SaveGameScreen` row and arrow pattern.
- Risk: an existing save in the XML prefs file is no longer seen by the game (intended, owner decision). The file stays
  on disk.
- Risk: the music of the screen that opens the picker restarts when the player goes back (each menu `show()` plays
  its music).

## Overlap with other branches

- `185c` (games layout) may move `MaverickTitle`, the screens and `LostMemoriesTitle`. The change here is small in each
  file; a merge resolves it by moving the edits.
- `187` (main menu overhaul, `187c` later) rewrites `MainMenuScreen`. This branch only changes its LOAD SAVE FILE row.
- `034` (settings file) will hold the last used slot.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` | no effect | The slots are fields of the game object, not globals. |
| `O1-R4` | helps | Typed `SaveFile` and `SaveSlotContent`; no new property keys. |
| `O1-R5` | helps | `SaveStorage` has only `read` and `write`, the calls that production makes; no `list`/`delete`. |
| `O2-R5` | no effect | One save per session, as decided in 2026-10-23. |
| `O2-R8` | no effect | The save is local data, read only at menus. |
| `O3-R1` | waiting | `savedAt` uses `java.time.Instant`; the web path waits for `032`. The web backend uses Preferences only. |
| `O6-R1` | waiting | Gradle could not be confirmed here; CI of the PR is the check. |
| `O6-R2` | helps | Unit tests name the slot rules. |
| `O9-R2` | helps | The `version` field is the hook; a newer version is refused and not overwritten. No old format is loadable, by owner decision (no compatibility). |
| `O9-R1` | no effect | The picker uses the d-pad, `A`/`START` and back, as the other menus. |

## Open questions

- Copy and delete of slots in the UI? (item)
- Save how full each health tank is? (item)
- Confirm before overwriting a used slot? (new, see `NOTES.md`)

# Contract

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes (new `SaveSlotsTest`, `SaveFileJsonTest`, `FileSaveStorageTest`).
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` and `./gradlew lost-memories-desktop:jar` build.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

| Level | What to check |
|-------|---------------|
| Main menu, `./run-mega-man-maverick-alpha.sh --saveFile slots-test` | LOAD SAVE FILE opens the slot list; all 5 rows say EMPTY; picking one plays the error sound; BACK and the back key return to the main menu. |
| `INTRO_STAGE`, same launch | START NEW GAME, beat the intro stage, SAVE on the save screen: the list opens on slot 1; pick slot 2: ping, the row shows the difficulty and `1 LEVELS`; BACK; MAIN MENU; LOAD SAVE FILE, slot 2: the level select opens with the intro stage done. |
| Any robot master level after loading slot 2 | Beat it, SAVE: the list opens on slot 2 (the active slot); save there. `~/.megaman-maverick/saves/slots-test/` holds `slot-2.json` and `slot-2.json.bak`. |

## Behaviors to check

- `--difficulty hard` (or `--heartTanks all`): SAVE, pick a slot: error sound, the slot stays as it was.
- A slot file with `"version": 99` (edit it by hand): its row says NEWER VERSION; load and save on it play the error
  sound, and the file does not change.
- A slot file with a level name that does not exist: the slot loads, the other names stay; the log has an error line.
- Lost Memories (`./run-mega-man-x-lost-memories-alpha.sh`) starts as before (its title redirects the save and menu
  screens).

## Intended changes

- The save goes to 5 JSON slots in `~/.megaman-maverick/saves/` (desktop) instead of the XML prefs file. The old save is
  not loaded.
- SAVE and LOAD SAVE FILE open a slot list. The save list starts on the active slot (the last slot loaded or saved in
  this run; slot 1 after a new game or at boot).
- SAVE with `--heartTanks`, `--healthTanks` or `--difficulty` plays the error sound (it played the ping before, while
  it wrote nothing).
- `--saveFile <name>` names a subfolder of the saves folder, not a prefs file.
