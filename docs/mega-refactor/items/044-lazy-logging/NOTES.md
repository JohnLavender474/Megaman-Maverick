# Notes: 044 Lazy debug and log strings

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

Copied as written from the review section "ITEM 43: Lazy debug log strings".

````text
(1) Current state
- `GameLogger` (`engine/.../common/GameLogger.kt`) takes a finished `String`. Kotlin builds the template string (`"x=$x"`) before the call, and only then does `print` check the level and tag filter (`GameLogger.kt:69-72`).
- Count in main sources: 798 `debug` and 4 `log` calls with a template on the same line, which matches the 802 in the brief. Another 137 calls continue onto the next line. Errors (65) always print, so laziness does not help them.
- With the empty `TAGS_TO_LOG` (F6), almost all of these strings are built and then thrown away.
- Other costs: the level check uses an `ObjectMap<GameLogLevel, Boolean>` lookup (`:46`), and each printed line creates a new `DateTimeFormatter` (`:76`).
- Some code already checks the filter by hand (`GameObjectPools.kt:131,142`).

(2) How the fix works
Add `inline` overloads that take a lambda:
```kotlin
inline fun debug(tag: String, message: () -> String) {
    if (isEnabled(GameLogLevel.DEBUG, tag)) print(GameLogLevel.DEBUG, tag, message())
}
inline fun debug(tag: String, t: Throwable?, message: () -> String) { ... }

@PublishedApi internal fun isEnabled(level: GameLogLevel, tag: String): Boolean
```
- Because the function is `inline`, the compiler copies its body into each caller. The lambda body goes inside the `if`, so no `Function` object is created and the string is built only when the line will print.
- A lambda that is not inlined would allocate an object at each call when it captures variables. That is why `inline` is required.
- An inline function cannot use private members, so `isEnabled` and `print` must be `@PublishedApi internal` (this works across the engine/core module boundary) or public.
- Call shape: `GameLogger.debug(TAG) { "fromString(): s=$s" }`.

Options to change the call sites
A. IDE: put `@Deprecated(message = "Use the lambda form", replaceWith = ReplaceWith("GameLogger.debug(tag) { message }"))` on the `String` overloads. Then use IntelliJ "Replace usages of deprecated ..." for the whole project.
   Pro: understands the syntax, handles multi-line calls. Con: the throwable versions need their own `ReplaceWith`. `GameLogger` implements LibGDX `ApplicationLogger`, so keep the `String` overrides; they can be deprecated.
B. A script (regex): `GameLogger.(debug|log)\((\w+), (".*")\)$` becomes `GameLogger.$1($2) { $3 }`, plus a pattern for the throwable form. It handles the 802 single-line calls; do the 137 multi-line calls by hand or with option A.
   Pro: fast. Con: regex can fail on strings that contain `)` or quotes. Review the diff.
C. Use the lambda only for calls with a template, and leave constant strings. A constant literal costs nothing, but mixing two forms is less clear.

(3) Recommendation
- Option A (with B as the fallback), and change all `debug` and `log` calls for one consistent form.
- Keep the deprecated `String` overloads until the change is done, then add a CI grep or detekt rule that blocks new eager template calls to `debug` and `log`.
- Leave `error` eager.
- Make the filter fast:
  - a `BooleanArray` indexed by `level.ordinal`, or `@JvmField var debugOn` (no boxing);
  - check the level before the tag set;
  - `ObjectSet<String>.contains` is cheap because Java strings cache their hash;
  - a `@JvmField var logAllTags` for `--logTags all`;
  - make the `DateTimeFormatter` a constant.
- Replace the hand checks in `GameObjectPools.kt` with `GameLogger.isEnabled`.
- Measure the gain with item 8 (bytes per frame before and after).
- This is one large, mechanical commit. Do it on its own branch, with a compile check and a short play test.

(4) Open questions for the owner
- Change all calls, or only calls with templates?
- Should `--logTags` (item 7) replace the code-level `TAGS_TO_LOG`?

(5) Proposed backlog wording
Title: Lazy debug and log strings
- `inline` lambda overloads for `debug` and `log` (with and without throwable); `@PublishedApi` filter check.
- Faster filter: a level array (no boxing), level checked before tag, constant time formatter.
- Change about 939 call sites with IDE `ReplaceWith` (or a script for single-line calls); deprecate the `String` overloads.
- A CI check against new eager template calls; measure with the item 8 allocation counter.

(6) Dependencies: 8 (measure the gain), 7 (`--logTags`), 1 (CI check).
````
