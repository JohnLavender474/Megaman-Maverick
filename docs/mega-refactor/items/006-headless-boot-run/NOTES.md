# Notes: 006 Headless boot-and-run per level (stage A)

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Testing, CI and bug tracking (2026-10-22)

Copied as written from the review section "ITEM 5. Headless boot-and-run per level: what it gains".

````text
(1) Current state and blockers (`MegamanMaverickGame.kt`)
- `create()` builds `ShapeRenderer()` and `SpriteBatch()` (`:257-259`). Both throw on `MockGL20`, so they need a factory or injection.
- `render()` takes `delta = Gdx.graphics.deltaTime` (`:414`). Headless `MockGraphics` gives wall-clock time, so this needs an injected fixed delta (item 9). Entities also read `Gdx.graphics.deltaTime` (`GameEntityUtils.kt:169`, `BikerKibbo.kt:141`, `SubmarineJoe.kt:277`) and `framesPerSecond` (`DrillHead.kt:159`, `Laser.kt:285`).
- Assets load asynchronously in `render()` through `assMan.update(ASSET_MILLIS)` (`:427`). `queueAssets()` queues all music, sounds, textures and all 18 TMX maps at boot (`:795-807`). A test needs `finishLoading()` or per-level loading (item 32), or boot takes long.
- Auto-perf changes the fixed step from FPS (`:454-467`), so it must be turned off (item 7 `--autoPerf false`).
- Pathfinding defaults to `async` (threads, `:842`). Use `sync`.
- Preferences: `Gdx.app.getPreferences` for the save (`:757-768`) and for keyboard and controller settings (`ControllerUtilsExtensions.kt:12-15`). The headless backend writes prefs to a dir in the user's home, so tests must point `preferencesDirectory` to a temp dir to protect real saves.
- Controllers: `Controllers.getControllers()` (`engine/.../ControllerUtils.kt:16,23`). I expect gdx-controllers to fall back to a stub on `HeadlessDesktop`. Verify this.
- Random sources (item 10).
- Input: headless `MockInput` reads all keys as up, so Mega stands still unless inputs are injected (item 30 `InputFrame`).
- There is already an entry hook: `RunType.TEST` calls `startLevel(...)` directly (`:395-398`).

(2) What it catches that items 2–4 do not
- Level build with real map data: crashes in layer builders, for example `spawn_room!!`, bad property types and object-reference properties. Item 2 checks names and enums statically, not the code paths.
- `onSpawn` with real TMX spawn props. Item 3 only covers `init()`.
- The first N frames of update: behaviors, contact listener and collision handler on real geometry, the spawn queues, `PLAYER_SPAWN`/`PLAYER_READY`, and room-entry event chains.
- Engine-level regressions that unit tests missed. Example: the verified `CullablesSystem.kt:26` bug, where `?: return` exits `process` for all remaining entities and also skips `diagnostics?.endEntry()`. It survived `CullablesSystemTest`. A long headless run with entity-count and pool assertions can expose leaks of this kind.
- Asset references: missing TMX, sounds or music for a level.
- Cheap metrics with no rendering: per-level simulate time, allocations and GC (feeds item 8), and pool-reclaim checks (item 44).
- Each refactor branch from 9 to 19 changes the loop or the engine. "Every level boots and runs 600 frames without an exception, and Mega is alive in the spawn room" is a coarse but strong check for all of them.

(3) Relation to record-and-replay
- The headless harness is the runner that record-and-replay needs. Replay adds an input source, determinism (9, 10, sync pathfinding) and trace compare. Stage A (idle boot-and-run) has no determinism needs beyond a fixed delta. Stage B (scripted or recorded inputs plus trace diff) is the record-and-replay item.

(4) Cost
- Stage A, small to medium:
  - inject a batch and shape-renderer factory (or a no-op renderer) into the game
  - an injectable delta source
  - a "load synchronously" boot path (`finishLoading()` with only the needed assets)
  - a prefs dir and flags for sync pathfinding and no auto-perf (overlaps item 7)
  - a test that runs each `LevelDefinition` for N frames in the `smokeTest` suite.
  These are boot-path changes with no gameplay effect, and the contract is easy to check.
- Stage B, larger: it depends on 9, 10 and 30, and on a trace format.

(5) Worth doing early or later?
- Do Stage A early, right after 1–3 and before the engine and loop items (9–19), which are the riskiest. Its game-side changes are small and are also needed by 7 and 9.
- Stage B comes later, as the record-and-replay item, after 9, 10 and 30.

(6) Open questions
- How many frames per level, and from which spawn points? All checkpoints is better, but needs item 35 data or `--room`.
- Should Stage A also tick a level at each performance tier? Today the physics step depends on the tier.
- What test runtime is acceptable in CI? Loading assets for 18 levels sets the cost.

(7) Backlog wording
Title: Headless boot-and-run per level (stage A), the base for record-and-replay
- Make boot headless-safe: injectable batch and shape renderer, injectable frame delta, synchronous asset load, temp prefs dir, sync pathfinding, auto-perf off.
- A `smokeTest` spec boots every `LevelDefinition` and runs N fixed-step frames with no input. It asserts no exception, Mega alive, entity and pool counts bounded, and the level and room as expected.
- Record frame time and allocations per level as a by-product (feeds item 8).
- Stage B (inputs plus trace diff) stays in the record-and-replay item.

(8) Dependencies: items 1 and 3 (harness). It overlaps 7 (flags), 9 (delta), 32 (per-level loading) and 8. Stage B needs 9, 10 and 30.
````
