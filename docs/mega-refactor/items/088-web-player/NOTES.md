# Notes: 088 Web player (TeaVM Wasm)

Findings of 2026-10-31. Maverick facts are from `master` at `77cf2c679`. X6 facts are from `~/IdeaProjects/mmx-x6` at
`4b2b197`, mainly `docs/superpowers/status/user-levels.md`, "Facts from Task 1", "The movement match" and "Rulings in
plan 2".

## Backend options

| Option | Result for Maverick |
|---|---|
| GWT (`gdx-backend-gwt`) | Not possible. GWT compiles Java source. Maverick has 1027 Kotlin files and 0 Java files. X6 also found that `gdx-backend-gwt` 1.14.2 uses GWT 2.11 (Java 11 syntax only) and runs `float` as a JS double, so physics drifts. |
| Kotlin/JS or Kotlin/Wasm | Not practical. LibGDX is a JVM library, and these targets cannot use it. |
| TeaVM JS target | Compiles bytecode, but runs `float` as a JS double (X6, plan 2 research). Rejected in X6 for this reason. |
| TeaVM Wasm-GC target | Compiles JVM bytecode, so Kotlin is not a problem. `float` maps to `f32`. X6 uses it, and the probe below shows that it builds Maverick. **Recommended.** |

## What X6 did

Versions: `gdx-teavm` 1.6.2 (the newest on Maven Central on 2026-10-31), TeaVM 0.15.0, LibGDX 1.14.2. These are the same
LibGDX version as Maverick.

- **Module split:** `core`, `lwjgl3`, `teavm`. Tests stay on the JVM. Commit `edb6af9`.
- **Platform seam:** `Platform` (exit, shots, pads, debug key, sound loader, level file, web requests) and `LaunchOptions`
  (Java properties on the desktop, the URL query on the web). `DesktopPlatform` and `WebPlatform` implement it.
- **Asset reads:** all through `AssetSource` with string paths. `SourceRulesTest` bans `java.nio.file`, `java.awt`,
  `String.format`, `System.getProperty`, `java.util.Random` and `Math.round` in `core`.
- **Determinism gate first:** `make web-probe` compares `float`, cast, `Random` and `Math` results between the JVM and
  both browsers. It found two TeaVM differences: `Math.round` rounds half away from zero, and `Random.nextInt(bound)` has
  another rejection rule. X6 added `X6Random` and `X6Math.round`.
- **Movement match:** `make web-trace` runs four scripted inputs on the desktop and in Chromium and Firefox, and compares
  the player and camera values as float bits for each update. All eight browser runs gave `MATCH`.
- **Web asset set:** `tools/web_assets.py` copies only the intro assets and repacks strips over 4096 px: 2546 files,
  28.7 MB. The output is 34 MB of assets and a 1.1 MB `app.wasm`.
- **Sounds:** per-clip pitch (`PitchedSounds`), and level banks load before the level starts. `HowlMusic` reads the whole
  music file into memory.
- **Page:** own `index.html` with a browser check (`browser-check.js`). Safari and all iOS and iPadOS browsers are blocked.
  OGG Vorbis and Wasm-GC must be present.
- **Limits:** keyboard only, localhost only, intro only, no error screen for an exception during play.
- **Headless runs:** `tools/web/console.sh` runs Flatpak Chromium or Firefox and prints the page console.
- **Build detail:** `outOfProcess = false`, because the TeaVM daemon failed with `NoRouteToHostException`.

## Probe of Maverick

Method: a throwaway copy of `master` in the session scratchpad, with a `teavm` module on the X6 versions, and
`MegamanMaverickGame` as the application. Nothing from the probe is in this repo. Each step fixed the failure of the step
before it. The patches are stubs, not proposed fixes.

Gradle 8.10.1 works with the plugin (X6 uses 9.1.0). The JDK must include `javac`; the local JDK 17 does not, so the
probe used JDK 23.

| # | Failure | Where | Probe patch | Real fix |
|---|---|---|---|---|
| 1 | `javax.swing.JFrame` not found | `DebugWindow` | empty class | Move it behind a platform interface. |
| 2 | `Executors`, `ExecutorService`, `Future` not found | `AsyncPathfindingSystem`, `AsyncFileWriter` | stubs, `sync` pathfinding | `032`. `SimplePathfindingSystem` exists. See "Pathfinding". |
| 3 | `BufferUtils.isUnsafeByteBuffer`, `RandomAccessFile.getChannel` not found | FreeType, reached through `BitmapFontHandle` | `gdx-freetype-web` | Add the dependency, and load `scripts/freetype.js` in the page. The generated page does not load it. |
| 4 | TeaVM crash in `CoroutineTransformation` (`Integer.parseIntImpl`, `Long.parseLongImpl`) | Blocking calls in reachable code | Removed the thread and the blocking queue calls from `RuntimeDiagnostics`, and the thread from `DarknessV4` | `032`. See below. |
| 5 | Chromium rejects `app.wasm`: `expected 1 elements on the stack for fallthru` | `FacingUtils.getPreferredFacingFor` | Removed the `try`/`catch` | Change the shape of the function, but keep the fallback. See below. |
| 6 | `ClassNotFoundException GwtControllers` | `Controllers` | `gdx-controllers-web` | Add the dependency. No real gamepad was tested. |
| 7 | `KotlinReflectionNotSupportedError` | `MegaEntityFactory` (`primaryConstructor`) | Generated a registry of 430 constructors | `021`. |

