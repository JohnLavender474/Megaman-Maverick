# CLAUDE.md

Megaman Maverick is a non-commercial Mega Man fan game. It uses Kotlin and LibGDX. The game is in a complete state. Gameplay changes must keep the current behavior unless the task says otherwise.

## Mega refactor (in progress)

A multi-branch refactor of `engine/` and `core/` is in progress. The goal is to remove deep inheritance, global state (for example `game.megaman`), and fragile event ordering, and to make the game easier to configure and extend. Performance is also a goal: the game must play well on weak hardware. The current gameplay is the contract: a change merges into `master` only if the game still plays the same.

- All docs for this effort are in `docs/mega-refactor/`. Read `docs/mega-refactor/CLAUDE.md` before you do refactor work.
- `docs/mega-refactor/ITEMS.md` lists every item and branch with its status. Each item has a folder `docs/mega-refactor/items/<id>-<slug>/`.
- Refactor branches are named `mega-refactor-<id><letter>-<slug>`, for example `mega-refactor-001a-package-rename`.
- Worktrees for parallel branches go in `.worktrees/` (git-ignored).
- Bugs and behavior questions go in `docs/issues/`, one file each. Read `docs/issues/README.md` first.

## Modules

`settings.gradle` includes three Gradle modules:

| Module | Source root | Purpose |
|---|---|---|
| `engine` | `engine/src/main/kotlin/com/maverick/game/engine/` | ECS engine for Mega Man-like games (the "Maverick Engine"). Genre concepts are allowed; Megaman Maverick content (its entities, levels, assets) is not. |
| `core` | `core/src/main/kotlin/com/megaman/maverick/game/` | All game logic. Depends on `:engine`. |
| `lwjgl3` | `lwjgl3/src/main/kotlin/com/megaman/maverick/game/lwjgl3/` | Desktop launcher (`DesktopLauncher.kt`). |

Versions are in `gradle.properties` (`projectVersion`, `kotlinVersion`, `gdxVersion`). The JVM target is 17.

## Build, run, test

- Debug run: `./run-game-debug.sh`. It adds `--debugText`, `--debugWindow`, `--diagnostics` and a 1920x1080 window.
- Alpha run: `./run-game-alpha.sh`. Normal play settings with `--pixelPerfect` and `--writeLogsToFile`.
- Test run: `./run-game-test.sh`. It uses `--runType test`, which starts `LevelDefinition.TEST_1` directly (same as `--level TEST_1`).
- The three run scripts pass extra arguments through to the game, for example `./run-game-alpha.sh --level DESERT_MAN --room room4`.
- Build JAR: `./gradlew lwjgl3:jar` (or `./gradlew lwjgl3:dist`). Output name is `Megaman-Maverick-<projectVersion>.jar`.
- Platform bundles: `./build-game-linux.sh`, `./build-game-mac.sh`, `./build-game-windows.sh`. They call `build-game.sh`, which bundles a JRE from `game-builds/jdk/`.
- Engine tests: `./gradlew engine:test` (Kotest + MockK, in `engine/src/test/`).
- Game tests: `./gradlew core:test` (only a few tests in `core/src/test/`; `MegamanHeartTankTest` shows how to build an entity with a relaxed MockK `MegamanMaverickGame`). Most gameplay has no tests. To verify a gameplay change, run the game.
- CI: `.github/workflows/ci.yml` runs on a push to any branch and on PRs to `master`: one Gradle job (`engine:test core:test lwjgl3:jar`, JDK 17) and the `utils/` tests. Kotest and MockK versions are in `gradle.properties`. Gradle 8.10.1 runs on JDK 17 or 21, not on JDK 25.

Launcher flags are in `DesktopLauncher.DesktopAppArgs`. Examples: `--performance low|medium|high`, `--pathfinding async|sync`, `--worldContainer`, `--logLevels debug,log,error`, `--debugShapes`, `--debugPathfinding` (draws the current path of each pathfinding entity as lines), `--fixedStepScalar`. Dev start options (checked by `resolveLaunchOptions`; a bad value exits with code 2): `--level <LevelDefinition>` starts a level; `--screen <ScreenEnum>` opens a screen, and with `--level` it stages that level for the next level start; `--room <name>` (needs `--level`) starts at the first player spawn in that room; `--logTags a,b` or `--logTags all`; `--weapons MOON_SCYTHES,RUSH_JET` or `--weapons all` gives Megaman these `MegamanWeapon`s at start (in memory only; the save file does not store weapons); `--autoPerf false`. `--runType test` is a deprecated alias for `--level TEST_1`.

Python tools in `utils/` (`level_analyzer`, `level_builder`, `diagnostics`) each have a `run.sh` and a `run_tests.sh`. `utils/tiled_extensions/` holds Tiled editor extensions.

## Core architecture

### Game object

`MegamanMaverickGame.kt` is the LibGDX `Game`. It owns the shared state:

