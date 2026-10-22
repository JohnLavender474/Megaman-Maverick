# Notes: mega-refactor-maverick-engine

Research findings from 2026-10-22. The counts come from `grep` on `master` at `9f3917b6a`.
Check them again before the work starts.

## Decisions

None yet. The open questions are in `SCOPE.md`.

## `MegamanMaverickGame` today

`core/.../MegamanMaverickGame.kt` has 1017 lines, 69 public or override members and 20 private
members. The declaration is
`class MegamanMaverickGame(val params) : Game(), IEventListener, IPropertizable` (`:137-139`).

Responsibilities:

| Group | Where | Generic or game-specific |
|---|---|---|
| LibGDX lifecycle: `create`, `render`, `resize`, `pause`, `resume`, `dispose` with ordered and guarded dispose actions | `:234`, `:406`, `:648-705` | Generic |
| Frame order: clear, drain `runQueue` (LIFO, `:419-421`), load assets, `postCreate`, auto-perf, `controllerPoller.run()`, `screenController`, screen `render`/`draw`/`drawShapes`, screenshot, `eventsMan.run()`, `audioMan.update`, updatables, debug text, notification, `GameObjectPools.performNextReclaim()` | `:406-590` | Generic loop. The parts inside it are mixed |
| Engine and systems: system order, `WorldSystem` parameters from `Performance`, `MegaContactListener`, pathfinding mode, `onQueueToSpawn`/`onSpawnRejected` cast to `MegaGameEntity` | `createGameEngine` `:810-880` | The assembly is game-specific. The pattern is generic |
| Assets: `assMan`, `queueAssets` from the asset enums, `ASSET_MILLIS` | `:197`, `:795-808` | The manager is generic. The lists are game-specific |
| Audio: `MegaAudioManager`, volumes | `:362-366` | The interface is generic (`engine/.../audio/IAudioManager.kt:3`) |
| Events: `eventsMan`; the game listens for `TURN_CONTROLLER_ON/OFF` and `TOGGLE_PIXEL_PERFECT` | `:173-177`, `:592-646` | The bus is generic. The handlers are game-specific |
| Screens: 16 screens registered in `postCreate`, `setCurrentScreen`, `currentScreen` | `:379-399`, `:882` | Hosting is generic. The list is game-specific |
| Cameras and viewports: `RotatableCamera`, `OrthographicCamera`, `FitViewport`/`PixelPerfectFitViewport` keyed by `ConstKeys.GAME`/`UI` | `:277-302`, `:612-640`, `:910-916` | Mostly generic. Camera rotation is game-specific |
| Input: `buttons`, `controllerPoller`, `selectButtonAction` | `:193-195` | The poller is generic |
| Game state and save through `Gdx.app.getPreferences` | `:202`, `:753-790` | Game-specific. Only the save backend is generic |
| Property bag (`IPropertizable.properties`): drawables, shapes, systems, room suppliers, world container, map load result, level def, camera flags | `:178`, `:708-990` | Mixed. Most keys are game state hidden in a map |
| Run queue | `:188` | Generic |
| Performance and auto-perf | `:141-146`, `:452-482`, `:1002-1016` | The policy is generic. The values are game-specific |
| Diagnostics: `RuntimeDiagnostics`, log file writer, `DebugWindow`, notifications, debug text | `:212-231`, `:974-986` | Generic tooling |
| Levels: `setCurrentLevel`, `getCurrentLevel`, `startLevel`, `startExtraLevel`, `applyLevelOverride`, `resetPlayerProgress` | `:708-751`, `:896-908` | Game-specific |
| `megaman`: `lateinit`, built by hand, a `GameState` listener | `:204`, `:368-373` | Game-specific |
| Rooms and world: `getRooms`, `getCurrentRoom`, `get/setWorldContainer`, `getTiledMapLoadResult` | `:938-972` | Level-scoped. Belongs in `LevelContext` |

Important: the class uses LibGDX `Game` only as an `ApplicationListener`. It never calls
`setScreen`, `getScreen` or `super.*`. An engine base can implement `ApplicationListener`
directly.

## How code reaches the game object

- 506 files in `core/src/main` take `game: MegamanMaverickGame`. Also `DesktopLauncher.kt:123`
  and `MegamanHealthTankTest.kt:24`. The engine has 0 references.
- Each entity holds it: `MegaGameEntity(override val game) : GameEntity(game.engine)`
  (`MegaGameEntity.kt:13`). `IMegaGameEntity` declares `val game` and the `megaman` extension
  (`IMegaGameEntity.kt:12`, `:22-23`).

Most-used members (`game.X` references, imports excluded):

