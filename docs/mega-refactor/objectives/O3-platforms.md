# `O3`: Web play and other platforms

- Status: planned

## Problem

- `P1`: The game runs only as a desktop download. A person must install it to play, and a browser cannot run it.
- `P2`: Some code on the path that a web build reaches uses JVM-only features (threads, `java.io`, `java.time`, Swing,
  Kotlin reflection). `088` `NOTES.md` lists the failures from the probe.
- `P3`: Startup loads all assets (178.3 MB of files, and 396.3 MB of textures when decoded), and some textures are over
  4096 px. A web page cannot load that much at start.
- `P4`: A web build must play the same as desktop. The X6 work found differences in `Math.round` and `Random` on
  TeaVM, so "it compiles" does not prove this.

## Goal

The ability to launch the game from desktop, as today, or to play it in a web browser.

Desktop stays the main target and must not get worse for the web build. The first web target is a local build, not
public hosting (`088`).

## Done means

- `DESERT_MAN` starts and plays in Chromium and Firefox from a local build.
- A trace compare shows that scripted input gives the same player and camera values on desktop and web.
- The full game loads in a browser with no read of all assets at start.

## Rules

### `O3-R1`: No JVM-only API on the web path

- Rule: code that a web build reaches uses no JVM-only API. A platform feature goes behind an interface.
- Problem: `P2`
- Check: search `engine/src/main` and `core/src/main` for `java.io`, `java.nio`, `java.time`, `java.util.concurrent`,
  `java.util.zip`, `javax.swing`, `java.awt`, `Thread`, `Executors`, `synchronized`, `@Volatile` and `.format(`.
- Applies: `always-on`
- Active: after `032`

### `O3-R2`: Entity creation with no reflection

- Rule: entity creation does not need Kotlin reflection.
- Problem: `P2`
- Check: search for `primaryConstructor`, `Class.forName` and `.java.enumConstants`.
- Applies: `linked items`
- Active: after `021`

### `O3-R3`: Load only what a level needs

- Rule: startup does not read all assets. A level names the assets that it needs.
- Problem: `P3`
- Check: read the loading code in `MegamanMaverickGame`, and measure the bytes loaded before the first level.
- Applies: `linked items`
- Active: after `033`

### `O3-R4`: Textures inside the WebGL limits

- Rule: a texture that a web build loads is at most 4096 px on each side.
- Problem: `P3`
- Check: list the sizes of the live atlas pages and tilesets.
- Applies: `linked items`
- Active: `now`

### `O3-R5`: Same results on desktop and web

- Rule: gameplay math gives the same result on the JVM and on TeaVM Wasm. Use project functions where a platform
  differs, for example for rounding and random values.
- Problem: `P4`
- Check: a trace compare, as in `088b`.
- Applies: `linked items`
- Active: after `088`

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01. Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O3-R1` | `broken` (waiting) | 21 code lines in 7 files: `RuntimeDiagnostics.kt`, `GameLogger.kt:7-8`, `AsyncPathfindingSystem.kt:7,29`, `AsyncFileWriter.kt`, `MegamanMaverickGame.kt:126`, `DebugWindow.kt:5-9`, `DarknessV4.kt:634`. Also 8 `java.util.concurrent` lines, 4 `synchronized` or `@Volatile` lines in `DarknessV4.kt`, and 1 `java.util.zip` line. `PlatformServices` has 0 hits. | `032` |
| `O3-R2` | `broken` (waiting) | `MegaEntityFactory.kt:20,49,52,70`, `SensorsLayerBuilder.kt:30`, `EnumStateMachineBuilder.kt:11` (25 lines name this builder), `GameObjectPools.kt:19`. | `021`, `032` |
| `O3-R3` | `broken` (waiting) | `queueAssets()` (`MegamanMaverickGame.kt:888-901`, called at `:373`) queues all assets: 178.3 MB of files (music 167.3 MB). Decoded RGBA is 396.3 MB (atlases 190.4 MB, tilesets 205.9 MB). | `033` |
| `O3-R4` | `partly` | 66 atlas pages, largest side 2688 px. 3 of 31 live tilesets are over 4096 px: `tomahawk_man_stage_tileset.png` 8196x2052, `GlacierMan.png` 6144x2880, `super_mario_bros_3_world_1_stage_1.png` 5632x1252. | `088c` |
| `O3-R5` | `not checked` | No `teavm` module or trace tool exists. Static read: `Math.round` 0 lines, but `roundToInt` 14 lines, for example `Electrocutie.kt:117`, and `getRandom` uses `java.util.Random` on most of 137 lines in 52 files. | `088b`, `011`, `010`, `055` |

## Redo risk

None found. No merged item added a JVM-only API. `MegaEntityFactory` keeps reflection after `016`, and `021` plans its replacement.

## Items

| Item | Status | Note |
|------|--------|------|
| `032` | planned | [Platform-neutral `engine` module](../items/032-platform-neutral-engine/ITEM.md) (main) |
| `033` | planned | [Per-level asset manifests and texture budgets](../items/033-per-level-assets/ITEM.md) (main) |
| `035` | planned | [Aspect-ratio policy for phones](../items/035-aspect-ratio/ITEM.md) (main) |
| `054` | planned | [Support web browsers and Android](../items/054-web-and-android/ITEM.md) (main) |
| `088` | idea | [Web player (TeaVM Wasm)](../items/088-web-player/ITEM.md) (main) |
| `021` | planned | [Entity registry with stable readable IDs and aliases](../items/021-entity-registry/ITEM.md) |
| `028` | planned | [Game-owned texture and animation cache](../items/028-texture-cache/ITEM.md) |
| `039` | planned | [Retire unused assets and review dead code](../items/039-retire-dead-assets-code/ITEM.md) |
| `067` | planned | [PPM 16 with 1x pixel art](../items/067-ppm-16/ITEM.md) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

- 2026-10-31 (from `088`): local first. Public hosting is not in scope for now.

## Open questions

- Android: is it a real target, or only a possibility? `054` has the open questions.
- Public hosting: see `088` question 1.
- Web netplay needs its own transport, because TCP sockets do not work on GWT and a browser gives no raw sockets. See
  `O2`.