- `engine: GameEngine`, `eventsMan: EventsManager`, `assMan: AssetManager`, `audioMan: MegaAudioManager`, `state: GameState`.
- `players: MegaPlayers` (the players service; capacity 1) and `gravity: WorldGravity`. `postCreate()` makes the one `Megaman` once (not pooled) and registers it in slot 0. Code reaches a player through `game.players` (for example `targetPlayer`, `players.forEach`, `x.asPlayer`), not through a global.
- `screens` (keyed by `ScreenEnum.name`), `viewports` (keys `ConstKeys.GAME` and `ConstKeys.UI`), and `runQueue`.
- `createGameEngine()` sets the system order: Controller, Behaviors, World, Cullables, Motion, Pathfinding, Points, Updatables, Fonts, Animations, Sprites, DrawableShapes, Audio, Groups.

Frame order in `render()`: drain `runQueue`, then `currentScreen.render(delta)` (the level screen calls `engine.update(delta)`), then `eventsMan.run()`.

### Engine ECS (`engine/`)

- `GameEngine` queues spawns and destroys. It processes them at the start of `update()`, then updates each `GameSystem`.
- `GameSystem` takes a component mask. An entity joins a system only if it has all components in the mask. A system can override `onEntityJoined`/`onEntityLeft`; they fire when an entity really joins or leaves (in the engine, right after `onSpawn` and before the component `reset()` at destroy).
- Entity groups: a `GroupsComponent` holds an entity's group names; `GroupsSystem` keeps the group index. Look up with `game.getSystem(GroupsSystem::class)!!.getEntitiesInGroup(name)`. `MegaGameEntity` puts the `ConstKeys.GROUP` spawn property into the component.
- `GameEntity` stores components in a map keyed by `KClass`. Add them with `addComponent(...)`.
- Components: `BodyComponent`, `SpritesComponent`, `AnimationsComponent`, `UpdatablesComponent`, `BehaviorsComponent`, `ControllerComponent`, `CullablesComponent`, `PointsComponent`, `DrawableShapesComponent`, `FontsComponent`, `PathfindingComponent`, and others.
- Physics is custom (not Box2D): `engine/.../world/` (`Body`, `Fixture`, `WorldSystem`). Game rules for contacts are in `core/.../world/contacts/MegaContactRules.kt` (a table of fixture-type pairs, dispatched by the engine `ContactRuleDispatcher`; one rule class per pair in `world/contacts/rules/`) and `world/collisions/MegaCollisionHandler.kt`. `ContactRuleSelectionTest` checks the table against `core/src/test/resources/contact-rule-selection.txt`.

### Entity lifecycle

`init()` runs once, on the first spawn. Then `onSpawn(spawnProps)` runs on each spawn, and `onDestroy()` runs on each destroy. On destroy, the engine calls `reset()` on every component before `onDestroy()`.

`spawn(props)` and `destroy()` only queue the change. The change happens on the next `engine.update()`.

### Entity base classes (`core/.../entities/contracts/`)

- `MegaGameEntity`: base for all game entities. Handles `runnablesOnSpawn`/`runnablesOnDestroy`, difficulty spawn flags (`ConstKeys.HARD_MODE_ONLY`, `ConstKeys.NORMAL_MODE_ONLY`) and registration in `MegaGameEntities` (lookup by tag, type or map object id).
- Map object id: an entity spawned with `ConstKeys.ID` (every map spawn, and code that passes an id on) has a `MapObjectComponent` (`entities/state/`); read it with `entity.mapObjectId` (`null` for a runtime entity). It is level data and not unique (successors and children can share it). `MegaGameEntities.getOfId(id)` finds the entities of a map object. Use an entity reference, not an id, to remember a runtime entity.
- `AbstractHealthEntity`: health, damage timers, damage blink, `damageNegotiator`, `onHealthDepleted()`.
- `AbstractEnemy`: adds Audio, Cullables, Body and Sprites components in `init()`. Subclasses implement `defineBodyComponent()` and `defineSpritesComponent()`. Handles item drops and spawn blink.
- `AbstractBoss`: boss intro, health fill, defeat sequence. Registers as an event listener in `onSpawn` and removes itself in `onDestroy`.
- `AbstractProjectile` and many small interfaces (`IFreezableEntity`, `IHazard`, `IOwnable`, `IFaceable`, and so on).
- `IMegaGameEntity` gives every entity the extensions `players` and `targetPlayer` (see `docs/mega-refactor/items/029-player-contract/`).

Entity packages match `EntityType` (`entities/EntityType.kt`): `blocks`, `enemies` (about 127 files), `bosses`, `hazards`, `items`, `projectiles`, `sensors`, `special`, `decorations`, `explosions`. The player is in `entities/megaman/` (behaviors, components, constants, handlers, sprites).

### Factories

- Use `MegaEntityFactory` (`entities/MegaEntityFactory.kt`). It makes one pool per class and calls the primary constructor `(game)` through reflection. Example: `MegaEntityFactory.fetch(Bullet::class)!!.spawn(props(...))`.
- A pooled entity goes back to its pool through `runnablesOnDestroy[ConstKeys.POOL]` (see `GameEntityPoolCreator`).
- `EntityFactories` (`entities/factories/EntityFactories.kt`) is deprecated. Do not add new entries to it. Some old code still calls it.

### Events

