# Notes: 007 Versioned JSON save slots with legacy import

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Tooling, saves, settings, logging, profiling and asset hygiene, new facts F1-F11 (2026-10-22)

````text
REVIEW: items 6, 7, 8, 33, 38, 39, 42, 43 (tooling, saves/settings, logging, profiling, asset hygiene)

Scope: I only read files. I edited nothing and did not run the game or the build. The only tool I ran was `TexturePacker --version` (7.9.0 is installed at /usr/bin).

New facts found during this review (verified):
- F1. Saved keyboard bindings are not applied at boot. `MegamanMaverickGame.kt:261` calls `loadControllerButtons()`, which uses only the default keys (`ControllerUtilsExtensions.kt:17-35`). The saved keys are read only when the player selects "load saved settings" (`ControllerSettingsScreen.kt:185`). Controller prefs load on connect only after that screen sets `loadPrefsOnConnect = true` (`ControllerSettingsScreen.kt:224`, `MegaControllerPoller.kt:30`).
- F2. `selectButtonAction` resets to `NEXT_WEAPON` at every boot (`MegamanMaverickGame.kt:263`), but the value is saved in prefs.
- F3. Volume can change in game (`GameSettingsPanel.kt:177-215`), but it is not saved. The CLI default is 0.8 (`DesktopLauncher.kt:38-39`) and the params default is 0.5 (`MegamanMaverickGame.kt:126-127`).
- F4. The save keeps only which health tanks you have, not how full they are (`GameState.kt:273`). Load puts them back empty (`putHealthTank` default `MIN_HEALTH`, `GameState.kt:85`).
- F5. `DesktopLauncher.kt` (the `logLevels` parse) has the same bug as `GameState.kt:293`: `replace("\\s+", "")` treats the pattern as literal text, not a regex.
- F6. `TAGS_TO_LOG` is an empty set in code (`MegamanMaverickGame.kt:166`), and `filterByTag = true` (`:253`). So no debug line prints unless you edit code, but all the debug strings are still built.
- F7. Dev runs (`./gradlew lwjgl3:run`) use the default GC (G1). Release builds use ZGC (`build-game.sh:86` `--useZgcIfSupportedOs`, `--vmargs Xms512M Xmx1024M`) on JRE 17. Dev and release measure different collectors.
- F8. Packr bundles all of `assets/*` (`build-game.sh:92`). This includes `sprites/frames` (16 MB), `piskel` (4.2 MB), `texture_packer`, all 87 TMX files (18 live, `LevelDefinition.kt`) and the text notes. The JAR leaves out music, sprites and maps (`lwjgl3/build.gradle:59-77`), so the size problem is in the packr bundle, not in the JAR.
- F9. 12 of 86 `.tps` files point to source folders that do not exist: `../Frames/...` and `../SpriteSheets/...` (wrong case), `../frames/Bosses`, and another repo (`RocketPartnersGame`). Examples: `FreezerMan.tps`, `MegamanMaverick.tps`, `AstroPlayer.tps`. 3 atlases have no `.tps`: `FridgeMan`, `MegamanChargedShot`, `MegamanMaverick_BUSTER`.
- F10. Music check: 27 files (37 MB) in `assets/music` have no name in `MusicAsset.kt`. The brief says ~83 MB. That number probably also counts enum entries that no code uses. Check before you write the number in the backlog.
- F11. `WILY_CASTLE_SCREEN` is in `ScreenEnum` but is not registered in the screen map (`MegamanMaverickGame.kt:381-398`).
- Note: the project memory says "no tests in core". That is out of date: `core/src/test` has tests, for example `MegaControllerPollerTest`.
````

## Review: Tooling, saves, settings, logging, profiling and asset hygiene (2026-10-22)

Copied as written from the review section "ITEM 6: Versioned JSON save, multiple slots, compatibility with the XML prefs file".

````text
(1) Current state
- `GameState.toString()` (`GameState.kt:264-288`) writes 6 fields separated by `;`: levels defeated, heart tanks, health tanks (names only), enhancements, currency, difficulty. Lists inside a field use `,`. The format has no version number.
- `fromString()` (`:290-351`) reads the fields by position. It logs and drops names it does not know. If a field is missing, it uses the default.
- Storage: `saveState()`, `hasSavedState()` and `loadSavedState()` (`MegamanMaverickGame.kt:753-790`) use `Gdx.app.getPreferences("Megaman Maverick - Save File")` with the key `state`. On desktop, LibGDX writes this as XML at `~/.prefs/Megaman Maverick - Save File`. The real file on this machine contains `INTRO_STAGE,INFERNO_MAN;D;;;0;HARD`. It saves in `SaveGameScreen.kt:75` and loads in `MainMenuScreen.kt:149`.
- There is one slot only.

