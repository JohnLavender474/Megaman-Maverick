# CLAUDE.md

Megaman Maverick is a non-commercial Mega Man fan game. It uses Kotlin and LibGDX. The game is playable from start to end, and work continues on its design and its features. A change keeps all behavior that its task does not name as a change.

## Mega refactor (in progress)

A multi-branch effort on `engine/` and `core/` is in progress. It includes refactors and also new behavior. `docs/mega-refactor/objectives/README.md` lists the objectives: clean design, multiplayer, web play, deeper levels and gameplay, performance on weak hardware, safe change, content from data, more characters and modes, and player experience. A branch keeps all behavior that its contract does not name as an intended change.

- All docs for this effort are in `docs/mega-refactor/`. Read `docs/mega-refactor/CLAUDE.md` before you do refactor work.
- Each item links to at least one objective. Each objective gives the rules that the code must obey, and the current state of each rule.
- `docs/mega-refactor/ITEMS.md` lists every item and branch with its status. Each item has a folder `docs/mega-refactor/items/<id>-<slug>/`.
- Refactor branches are named `mega-refactor-<id><letter>-<slug>`, for example `mega-refactor-001a-package-rename`.
- Bugs and behavior questions go in `docs/issues/`, one file each. Read `docs/issues/README.md` first.

## Branches

- The main checkout stays on `master`. Never switch branches in it (`git switch`, `git checkout <branch>`).
- Every other branch goes in its own worktree in `.worktrees/<branch>/` (git-ignored), whatever its type: `git worktree add -b <branch> .worktrees/<branch> master`.
- `master` itself never goes in a worktree. Work that belongs on `master`, such as `docs/issues/`, is done in the main checkout.

## Temporary files

Put temporary work files in `temp/` at the repo root (git-ignored by `**/temp/`), one subfolder per task, for example `temp/x-sabre-gifs/`. This includes GIFs, sprite drafts, contact sheets, previews, level drafts and one-off helper scripts. Use it in place of a session scratchpad under `/tmp`, so that the owner can find the files later. Subagents write there too. Never commit files from `temp/`.

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
- Co-op test run: `./run-game-test-coop.sh`. The test run settings with `--players 2`; it starts `REFACTOR_TEST_047` (the co-op test map of item `047`) in `--mode coop` unless the arguments give `--level` or `--mode` (for example `./run-game-test-coop.sh --room group_wide`).
- The run scripts pass extra arguments through to the game, for example `./run-game-alpha.sh --level DESERT_MAN --room room4`.
- Build JAR: `./gradlew lwjgl3:jar` (or `./gradlew lwjgl3:dist`). Output name is `Megaman-Maverick-<projectVersion>.jar`.
- Platform bundles: `./build-game-linux.sh`, `./build-game-mac.sh`, `./build-game-windows.sh`. They call `build-game.sh`, which bundles a JRE from `game-builds/jdk/`.
- Engine tests: `./gradlew engine:test` (Kotest + MockK, in `engine/src/test/`).
- Game tests: `./gradlew core:test` (only a few tests in `core/src/test/`; `testServices()` in `core/src/test/.../TestServices.kt` builds a real `MegaServices` for an entity test, with relaxed mocks only for the engine, the assets and the audio; `MegamanHeartTankTest` shows its use). Most gameplay has no tests. To verify a gameplay change, run the game.
- Smoke tests: `./gradlew core:smokeTest` (`core/src/smokeTest/`; part of `check`). It builds every concrete entity class and calls `init()` headless, with the real atlases and no window. A missing region fails, also when `init()` does not throw; a miss through `containsRegion` does not count. `known-entity-failures.txt` lists the classes that fail today: do not add to it, and remove a line when you fix the class.
- CI: `.github/workflows/ci.yml` runs on a push to any branch and on PRs to `master`: one Gradle job (`engine:test core:test core:smokeTest lwjgl3:jar`, JDK 17) and the `utils/` tests. Kotest and MockK versions are in `gradle.properties`. Gradle 8.10.1 runs on JDK 17 or 21, not on JDK 25.