Notes on these failures:

- **Failure 4:** at the crash, `DarknessV4` was not reachable, because nothing refers to it. So `RuntimeDiagnostics` is the
  likely trigger. This is a deduction; the probe patched both in one step. No code outside `DarknessV4.kt` and no TMX file
  names `DarknessV4`, so `039` can probably delete it.
- **Failure 5:** the function has a `try` and a `catch` that both `return`. The source has 73 `try` blocks, but only this
  function failed in the build with 430 entities. The `catch` is real behavior: it handles a facing that is not set yet.
  So a fix must keep it. Optimization level `NONE` gives the same failure. Report this to TeaVM with a small test case.
- **`Class.forName`:** TeaVM includes only the classes that it can reach. So `MegaEntityFactory` and `SensorsLayerBuilder`
  cannot find an entity by name unless a registry refers to it (`021`).
- **`kotlin.reflect.full`:** used in `MegaEntityFactory` and `GameObjectPools`. Plain `KClass` map keys and `simpleName`
  ran with no error.

## Result of the probe

With these patches and `LaunchOptions(level = "DESERT_MAN")`, headless Chromium 156 ran for 90 s:

- The level screen was active within 33 s of the game start (localhost). The log showed about 60 fps after that.
- 5 spawner errors (`Cactus` 4, `Bat` 1), because the generated registry missed these classes. No other error.
- `app.wasm` with 430 entities: 7.31 MB, 2.41 MB with `gzip -9`. `scripts/` adds 1.3 MB. The Gradle build took 70 s to
  94 s with Kotlin up to date.

**Not proven:** no screenshot was taken, no input was sent, no sound was heard, and the probe did not check that Megaman
spawned. The 60 fps number is from a headless run with no player actions, so it says nothing about weak hardware.
Firefox was not run.

## Obstacles that remain

- **Assets:** the probe output is 255 MB. Audio is 201 MB (179 OGG, 25 MP3); the largest file is 7 MB.
  `MegamanMaverickGame.queueAssets()` queues all music, sounds, textures and levels at startup. The live maps are 23 files
  of 7.1 MB, out of 92 TMX files. The audio that levels really use is not measured.
- **Textures:** 12 PNG files are over 4096 px, 5 of them over 8192 px. Only 3 are live tilesets:
  `tomahawk_man_stage_tileset.png` (8196), `GlacierMan.png` (6144), `super_mario_bros_3_world_1_stage_1.png` (5632). 4096 px
  is X6's limit; the limit that Maverick needs is not measured.
- **Same play:** Maverick uses a variable frame delta and an unseeded `Random(System.currentTimeMillis())`
  (`UtilMethods.kt:22`, `ArrayExtensions.kt:112`, `ObjectSetExtensions.kt:84`). `Random` or `Math.round` related calls: 159
  hits, not sorted. An X6-style trace compare needs `011` and a fixed tick or a recorded delta (`010`, `055`).
- **Frame rate:** the probe ran `Performance.VERY_LOW` (30 fps), but the log showed 60 fps. So `setForegroundFPS` may do
  nothing on the web, and `maxIterations` in `WorldSystem` assumes the configured rate. Check this, also on a 144 Hz display.
- **Pathfinding:** the desktop default is `async`. On the web it is `sync`, on the main thread. Enemy timing can change and
  long searches can drop frames. No threads exist in the web build.
- **Saves:** `Preferences` compiled and ran through `WebPreferences`. Browser storage can be cleared; `007` and `034` are
  related.
- **Dev tools:** `RuntimeDiagnostics` and `AsyncFileWriter` file output have no web target (open question in `032`).
- **Errors:** an exception during play freezes the canvas (X6 has the same limit).
- **Legal:** the game is a non-commercial fan game with Capcom-derived content. A public URL differs from a download.
  This is a decision for the owner, not a technical finding.

## Effect on other items

- `021` removes failure 7. It also puts every entity that a map names by string into the web build.
- `032` removes failures 1, 2 and 4. If this item is accepted, web is a real target, which answers the open question in
  `032`, and the TeaVM compile check can go in CI. The `032` note says that `Controllers` is fine as it is. On the web, this
  is true only with `gdx-controllers-web`.
- `033` helps the asset work but is not needed for the first local build.
- `035`: not needed for the web. Decision J keeps 16×14.
