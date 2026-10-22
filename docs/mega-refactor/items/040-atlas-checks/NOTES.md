# Notes: 040 Atlas checks and one-command repack (keep TexturePacker)

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

Copied as written from the review section "ITEM 39: Atlas build (Gradle packing compared to TexturePacker)".

````text
(1) Current state
- 86 `.tps` projects in `assets/sprites/texture_packer/` (libgdx format, Basic algorithm, no trim, max 2048 in `Enemies1.tps`).
- The output `.png` and `.txt` files are committed in `sprites/sprite_sheets/` and named in `TextureAsset.kt` (66 entries).
- You pack by hand in the GUI.
- 12 `.tps` files are stale and 3 atlases have no project (F9).
- Nothing checks that the regions code asks for (561 `findRegion` calls) exist.

(2) Options
A. Keep TexturePacker as it is. Pro: no work, a known look. Con: the stale projects and missing checks stay.
B. Change to the LibGDX `TexturePacker` from gdx-tools in a Gradle task.
   - Pro: free, so other people can rebuild; it can run in CI; packing is in code review.
   - Con: a different packing algorithm repacks every page, which gives large binary diffs and a risk to the look. Animation index naming (`_1` suffix) and padding can differ, so check every page visually. The pro features you paid for are lost.
C. Keep TexturePacker and add checks that need no license:
   - a check (Kotlin test or Python in `utils/`) that every `findRegion` name exists in its atlas;
   - every `.tps` source folder exists;
   - every atlas has a `.tps`;
   - page size is not more than 2048;
   - `TextureAsset` entries match the files.
   Also a local `utils/atlas/pack-all.sh` that runs the TexturePacker CLI (installed, 7.9.0) on all `.tps` files, so a rebuild is one command.
D. Option C, plus the TexturePacker CLI in CI to check that the committed output matches the projects. This needs a license key on the CI runner (check CodeAndWeb's license terms). Output can also change between TexturePacker versions.

(3) Recommendation: option C.
- Most of the value of a Gradle task is the checks and the one-command rebuild. Option C gives both, keeps your tool and avoids a full repack.
- Fix the 12 stale `.tps` files first (wrong-case paths), and add projects for the 3 orphan atlases.
- Change to option B only if other contributors must edit art.
- "Animation definitions as data" is a separate piece of item 39 and does not depend on the packer.

(4) Open questions for the owner
- Will other people edit art?
- Are the 12 stale projects (for example `AstroPlayer`, which points to `RocketPartnersGame`) still needed, or should they go to `retired/`?

(5) Proposed backlog wording
Title: Atlas checks and one-command repack (keep TexturePacker)
- A check that every `findRegion` name exists in its atlas (CI, no license needed).
- A check of `.tps` hygiene: source folders exist, every atlas has a project, pages are not more than 2048.
- `utils/atlas/pack-all.sh` that uses the TexturePacker CLI.
- Fix the 12 stale `.tps` files and add the 3 missing ones.
- Optional: animation definitions as data.

(6) Dependencies: 3 (the smoke test also finds missing regions), 27 (texture cache), 32 (page budgets), 38, 1.
````