| Member | Refs | Files | Generic or game-specific |
|---|---|---|---|
| `assMan` | 482 | 414 | Generic (almost all `getTextureAtlas`) |
| `eventsMan` | 139 | 66 | Generic |
| `state` | 130 | 46 | Game-specific |
| `audioMan` | 92 | 38 | Generic |
| `getGameCamera` | 75 | 57 | Generic, but typed `RotatableCamera` |
| property bag calls | 58 | — | Game-specific |
| `controllerPoller` | 54 | 20 | Generic |
| `isCameraRotating` | 42 | 32 | Game-specific |
| `getUiCamera` | 36 | 21 | Generic |
| `setCurrentScreen` | 32 | 18 | Generic |
| `getCurrentLevel` | 24 | 16 | Game-specific |
| `batch` | 24 | 13 | Generic |
| `game.megaman` (direct) | 23 | 17 | Game-specific (the extension has about 1096 refs in 235 files) |
| `paused` | 14 | — | Generic |
| `selectButtonAction` | 13 | — | Game-specific |
| `getCurrentRoom` | 13 | 10 | Level-scoped |
| `getPerformance` | 11 | — | Generic |
| `game.engine` | 8 | 6 | Generic |

About 80% of the call traffic goes to services that are already generic.

## What `engine/` has today

- 251 main files, 52 test files, 237 declared types.
- `GameEngine` (ECS runner, `GameEngine.kt:13`), `GameSystem`, `GameEntity`, components,
  systems, custom physics (`Body`, `Fixture`, `WorldSystem`), `EventsManager` (concrete, no
  interface), `IAudioManager`, `IControllerPoller`, `IScreen`, `BaseScreen`,
  `TiledMapLevelScreen`, `PixelPerfectFitViewport`, `GameLogger`, `GameObjectPools`,
  diagnostics, pathfinding, state machines.
- No host notion and no game-specific names.
- Platform coupling (relevant to the platform-neutral engine item):
  - `Gdx.*` in `ControllerButton.kt:14`, `ControllerUtils.kt:9`, `BitmapFontHandle.kt:37`.
  - `engine/build.gradle` has `api` dependencies on `gdx-controllers-desktop` and `gdx-tools`
    (desktop only).
  - `eclipse.project.name = appName + '-core'` (`engine/build.gradle:2`) is a wrong name.

## Interface or abstract class

What goes into the engine contract and what stays in the game:

| Engine contract (generic) | Stays in the game |
|---|---|
| `engine: GameEngine` | `megaman` (replaced by the players service) |
| events bus | `GameState`, save format |
| assets or a texture cache | `LevelDefinition`, `startLevel`, level overrides |
| audio (`IAudioManager`) | rooms, world container, map load result (into `LevelContext`) |
| input (`IControllerPoller`, later `InputFrame`) | camera rotation and focus flags |
| screen host (`setScreen(key, props)`, current screen) | `selectButtonAction`, `ConstKeys` property bag |
| cameras and viewports by name | `Performance` values, system list and order |
| `post(runnable)` (run queue) | notification and debug-text content |
| clock or fixed step, random | |
| platform services: preferences, files, logging | |

Options:

- (a) Only an interface. Small change and easy to mock, but the loop, run queue, auto-perf and
  dispose order stay in the game, and each user must copy them.
- (b) Only an abstract base that owns the loop. Reusable and tested once, but consumers depend
  on a concrete class that is hard to mock, and it tends to grow.
- (c) Both: a small `IMaverickGame` and an optional abstract `MaverickGame` that implements it.
  Consumers depend on the interface or on narrow services. Hosts can use the base or write
  their own loop. Tests use fakes. **Recommended.**

A Kotlin interface cannot hold state, `lateinit` fields or the frame loop. LibGDX `Game` uses
the single superclass slot, but the game does not need `Game`.

God-interface risk:

- Entities and systems must not depend on the host contract. They take narrow services
  (`LevelContext`, assets, audio, events, players). The host only builds and owns them.
- Rule: no game feature adds a member to the host contract. A new need becomes a service.
- Today one entity test needs a relaxed mock of a class with 69 members. With narrow services, a
  test passes 2–3 small fakes.
- If the contract is extracted before the narrow-services work, it copies about 69 members.

## Naming

### `MaverickEngine` as the host name

Not recommended:

- `GameEngine` already means the ECS runner. A host named `MaverickEngine` gives
  `MaverickEngine.engine: GameEngine`.
- "Engine" should name the library or the runner, not the application that uses it.

Proposed scheme:

- Library and product: **Maverick Engine**.
- Gradle module or artifact: `maverick-engine`.
- Host interface: **`IMaverickGame`**. The `I` prefix follows the repo (`IGameEntity`,
  `IScreen`, `IAudioManager`, `IControllerPoller`).
- Optional base: **`MaverickGame`**. Alternative: `IMaverickApp`/`MaverickApp`.
- The game: `class MegamanMaverickGame : MaverickGame()`.
- ECS runner: keep `GameEngine`, or rename it to `EntityEngine`. `MaverickWorld` collides with
  the physics `world` package.

### Prefix every class with `Maverick`?

Not recommended. Use a distinct package, and use the prefix only on entry points.

Engine names that collide with common libraries:

