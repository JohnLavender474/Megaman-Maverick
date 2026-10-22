# Notes: 009 Allocation and GC baseline per branch

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

Copied as written from the review section "ITEM 8: Per-branch performance baseline (allocations and GC) with low overhead".

````text
(1) Current state
- `RuntimeDiagnostics` (`engine/.../diagnostics/RuntimeDiagnostics.kt`) records only a timing tree per frame. It writes on a background thread through a 256-slot queue and drops frames when the queue is full.
- It allocates one `String` per frame itself (in `endFrame`).
- The frame starts at `MegamanMaverickGame.kt:407` and ends at `:589`.
- `utils/diagnostics/analyze.py` and the `diagnostics-analyzer` skill read this format.
- No allocation or GC data is recorded.
- The collector differs between dev (G1) and release (ZGC, JDK 17, not the generational kind) (F7).

(2) Options and cost (the costs are from general JVM knowledge; measure them here before you depend on them)
A. In-process counters, written in the frame header line:
   - `com.sun.management.ThreadMXBean.getCurrentThreadAllocatedBytes()`, read at frame start and before serialization. This gives the bytes allocated by the render thread in that frame. Cost: about tens of ns, no allocation.
   - `GarbageCollectorMXBean.getCollectionCount()` and `getCollectionTime()`, summed over all beans, as the change since the last frame. Cost: low, a few calls per frame. The bean names differ by collector, so write them once in the file header.
   Pro: almost free; a row per frame lines up with the current timing tree and rooms. Con: no stack traces; HotSpot only (not Android).
B. JFR (Java Flight Recorder), built into JDK 17: `-XX:StartFlightRecording=filename=run.jfr,settings=profile`.
   - Built-in events: `jdk.GarbageCollection`, `jdk.GCPhasePause`, `jdk.GCHeapSummary`, and `jdk.ObjectAllocationSample` (rate-limited, with stack traces, so it shows which code allocates).
   - A custom `jdk.jfr.Event` per frame or per room change (level, room, frame time) ties GC events to the game.
   - Cost: usually about 1% with the default settings and about 2% with `profile`. It records on another thread.
   - Read it with JDK Mission Control or the `jfr print --json` CLI, which a script can read.
   - Con: the release JRE may not include the `jdk.jfr` module. Check this.
C. async-profiler with `-e alloc`: allocation flame graphs, low cost, Linux and macOS only. Best for "which line allocates". Use it as needed; it does not give a baseline.
D. `-Xlog:gc*:file=gc.log:uptime,level,tags` (add `-Xlog:async` on 17): almost free. Gives pause times and heap before and after. Does not show which code allocates.
E. Avoid: agents that count each allocation, or turning on `-verbose`-style output on the render thread. The cost is high and changes the result.

(3) Recommendation
- Level 1 (always available with `--diagnostics`): option A.
  - Add `alloc=<bytes> gc=+<count> gcMs=+<ms>` to the `=== Frame #N` header line.
  - Read the allocation counter before `endFrame` builds its string, so the tool does not count its own allocation.
  - Put the probe behind a small engine interface (`MemoryProbe`) with a desktop version and a no-op version, so the engine stays platform-neutral (item 31).
- Level 2 (opt-in): a `--jfr` option in the run scripts that adds `-XX:StartFlightRecording`, and a custom frame/room event. Add `-Xlog:gc` beside it.
- async-profiler only for one-off searches.
- Set the collector and heap the same way in dev and release for benchmarks: `-Xms = -Xmx`, and the same GC flag (F7).
- Compare two branches:
  - Same scenario: needs items 5, 7 (`--level --room --seed`), 9 and 10.
  - Same JVM flags; throw away the first N seconds (warm-up); do 3 to 5 runs.
  - Compare median and p99 frame time, bytes per frame, GCs per minute and the longest pause.
  - Add a compare mode to `analyze.py` (`--compare a.txt b.txt`).
- CI later: a headless scenario run (item 5). Gate on allocated bytes per frame and GC count, which change little between machines. Do not gate on milliseconds; CI runners are too noisy. Store the baseline in the branch.

(4) Open questions for the owner
- Does the shipped JRE include JFR? Do you want to move to JDK 21, where generational ZGC can reduce GC cost? That needs its own decision.
- Which scenarios make the baseline (for example Wily 3 background, a gauntlet, a boss fight)?

(5) Proposed backlog wording
Title: Allocation and GC baseline per branch
- Add per-frame allocated bytes and GC count/time changes to `RuntimeDiagnostics` through a platform-neutral probe.
- Opt-in JFR recording with a custom frame/room event; `-Xlog:gc` beside it.
- Same collector and heap flags for benchmark runs; a warm-up rule.
- `analyze.py` compare mode for two captures.
- Later: a CI gate on bytes per frame for a fixed headless scenario.

(6) Dependencies: 5, 7, 9, 10 (repeatable scenario), 31 (probe interface), 1 (CI), 43 and 45 (the first allocation fixes to measure).
````
