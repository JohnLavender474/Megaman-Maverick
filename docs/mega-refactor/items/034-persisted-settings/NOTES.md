# Notes: 034 Persisted settings file

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

Copied as written from the review section "ITEM 33: Persisted settings, not CLI flags".

````text
(1) Current state
- Volume, performance, pixel-perfect, vsync and fullscreen come from the CLI only (`DesktopLauncher.kt`).
- Volume changes in game are lost when you quit (F3).
- Key bindings are in two kinds of prefs files: keyboard, and one file per controller name (`ControllerUtilsExtensions.kt:11-15`). Saved keyboard keys and the select action are not applied at boot (F1, F2).

(2) Options
A. Keep Preferences for settings and add keys.
B. A `settings.json` through the same `SaveStorage` as item 6, with its own `version` and migration from the 2 to N legacy prefs files.

(3) Recommendation: option B.
- Contents: audio volumes; video (fullscreen, window size, pixel-perfect, vsync); performance tier and auto-perf; keyboard bindings; controller bindings keyed by controller name; select-button action; last used slot.
- Order of precedence: defaults, then `settings.json`, then CLI flags. CLI values apply to that run only and are not saved.
- Import the legacy keyboard and controller prefs once, and leave the old files unchanged.
- Apply the saved bindings at boot. This fixes F1 and F2. Ask the owner first in case the current behavior is on purpose.

(4) Open questions for the owner
- Is the "load saved settings" button (F1) on purpose, or should saved bindings always apply at boot?
- Which values does the player change in game, and which are dev-only?

(5) Proposed backlog wording
Title: Persisted settings file
- `settings.json` with `version`, stored through `SaveStorage`.
- Order: defaults, then file, then CLI (CLI values are not saved).
- Import the legacy keyboard and controller prefs once.
- Apply the saved bindings and select action at boot; save volume changes.

(6) Dependencies: 6 (storage, migration pattern), 7, 30 (bindings per player), 31.
````