| Engine name | Core imports | Collides with |
|---|---|---|
| `Animation` | 279 | `com.badlogic.gdx.graphics.g2d.Animation` |
| `Timer` | 308 | `com.badlogic.gdx.utils.Timer`, `java.util.Timer` |
| `Body` | 277 | gdx-box2d `Body` |
| `Fixture` | 203 | gdx-box2d `Fixture` |
| `Contact` | — | gdx-box2d `Contact` |
| `Properties` | 449 | `java.util.Properties` |
| `Event` | 62 | scene2d `Event` |
| `Pool` | 7 | `com.badlogic.gdx.utils.Pool` |
| `Table` | — | scene2d `Table` (already imported in `ScreenController.kt:12`) |
| `StateMachine` | — | gdx-ai `StateMachine` |

- The repo has 0 `import ... as` aliases, so the collisions do not hurt the game today. They
  would hurt an outside user who also uses box2d or scene2d. A distinct package fixes that,
  because auto-import shows the full package.
- A prefix on 237 types would change about 11,000 use sites and add noise.
- Precedents: LibGDX (`com.badlogic.gdx.*`, only `Gdx` carries the brand), Ashley
  (`com.badlogic.ashley.core.Engine`), Fleks (`com.github.quillraven.fleks.World`), KTX
  (`ktx.*`). All use the package for identity.
- The existing `Game*` names (`GameEngine`, `GameEntity`, `GameSystem`, `GameLogger`,
  `GameSprite`, `GameRectangle`) already act as a prefix.

### Root package

- `com.mega.game.engine` contains "mega", which reads as Mega Man.
- `com.maverick.engine`: short. The domain is not owned, which is fine if the engine is not
  published.
- `io.github.johnlavender474.maverick`: Maven Central accepts `io.github.<user>` after GitHub
  verification. Use it only if you publish.
- JitPack uses the group `com.github.JohnLavender474` whatever the package is.

## Rename cost and safety

| Item | Count |
|---|---|
| Files with `package com.mega.game.engine...` | 304 |
| Import lines in `engine/src/main` | 352 in 126 files |
| Import lines in `engine/src/test` | 98 in 33 files |
| Import lines in `core/src/main` | 10,920 in 602 files |
| Import lines in `core/src/test` | 5 in 3 files |
| Import lines in `lwjgl3` | 1 in 1 file |
| Total | about 11,376 import lines and 304 package lines in about 760 files |

Fully qualified names outside imports: KDoc in `LooseQuadtreeWorldContainer.kt:20`,
`IContactFilter.kt:12`, `:21`; code in `CullableOnUncontainedTest.kt:14`, `:33`; the path in the
root `CLAUDE.md:19`.

Safety:

- IntelliJ "Rename package" (with "search in comments and strings") updates package lines,
  imports, KDoc and qualified names in one step. A `sed` script over about 760 files plus
  `git mv` also works.
- Runtime class lookups build only core package strings:
  `EntityType.PACKAGE_PREFIX = "com.megaman.maverick.game.entities."` (`EntityType.kt:20`),
  used by `MegaEntityFactory.kt:49`, `:52`, `SensorsLayerBuilder.kt:30`, `HeartTank.kt:141`,
  `BossSpawnEventHandler.kt:21`. No engine package string is built at runtime.
- Other places that name the core package: `lwjgl3/build.gradle:23` (`mainClassName`), `:109`,
  `:117`.
- Check: `./gradlew engine:test core:test lwjgl3:jar` and one game run.

Git:

- One rename commit keeps file content almost the same. Rename detection, `git log --follow`
  and IntelliJ history keep working. Add the commit to `.git-blame-ignore-revs`.
- Engine history already restarts at `2cb62816c` (2024-12-25, "Engine is now a part of the
  Megaman Maverick project").
- Branches on 2026-10-22: no `mega-refactor-` branch is active. `overhaul_1` is 9 ahead and 957
  behind with no engine changes. `fix/pool-identity-freeset`, `refactor/matrix-flat-array`,
  `temp` and `old-v1` are 0 ahead. Now is the quietest time for the rename.

## Publishing

- The code is MIT (`LICENSE`; `NOTICE.md` says "Original source code: MIT"). The engine has no
  assets. The Capcom disclaimer in `NOTICE.md` applies to the game, not the engine.
- A separate engine repo or artifact needs its own README and LICENSE.
- Missing for a release: a `group`, `maven-publish`, a separate engine version (the root
  `version = '$projectVersion'` at `build.gradle:66` is the game version), and platform-neutral
  dependencies.
- A minimum step is a JitPack or `mavenLocal` publish from this repo, with an engine version.

## Order of work

1. Part A, the package rename: first and alone, while no other branch is active.
2. Part B, the host contract: after narrow services; with or after the fixed tick; before or
   with the platform-neutral engine; soft dependency on `IPlayer`.

Add one line to the narrow-services backlog item: "entities never take the host contract".

## Overlap with other branches

None. No `mega-refactor-` branch is active (2026-10-22).
