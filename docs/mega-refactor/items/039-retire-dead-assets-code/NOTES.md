# Notes: 039 Retire unused assets and review dead code

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

Copied as written from the review section "ITEM 38: Retire dead assets and review dead code".

````text
(1) Current state
- Packr bundles all of `assets/*` (F8). Every TMX file is shipped, including the 69 that are not in `LevelDefinition` (87 files, 18 live), and the source art too.
- Unused music: 27 files, 37 MB by file name (F10).
- There are many code siblings, for example:
  - `RobotMasterIntroScreen` and `RobotMasterIntroScreenV2`: only the first is registered (`MegamanMaverickGame.kt:392-393`);
  - `LevelSelectScreen` and `LevelSelectScreenV2`;
  - `temp_BossIntroScreen.kt`;
  - `CartV2`, `DarknessV2`, `TubeBeamerV2`, `MegamanTrailSpriteV2`.

(2) Options for the asset location
A. `retired/assets/<original path>` at the repo root, outside `assets/`. Packr and Gradle never see it, and code cannot load it by mistake.
B. A separate branch or repo for retired files. Harder to try things with.
C. Leave files in `assets/` and filter them out of the packr bundle. The game can still load them, so it is easy to depend on them by mistake.

Options for code
a. Delete, and keep a list (`retired/CODE.md`) of class name, path and the last commit SHA where it existed. Restore with `git checkout <sha> -- <path>`.
b. A `retired/src/` folder that no source set compiles. It rots fast because the APIs change; it adds noise to search and the IDE; it gives a false sense that the code can be restored.
c. Keep the code in place with a `@NotYetUsed` annotation (for "not used yet").

(3) Recommendation
- Assets: option A, with the same path inside. Use `git mv`, so `git log --follow` keeps the history. To restore, do the reverse `git mv`.
- Keep the source art active and do not move it, because a move breaks the relative paths in `.tps` and TMX files. Instead, add a Gradle `Copy` task with an allow-list (`music`, `sounds`, `sprites/sprite_sheets`, live `tiled_maps`, the font, and so on). It builds a `dist-assets/` folder, and packr uses `--resources dist-assets/*`. This removes the source art from the shipped build without moving files.
- Code: verdict "truly dead" uses option a; "not used yet" uses option c; "older version of something used" uses option a with a link to the class that replaced it.
- Review sheet: a generated TSV or Markdown table from a new `utils/dead_code/` tool. Columns:
  - class, path, line count;
  - code references (excluding itself);
  - TMX references (object type or name in live and old maps);
  - factory or registry entry;
  - sibling names (`V2`, `_v2`, `temp_`, `Old`);
  - last commit date and message;
  - suggested category;
  - owner verdict (`delete` / `retire` / `keep-not-yet` / `keep-alternative`), and notes.
  A second sheet does the same for assets: path, size, enum references, TMX references, `.tps` references, last commit.
  A small apply step reads the verdict column and does the `git mv` or delete in one commit per category.

(4) Open questions for the owner
- Do old TMX maps go to `retired/` or stay in `assets/tiled_maps/` but outside the shipped list? Moving them can break their relative tileset paths.
- Is git history enough for retired code, or do you also want the files on disk?

(5) Proposed backlog wording
Title: Retire unused assets and review dead code
- `retired/` at the repo root, with the same paths inside; use `git mv` so the history follows; never bundled.
- A Gradle staging task with an allow-list for packr, so source art and unlisted maps are not shipped.
- `utils/dead_code` generates review sheets (code and assets) with evidence and a verdict column.
- Apply the verdicts: delete (with a SHA index), retire, or mark `@NotYetUsed`.

(6) Dependencies: 20 and 24 (registry and manifest make "referenced" exact), 2 (the validator shares the TMX scan), 32 (asset budget), 39 (stale `.tps` files), 42 (smaller working tree).
````