(2) Options
A. Store the JSON as a string inside LibGDX Preferences (one key per slot).
   Pro: works the same on every platform (on web, Preferences uses localStorage).
   Con: the JSON stays inside XML on desktop, which the owner wants to stop. Hard to read and to back up.
B. One JSON file per slot through `Gdx.files.external` or `Gdx.files.local`.
   Pro: easy to read, diff and back up.
   Con: no file system on web (GWT/TeaVM). On desktop, `local` is the install folder, which can be read-only (Program Files, macOS .app).
C. A small `SaveStorage` interface in core: `read(name)`, `write(name, text)`, `list()`, `delete(name)`. Each platform has a backend:
   - desktop: files in a user folder through `Gdx.files.external`, next to `~/.prefs`, for example `~/.megaman-maverick/` (or the OS app-data folder);
   - Android: `Gdx.files.local` (private app storage, no permission needed);
   - web: Preferences/localStorage, one key per file name.
   Pro: real JSON files where a file system exists, and web still works. Tests can use an in-memory backend.
   Con: a little more code.

(3) Recommendation: option C.
- File layout: a fixed number of slots, `saves/slot-1.json` to `saves/slot-3.json`, and a separate `settings.json` (item 33). You do not need an index file. Keep "last used slot" in `settings.json`. `list()` finds the slots that exist.
- Write each file safely: write `slot-N.json.tmp`, rename it over the real file, and keep `slot-N.json.bak`.
- Schema v1:
  `{ "version": 1, "gameVersion": "1.0.1", "savedAt": "<ISO time>", "progress": { "levelsDefeated": [...], "heartTanks": [...], "healthTanks": { "A": 0, ... }, "enhancements": [...], "currency": 0, "difficulty": "HARD" }, "unknown": { ... } }`
  The `healthTanks` map can store how full each tank is (F4), if the owner wants that. When a name does not match an enum, keep the raw name in `unknown` and do not drop it. Then a retired level does not silently disappear from a save.
- Migration chain: a list of steps, each `from -> to`, that work on the raw JSON tree (LibGDX `JsonValue`).
  - v0 is the legacy positional string. A frozen copy of the current parser (`LegacySaveV0`) turns it into a v1 tree. Do not call `GameState.fromString` for this, because that code will change later.
  - On load: read `version`, then run each step until the file is at the current version, then fill `GameState`.
  - If `version` is newer than the game knows, refuse to load and never overwrite the file.
  - Fix the `\\s+` bug in the v0 parser (`GameState.kt:293`).
- Legacy import: at boot, if no slot files exist and the legacy prefs contain `state`, import that string into slot 1 and write the slot file. Do not delete, rename or change the legacy XML file. This follows the owner's rule to keep data. Record `"importedFrom": "prefs:Megaman Maverick - Save File"` in the slot file so the import runs only once. A "re-import legacy save" debug option is cheap to add.
- Compatibility test (`core/src/test`, plain unit test, no GL needed):
  - fixture strings: the real file `INTRO_STAGE,INFERNO_MAN;D;;;0;HARD`; an empty string; an old 5-field string with no difficulty; a string with an unknown enum name; a string with spaces; a string that uses every `LevelDefinition`, heart tank and enhancement name;
  - golden JSON files in `core/src/test/resources/saves/v1/*.json`, which you keep for ever and add to for each new version;
  - checks: v0 to current gives the expected `GameState`; current to JSON to current gives the same state (round trip); a newer version is refused.

(4) Open questions for the owner
- How many slots? Copy and delete slot in the UI?
- Save how full each health tank is (F4)? Save more, for example lives or weapons?
- The desktop folder name and place: `~/.megaman-maverick/` or the OS app-data folder?
- Keep the legacy file for ever, or offer a "clean up old save" option later?

(5) Proposed backlog wording
Title: Versioned JSON save slots with legacy import
- `SaveStorage` interface with desktop (files), Android (local) and web (Preferences) backends.
- Fixed slots `saves/slot-N.json`; schema with `version`; safe write with `.tmp` and `.bak`; keep unknown names.
- Migration chain v0 (legacy prefs string) to v1; refuse versions newer than the game.
- Import the legacy prefs save into slot 1 once; never change or delete the XML file.
- Compatibility tests with fixture strings and golden JSON files.

(6) Dependencies: 33 (shared storage and settings file), 1 (CI runs the tests), 7 (`--slot`/`--state`), 31 (platform-neutral storage), 24 (a level manifest makes stable level IDs possible later).
````
