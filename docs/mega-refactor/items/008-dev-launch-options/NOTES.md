# Notes: 008 Dev launch options and level staging

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

Copied as written from the review section "ITEM 7: Dev launch options, level staging, TMX hot reload".

````text
(1) Current state
- `DesktopLauncher.DesktopAppArgs` (JCommander) has `--runType game|test`, `--performance`, window, debug, volume, `--diagnostics`, `--worldContainer` and `--pathfinding`. You cannot choose a screen or a level.
- `--runType test` hard-codes `startLevel(LevelDefinition.TEST_1)` (`MegamanMaverickGame.kt:400-403`). Otherwise the game starts at `SIMPLE_INIT_GAME_SCREEN`.
- The staged level already exists. It is the game property `LEVEL_DEF`: `setCurrentLevel` and `getCurrentLevel` (`MegamanMaverickGame.kt:708-710`, with a `!!` that crashes if no level is set). `startLevel(null)` uses it (`:896-908`).
- Code that writes the staged level: `LevelSelectScreenV2.kt:248`, `LevelSelectScreen.kt:382`, `MainMenuScreen.kt:156` (when the intro stage is not yet beaten), `startLevel(level)` and `startExtraLevel`.
- All maps load at boot through the AssetManager (`MegamanMaverickGame.kt:801-806`). `TiledMapLoader.load` only fetches a map that is already loaded (`TiledMapLoader.kt:24-26`).
- `lwjgl3:run` uses `workingDir = assets` (`lwjgl3/build.gradle:53`), so dev runs read the source TMX files.
- Auto-perf always runs and has no flag (`MegamanMaverickGame.kt:452-480`).
- Log tags are fixed in code (F6).
- Volume, performance and similar values come only from the CLI.

(2) Options
A. Add more JCommander fields and read them directly in `MegamanMaverickGame`.
   Pro: small change. Con: core depends on desktop-only ideas, and it is hard to test.
B. A platform-neutral `LaunchOptions` data class in core (or engine), next to `MegamanMaverickGameParams` or inside it. The desktop launcher fills it from JCommander. Android and web pass the defaults, or Android can fill it from intent extras later.
   Pro: one place to check the values, easy to unit test, follows item 31.
   Con: one more class.

(3) Recommendation: option B.

Arguments:
- `--screen <ScreenEnum>`: case-insensitive. Default `SIMPLE_INIT_GAME_SCREEN`, or `LEVEL_SCREEN` if `--level` or `--tmx` is given and `--screen` is not.
- `--level <LevelDefinition>`
- `--tmx <path>`: a map that is not in `LevelDefinition`. It replaces the map source. Music, gravity checks and similar data come from `--level` if given, else from `TEST_1`.
- `--room <name>` and `--checkpoint <spawnName>`
- `--slot <n>` or `--state <string|file>`: legacy string or JSON fixture. You can use one of these, not both.
- `--difficulty <DifficultyMode>`: applied after the state loads.
- `--logTags a,b,c` (fills `GameLogger.tagsToLog`) and `--logTags all` (sets `filterByTag = false`)
- `--strictErrors`: errors and skipped TMX objects stop the game (item 2).
- `--autoPerf true|false`
- `--seed <long>` (item 10)
- `--hotReload` (see below)
- Keep `--runType test` as an alias for `--level TEST_1` and mark it deprecated.

Staging rules (these match the owner's text):
- `--level L` with no `--screen`: call `startLevel(L)`.
- `--screen S` (not `LEVEL_SCREEN`) with `--level L`: call `setCurrentLevel(L)`, then `setCurrentScreen(S)`. The next `startLevel()` with no argument loads L, unless code has called `setCurrentLevel` again (level select, main-menu load, `startLevel(x)`). This is the "persist until overwritten" rule that X6 and Maverick already use.
- `--tmx` stages a level in the same way. To do this, the staged value must become a small `StagedLevel(def, tmxOverride?)` and not only the enum.
- `--room` and `--checkpoint` are used one time only: on the first level start after boot, then cleared. If they stayed staged for ever, every death or later level would jump to that room. Error if they are given with no level.
- `--screen LEVEL_SCREEN` with no level: error.

Checks (exit code 2, and print the usage):
- Unknown enum value: "Invalid --level 'MOONMAN'. Did you mean MOON_MAN? Allowed: ..." List all values and suggest the nearest name.
- `WILY_CASTLE_SCREEN` (F11): "Screen WILY_CASTLE_SCREEN is not registered."
- `--tmx` file not found: show the full path that was tried.
- `--room` not found after the map loads: show the room names in that map. In strict mode this stops the game; otherwise it logs an error and uses the normal spawn.
- `--slot` and `--state` together: error.
- Also fix the `\\s+` parse bug (F5).

TMX hot reload: possible in steps.
- Step 1: a debug key that unloads the map from the AssetManager, loads it again from disk (`assMan.unload`, `load`, `finishLoadingAsset`) and restarts the level at the current room or checkpoint. Dev runs read `assets/` directly, so a Tiled save shows after one key press.
- Step 2: a desktop-only file watcher (`java.nio.file.WatchService` on a daemon thread) that sets a flag. The render thread reads the flag and starts step 1.
- Do not try to patch the running world in place.
- Risk: a clean restart needs a clean teardown of pools, listeners and backgrounds. That is items 12, 36 and 37. Until then, a reload can leave old state behind.
- `--tmx` with a path outside `assets/` needs a direct `TmxMapLoader` call, because the AssetManager uses `InternalFileHandleResolver`.

(4) Open questions for the owner
- Should `--room` also stay staged, or is one time correct (my recommendation)?
- Should `--state` and `--slot` write back to the slot when the game saves, or should a dev run never save (safer)?
- Which screens are valid start points? Some screens need props from the screen before them (for example `ROBOT_MASTER_INTRO_SCREEN` reads the current level).

(5) Proposed backlog wording
Title: Dev launch options and level staging
- Platform-neutral `LaunchOptions` filled by `DesktopLauncher`; checks with clear errors and exit code 2.
- `--screen`, `--level`, `--tmx`, `--room`/`--checkpoint`, `--slot`/`--state`, `--difficulty`, `--logTags`, `--strictErrors`, `--autoPerf`, `--seed`.
- Staging: a non-level screen with a level stages it until overwritten; `--room` is one time only.
- Deprecate `--runType test`; fix the `logLevels` parse bug.
- TMX hot reload: a reload-and-restart key first, then a desktop file watcher.

(6) Dependencies: 6 (`--slot`), 10 (`--seed`), 2 (`--strictErrors`), 43 (`--logTags`), 35 (named checkpoints for `--checkpoint`), 12/36/37 (clean restart for hot reload), 31, 33.
````