Launcher flags are in `DesktopLauncher.DesktopAppArgs`. Examples: `--performance low|medium|high`, `--pathfinding async|sync`, `--worldContainer`, `--logLevels debug,log,error`, `--debugShapes`, `--debugPathfinding` (draws the current path of each pathfinding entity as lines), `--fixedStepScalar`, `--loadKeyboardPrefs` (loads the saved keyboard settings at startup, the same as "LOAD SAVED SETTINGS" in the keyboard settings screen; default off), `--resetBindings` (clears the slot settings file at startup, before `--loadKeyboardPrefs` runs), `--interpolation true|false` (default `true`: draws the bodies and the camera between the last 2 physics steps; when the launch gives the flag, the `I` key toggles it live; item `134`). Dev start options (checked by `resolveLaunchOptions`; a bad value exits with code 2): `--level <LevelDefinition>` starts a level; `--screen <ScreenEnum>` opens a screen, and with `--level` it stages that level for the next level start; `--room <name>` (needs `--level`) starts at the first player spawn in that room; `--logTags a,b` or `--logTags all`; `--weapons MOON_SCYTHES,RUSH_JET` or `--weapons all` gives Megaman these `MegamanWeapon`s at start (in memory only; the save file does not store weapons); `--heartTanks A,B` or `--heartTanks all` gives these `MegaHeartTank`s and `--healthTanks A,B` or `--healthTanks all` gives these `MegaHealthTank`s full at start (the save file stores tanks, so with either option `saveState()` writes nothing); `--autoPerf false`; `--players 2` adds a second tinted `Megaman` in slot 1, which uses the last connected pad, while slot 0 keeps the keyboard and the other pads (co-op is in progress, see item `047`); `--mode coop|pvp` chooses the `GameMode` of a 2-player session (default `coop`; either value with 1 player is an error; `--players 2` with a level whose `characters` has no `MEGAMAN`, for example `X6_INTRO`, is an error). A `player` layer spawn can set a `player_2` object property to the spawn point of slot 1, a rectangle in a layer with no builder (for example `player_slot_spawns`); with no property, slot 1 spawns 1 tile to the right, relative to the spawn's `direction`. `--runType test` is a deprecated alias for `--level TEST_1`.

Python tools in `utils/` (`level_analyzer`, `level_builder`, `diagnostics`, `sprite_slicer`) each have a `run.sh` and a `run_tests.sh`. `utils/tiled_extensions/` holds Tiled editor extensions.

## Core architecture

### Game object

`MegamanMaverickGame.kt` is the LibGDX `Game`. It owns the shared state:

- `engine: GameEngine`, `events: EventsManager`, `assets: AssetManager`, `audio: MegaAudioManager`, `gameState: GameState`.
- `players: MegaPlayers` (the players service; capacity `MAX_PLAYERS`, which is 2) and `gravity: WorldGravity`, built with the game object (before `create()`). `postCreate()` makes each `Megaman` once: slot 0 always, and slot 1 with `--players 2`. A player is slot-owned, not pooled: one entity per slot, and `destroy` does not release it. Code reaches a player through `game.players` (for example `targetPlayer`, `players.forEach`, `players.anyLive`, `x.asPlayer`), not through a global. `players.settings(slot)` gives the `PlayerSettings` of a slot: the bindings of the 8 `MegaControllerButton`s and `selectButtonAction` (default `NEXT_WEAPON`; X does not read it, because `SELECT` is always X's dash). `PlayerSettingsStore(slot)` saves them in one prefs file per slot.
- `clock: GameClock` (`state/GameClock.kt`): `clock.paused` is the pause flag. `pause()` and `resume()` change it (and pause or resume the current screen); every reader reads `game.clock.paused`.
- `levelState: LevelState` (`levels/LevelState.kt`): the level values, typed. It lives for the session (pooled entities outlive a level); the level screen sets the values at level start and during play, and callers read them when used: `currentLevel` (read with `!!` in a level), the rooms and current-room suppliers (read with `getRooms(out)` and `currentRoom`), `isInExtraLevel`, `worldContainer`, `tiledMapLoadResult`, `roomTransition`, the beam flag and center, `coins`, `levelPoints`, the floating-points state, and `weaponsAttained` (the reward weapons of the last level that was newly defeated, for the get-weapon screen, which clears it; a save load and `resetPlayerProgress()` also clear it; not saved). The game has no forwarding functions for these values; read the home (`game.levelState.currentLevel!!`).
- `gameCamera`: a plain `OrthographicCamera`, made with the game. `cameraRotation: CameraRotation` (`engine/.../camera/`): the turn of the game camera, made with the game; it holds `direction`, `rotating` and `angle`, it never touches the camera, and it is not `Resettable`, so a respawn in a turned room keeps its smooth turn. `cameraView: CameraView`: the reads of the game camera; `viewWidth` and `viewHeight` are final at once, and `blendedViewWidth` and `blendedViewHeight` move over a turn. With no rotation part, a view gives `rotating = false`, direction `UP` and the viewport sizes.
- `cameraController: CameraController` (`engine/.../camera/`): runs the camera parts in a fixed order, made with the game. Movers run first, and a mover can hold the camera for the frame: `CameraRoomBounds`, `CameraRoomTransition`, `CameraFollow`, `CameraScroll`, `CameraLimitStage`, `CameraRoomClamp`, `CameraRoomScan`. Effects always run after them: `CameraRotation`, `CameraShake`, the follow stop rule, `CameraEdgeBlocks`. The controller writes the orientation after the parts update (`applyOrientation()`). `CameraLimitStage` takes the room as the base and the outer limit of the view, and then applies each `ICameraLimitRule` (`CameraZones`, then `CameraScroll`). Each axis is a range or a pin. `controller.reset()` resets each `Resettable` part. The level screen adds the parts. The camera holds no focus, room or bounds rules. `LevelRoomHandler` (`screens/levels/`) holds the side effects of a room transition.
- `services`: the `MegaServices` holder, for the level screen's room handler.
- `settings: GameSettings` (`state/GameSettings.kt`): `settings.performance` (a `Performance`, `state/Performance.kt`), read when used. `setPerformance()` changes it (and the FPS, the world fixed step, and the `CHANGE_PERFORMANCE_MODE` event).
- `drawables` and `shapes`: the render collectors, read-only.
- Entities and the entity utilities do not take the game. They take `MegaServices` (`MegaServices.kt`): one holder of the same session objects (`engine`, `events`, `assets`, `audio`, `gameState`, `players`, `gravity`, `levelState`, `clock`, `settings`, `updatables`, `runQueue`, `groups`, `cameraController`, `rules`, and `gameCamera`, typed as the read-only engine interface `ICameraView` (`engine/.../camera/ICameraView.kt`; it also gives `rotating`; `core` adds the pooled `getRotatedBounds()`, `toGameRectangle()` and `overlaps(bounds)` in `utils/extensions/CameraExtensions.kt`)). `gameCamera` is for reads; an entity asks for a smooth camera move with `services.cameraController.startSmoothFollow()` and `stopSmoothFollow()`. `postCreate()` builds it once, before the entity pools and `Megaman`. Inside an entity and an entity utility it is named `services` (`MegaGameEntity.services`). Screens, layer builders, backgrounds, `ScreenController` and the launcher keep the game. `rules` is the read-only `SessionRules` of the session (`state/SessionRules.kt`): each `GameMode` (`SINGLE`, `COOP`, `PVP`) has one set, with a typed field for each toggle, and the launch chooses the mode. The contact rules take `players`, `gravity` and the rules (`megaContactRules(players, gravity, rules, debugFilter)`), and the pathfinder factory takes `levelState` (`MegaPathfinderFactory(levelState)`); `createGameEngine()` builds both.
- The game has no property bag (it is not an `IPropertizable`). Add a typed field, not a string key.
- `screens` (keyed by `ScreenEnum.name`), `viewports` (keys `ConstKeys.GAME` and `ConstKeys.UI`), and `runQueue`.
- `createGameEngine()` sets the system order: Controller, Behaviors, World, Cullables, Motion, Pathfinding, Points, Updatables, Fonts, Animations, Sprites, DrawableShapes, Audio, Groups.

Frame order in `render()`: drain `runQueue`, then `currentScreen.render(delta)` (the level screen calls `engine.update(delta)`), then `events.run()`.

### Engine ECS (`engine/`)

- `GameEngine` queues spawns and destroys. It processes them at the start of `update()`, then updates each `GameSystem`.
- `GameSystem` takes a component mask. An entity joins a system only if it has all components in the mask. A system can override `onEntityJoined`/`onEntityLeft`; they fire when an entity really joins or leaves (in the engine, right after `onSpawn` and before the component `reset()` at destroy).
- Entity groups: a `GroupsComponent` holds an entity's group names; `GroupsSystem` keeps the group index. An entity looks up with `services.groups.getEntitiesInGroup(name)` (the game itself uses `getSystem(GroupsSystem::class)`). `MegaGameEntity` puts the `ConstKeys.GROUP` spawn property into the component.
- `GameEntity` stores components in a map keyed by `KClass`. Add them with `addComponent(...)`.
- Components: `BodyComponent`, `SpritesComponent`, `AnimationsComponent`, `UpdatablesComponent`, `BehaviorsComponent`, `ControllerComponent`, `CullablesComponent`, `PointsComponent`, `DrawableShapesComponent`, `FontsComponent`, `PathfindingComponent`, and others. In `core`, `CrusherComponent` (`entities/state/`) makes an entity a crusher: it kills a player on a set side at once, with an optional test of the opposite side.
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
- `IMegaGameEntity` holds only the tag and the `EntityType` (item `072` removes it). `players` is a member of `MegaGameEntity`, and the extension `targetPlayer` takes a `MegaGameEntity` (see `docs/mega-refactor/items/029-player-contract/`); `overlapsGameCamera()` takes a `MegaGameEntity` that is also an `IBodyEntity`. An entity plays a sound with `services.audio.playSound(...)` and reads the camera with `services.gameCamera`.

Entity packages match `EntityType` (`entities/EntityType.kt`): `blocks`, `enemies` (about 127 files), `bosses`, `hazards`, `items`, `projectiles`, `sensors`, `special`, `decorations`, `explosions`. The player is in `entities/megaman/` (behaviors, components, constants, handlers, sprites). The second player class, X, is in `entities/megamanx/` (item `081`). `LevelDefinition.characters` (default `setOf(MEGAMAN)`) names the character of a level, and `PlayerRoster` (`players/`) puts it in slot 0 at `startLevel`; `X6_INTRO` sets X.

### Factories

- Use `MegaEntityFactory` (`entities/MegaEntityFactory.kt`). It makes one pool per class and calls the primary constructor `(services: MegaServices)` through reflection (`EntityConstructorsTest` checks every entity class). Example: `MegaEntityFactory.fetch(Bullet::class)!!.spawn(props(...))`.
- A pooled entity goes back to its pool through `runnablesOnDestroy[ConstKeys.POOL]` (see `GameEntityPoolCreator`).
- `EntityFactories` (`entities/factories/EntityFactories.kt`) is deprecated. Do not add new entries to it. Some old code still calls it.

### Events

- `EventsManager` (engine) is pub/sub. Listeners implement `IEventListener` with `eventKeyMask: ObjectSet<Any>` and `onEvent(event)`.
- An entity submits with `services.events.submitEvent(Event(EventType.X, props(...)))`. A screen uses `game.events`. Event keys are in `events/EventType.kt`.
- Events are buffered. Listeners get them only when `events.run()` runs, after the screen render. Listener order is registration order.
- Register in `onSpawn` (`services.events.addListener(this)`) and remove in `onDestroy`.
- Level exits go through the level screen's `onEvent`: `END_LEVEL` (the next screen from `screenOnCompletion`) and `GAME_COMPLETE` (the ending screen; `DrWily` submits it after the final boss). Entities do not call `setCurrentScreen`.

### Levels and Tiled pipeline

- Each level is an entry in `levels/LevelDefinition.kt`. `tmxMapSource` is a file in `assets/tiled_maps/tmx/`. The entry also sets `type`, `music`, `screenOnCompletion` (a function of the `LevelState` that gives the next screen; `LevelCompletionScreensTest` lists the screen of each level) and an optional `levelOverride`.
- `assets/tiled_maps/tmx/` has many old map versions (for example `MoonMan_16x14_v5.tmx`). Only the file named in `LevelDefinition` is live.
- `screens/levels/MegaLevelScreen.kt` loads the map. `screens/levels/tiled/layers/MegaMapLayerBuilders.kt` maps layer names (`ConstKeys` values such as `game_rooms`, `player`, `enemies`, `blocks`, `triggers`) to builders. It builds layers in a fixed order and skips unknown layers. Layers named `blocks_*` use the `blocks` builder.
- `SpawnersLayerBuilder`: the Tiled object `name` is the entity class simple name. It resolves to `com.megaman.maverick.game.entities.<package>.<name>` from the layer's `EntityType`. Object properties become the `spawnProps`.
- Room transitions are handled by the camera parts `CameraRoomBounds` and `CameraRoomTransition`; `LevelRoomHandler` does their side effects.
- Camera zones: objects with `type=zone` in the `camera_sections` layer (`CameraSectionsLayerBuilder`). `min_x`, `max_x`, `min_y` and `max_y` are references to Tiled objects, and each one sets a view edge: a `min_*` property reads the low edge of its object, and a `max_*` property reads the high edge. Point `min_y` and `max_y` at one rectangle of the view height to lock the view to that rectangle. The objects can stay in the same layer, with no `type`. A zone acts while the focus point is inside it. Active zones apply in layer order, and a later zone wins per edge. A zone never changes the current room, and the view never goes past the room. `speed` sets the ease in tiles per second. `edge_blocks=left,right` puts a players-only `CameraEdgeBlock` on these edges (not at a room edge). A block covers the zone rectangle across its edge, so draw a zone over every position the block must hold.
- Camera scrolls: objects with `type=scroll` in the `camera_sections` layer, with `direction` (`left`, `right`, `up`, `down`) and `speed` in tiles per second. A section starts when the focus point enters it, and it takes control of its axis from the current camera position; the other axis keeps its follow and its limits. One section runs at a time. It stops when the trailing view edge reaches the far edge of the rectangle, or the leading edge with `stop=arrive`. Then the trailing edge stays as a limit. Both edges have a players-only `CameraEdgeBlock` by default, which covers the section rectangle across its edge. An edge in the fall direction has no block, and a player fully past it dies, except at a room edge if it is the leading edge. A player fully past the trailing edge of a running section dies. `edge_blocks` overrides the blocks. The tables are on `CameraScroll` and in the `051` item `NOTES.md`. `next` is a one-way object reference to the section that starts when this one stops. A room edge stops the scroll, and it continues after the pan if the section still holds the focus. The scroll waits for a pan, a gate, a camera turn and a player who is not ready.
- Crush surfaces: objects with `type=crush_surface` in the `camera_sections` layer, with `section` as an object reference to a zone or a scroll section. A player dies only when an edge block moves into the view and pins them against a surface whose section is active (a scroll that runs, or a zone that holds the focus). A solid with no surface does not crush. Put a surface on each face that must kill.

### Screens

`screens/ScreenEnum.kt` lists all screens. They are registered in `MegamanMaverickGame.postCreate()`. Change screens with `game.setCurrentScreen(ScreenEnum.X.name)`. Start a level with `game.startLevel(LevelDefinition.X)`.

### Assets

- `assets/TextureAsset.kt` (atlases in `assets/sprites/sprite_sheets/`), `SoundAsset.kt` (`assets/sounds/`), `MusicAsset.kt` (`assets/music/`). All implement `IAsset` (`source`, `assClass`).
- Get a texture region with `services.assets.getTextureAtlas(TextureAsset.X.source).findRegion("$TAG/name")` (in a screen, `game.assets`).
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
- Fail fast. A state that only a mistake can cause (bad map data, a wrong label, a missing entity) throws at once; do not fall back to a default with `as?` or a silent `return`. Where the raw error would be unclear (for example a bare `ClassCastException`), catch it and throw a custom exception made for that case. Its message says what is wrong and how to fix it, and the original error is its `cause`. Put the class next to the code that throws it (examples: `engine/.../components/MissingComponentException.kt`, `core/.../world/collisions/InvalidSlopeLabelException.kt`), and test that it is thrown.
- Keep an interface method the one entry point where callers use it. Do not add an overload or a second method next to it to pass one more value (for example a `draw(batch, offset)` next to `override fun draw(batch)`). Put the value in a field of the object, which the caller sets before the call (as `Background.cameraDrawOffset` and `GameSprite.drawOffsetX`), or change the interface for all of its implementations. Then each caller goes through the same contract, and a later change cannot miss a path.

## Package READMEs

A package can have a `README.md` next to its source files that explains and illustrates its design. The first one is `core/.../fights/README.md` (2026-11-02); use it as the model.

The owner started this practice long after most of the repo was built, so most packages have no README. Fill them in lazily: do not write READMEs for old packages as a task of their own.

- **Write one** when you add a new package or a new design that a reader cannot see from one file: several types that work together, a life cycle, an order that matters, or rules a change must keep.
- **Also write one** when a branch makes a large change to an old package, if the design is not clear from the code.
- **Skip it** for a package of similar classes (for example `entities/enemies/`), a single class, or a design that the code already shows.
- **Contents:** the purpose and why the design is as it is; the current status (for example which parts are not moved yet); the parts and how they connect; the life cycle or the order in a frame; how to change the package; the rules a change must keep. Leave out what each class or function does line by line: that belongs in the code.
- **Stay generic:** show the design with a hypothetical example, and name only the core types that define it. Do not list the current steps, entries, counts or call sites, because they drift. Point to the file that holds the real list.
- **Diagrams:** Mermaid in `mermaid` code blocks: `flowchart` for parts, `stateDiagram-v2` for a life cycle, `sequenceDiagram` for an order in time.
- **Links:** point to the item folder in `docs/mega-refactor/` for the research and the decisions. Do not copy them.
- **Keep it true:** a branch that changes the design of a package with a README updates the README in the same branch.

## Gotchas

- `GameLogger.filterByTag` is `true`. Debug and log messages print only if the tag is in `MegamanMaverickGame.TAGS_TO_LOG`. Errors always print.
- `runQueue` is drained with `removeLast()`, so the last runnable added runs first.
- Pooled entities are reused. Reset all per-spawn state in `onSpawn`, not in the constructor or `init()`.
- After `destroy()`, the entity stays in the engine and its systems until the next `engine.update()`. Its `existenceState` changes at once. The setter is public, but only the engine should write it: `existenceState.liveNow` = in the engine now, `existenceState.liveAfterUpdate` = in the engine after the next update.
- The `Megaman` player is slot-owned: it is created, initialized and registered manually in `postCreate()`. Do not fetch it from a factory. A pool treats `destroy` as release, and a slot keeps its player through death.
- Tiled entity names must match the Kotlin class name and the package for the layer exactly. A wrong name logs an error and skips the spawner.

## Project skills (`.claude/skills/`)

- `level-designer`: design or edit a level.
- `level-builder`: build or structurally edit a level TMX file (rooms, blocks, spawns).
- `level-analyzer`: read and analyze an existing TMX level.
- `gauntlet-stages`: gauntlet stages that use `GauntletMaster`.
- `diagnostics-analyzer`: analyze `*-diagnostics.txt` files from `--diagnostics` runs.
- `sprite-slicer`: cut a tightly packed sprite sheet into fixed-size animation strips and an atlas.