- `EventsManager` (engine) is pub/sub. Listeners implement `IEventListener` with `eventKeyMask: ObjectSet<Any>` and `onEvent(event)`.
- Submit with `game.eventsMan.submitEvent(Event(EventType.X, props(...)))`. Event keys are in `events/EventType.kt`.
- Events are buffered. Listeners get them only when `eventsMan.run()` runs, after the screen render. Listener order is registration order.
- Register in `onSpawn` (`game.eventsMan.addListener(this)`) and remove in `onDestroy`.

### Levels and Tiled pipeline

- Each level is an entry in `levels/LevelDefinition.kt`. `tmxMapSource` is a file in `assets/tiled_maps/tmx/`. The entry also sets `type`, `music`, `screenOnCompletion` and an optional `levelOverride`.
- `assets/tiled_maps/tmx/` has many old map versions (for example `MoonMan_16x14_v5.tmx`). Only the file named in `LevelDefinition` is live.
- `screens/levels/MegaLevelScreen.kt` loads the map. `screens/levels/tiled/layers/MegaMapLayerBuilders.kt` maps layer names (`ConstKeys` values such as `game_rooms`, `player`, `enemies`, `blocks`, `triggers`) to builders. It builds layers in a fixed order and skips unknown layers. Layers named `blocks_*` use the `blocks` builder.
- `SpawnersLayerBuilder`: the Tiled object `name` is the entity class simple name. It resolves to `com.megaman.maverick.game.entities.<package>.<name>` from the layer's `EntityType`. Object properties become the `spawnProps`.
- Room transitions are handled by `CameraManagerForRooms` in `MegaLevelScreen`.

### Screens

`screens/ScreenEnum.kt` lists all screens. They are registered in `MegamanMaverickGame.postCreate()`. Change screens with `game.setCurrentScreen(ScreenEnum.X.name)`. Start a level with `game.startLevel(LevelDefinition.X)`.

### Assets

- `assets/TextureAsset.kt` (atlases in `assets/sprites/sprite_sheets/`), `SoundAsset.kt` (`assets/sounds/`), `MusicAsset.kt` (`assets/music/`). All implement `IAsset` (`source`, `assClass`).
- Get a texture region with `game.assMan.getTextureAtlas(TextureAsset.X.source).findRegion("$TAG/name")`.
- The `generateAssetList` Gradle task writes `assets/assets.txt` on each build.

### Constants (`Constants.kt`)

- `ConstVals`: numbers. `PPM = 32`, `VIEW_WIDTH = 16f`, `VIEW_HEIGHT = 14f` (tiles), `MAX_HEALTH = 30`.
- `ConstKeys`: string keys (about 400) for `Properties`, events and Tiled properties. Use an existing key before you add a new one.
- `ConstFuncs`: camera start positions.

## Conventions

- Each entity has `companion object { const val TAG = "ClassName" }`. `TAG` is also the atlas region prefix.
- Shared texture regions go in a companion `ObjectMap` and load once in `init()`, before `super.init()`.
- Components are built in `define...Component()` methods and added in `init()`.
- Build `Properties` with `props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo spawn)`. Read them with `spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!` or `getOrDefault(key, default, Type::class)`.
- Scale all sizes, speeds and gravity by `ConstVals.PPM` (for example `SWIM_SPEED * ConstVals.PPM`).
- Collections are LibGDX types (`Array`, `ObjectMap`, `OrderedMap`, `ObjectSet`). Helpers: `gdxArrayOf`, `objectMapOf`, `objectSetOf`, `orderedMapOf`.
- Body fixtures use `FixtureType` (`world/body/FixtureType.kt`). Use `BodyComponentCreator.create(this, body, BodyFixtureDef.of(...))`.
- Log with `GameLogger.debug(TAG, ...)`, `GameLogger.log(TAG, ...)`, `GameLogger.error(TAG, ...)`.

## Gotchas

- `GameLogger.filterByTag` is `true`. Debug and log messages print only if the tag is in `MegamanMaverickGame.TAGS_TO_LOG`. Errors always print.
- `runQueue` is drained with `removeLast()`, so the last runnable added runs first.
- Pooled entities are reused. Reset all per-spawn state in `onSpawn`, not in the constructor or `init()`.
- After `destroy()`, the entity stays in the engine and its systems until the next `engine.update()`. Its `existenceState` changes at once. The setter is public, but only the engine should write it: `existenceState.liveNow` = in the engine now, `existenceState.liveAfterUpdate` = in the engine after the next update.
- The `Megaman` player is created, initialized and registered manually in `postCreate()`. Do not fetch it from a factory.
- Tiled entity names must match the Kotlin class name and the package for the layer exactly. A wrong name logs an error and skips the spawner.

## Project skills (`.claude/skills/`)

- `level-designer`: design or edit a level.
- `level-builder`: build or structurally edit a level TMX file (rooms, blocks, spawns).
- `level-analyzer`: read and analyze an existing TMX level.
- `gauntlet-stages`: gauntlet stages that use `GauntletMaster`.
- `diagnostics-analyzer`: analyze `*-diagnostics.txt` files from `--diagnostics` runs.
