# Scan: `016d` (systems take narrow services)

Base: the parent `mega-refactor-016-master` at `48f7e72f5` (the branch start; it contains the merged `016a`, `016b` and
`016c`). `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`,
`MMG` = `C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`. Line numbers are on the base.

Method: `grep` for `MegamanMaverickGame` in `core` main, `core` test and `lwjgl3`; a read of each hit outside the
screens; a read of `create()`, `postCreate()` and `createGameEngine()` in `MMG`; a scratch script (not committed) that
finds the rooms of the live TMX files that hold `QuickSand`, `GravityChange`, `GravitySwitcharoo` and the pathfinding
enemies, and the rooms with a player spawn.

Baseline on the base: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
lwjgl3:jar` passes; 575 engine and 259 core tests, 0 failures, 0 errors, 0 skipped; 81 unique warnings. Same as the
parent baseline.

## 1. Short answer

- Outside the screens, only 4 main files name the host in code and are not host UI: `MegaContactRules.kt`,
  `MegaPathfinderFactory.kt`, `LevelDefinition.kt` and `RobotMasterLevelOnCompletion.kt`. The other hits are the
  host itself (`MMG`), host UI (13 backgrounds, `ScreenController`), the launcher, and 5 entity files that name it only
  in commented-out code.
- The contact rules read 2 host members: `players` and `gravity`. The pathfinder factory reads 1: `levelState`.
- `levelState` is already a field that is built with the game object (`MMG:183`). The pathfinder factory can take it
  with no change to the creation order.
- `players` and `gravity` are `lateinit var` (`MMG:220-221`), set in `postCreate()` (`MMG:401-402`). The engine, and
  with it the contact rules, is built in `create()` (`MMG:305`). So the two objects must exist earlier. Their
  constructors have no side effect, and no code reads them between `create()` and `postCreate()` (section 4).
- `MegaServices` cannot go to the contact rules: it holds `engine`, and the engine is built from the rules (a cycle).
- `LevelDefinition.screenOnCompletion` is `(MegamanMaverickGame) -> ScreenEnum`. Of its 19 live values, 9 call
  `RobotMasterLevelOnCompletion`, which reads only `game.levelState.weaponsAttained`; 10 are constants. Only the level
  screen calls it (`MLS:878`).
- 2 tests mock the host only for the contact rules: `ContactRuleSelectionTest.kt:54`, `CartV2ItemTest.kt:84`.
- The `GRAVITY_CHANGE` fixture (from `GravityChange`) is in no real level. Only `Test1.tmx` has it, in rooms with no
  player spawn (section 7).

## 2. Where the host type is named (code lines, not comments)

| Where | Files | Kind |
|---|---|---|
| `C/world/contacts/MegaContactRules.kt:13`, `:20` | 1 | system code, this branch |
| `C/pathfinding/MegaPathfinderFactory.kt:13`, `:16` | 1 | system code, this branch |
| `C/levels/LevelDefinition.kt:8`, `:22` | 1 | level data (open question) |
| `C/levels/RobotMasterLevelOnCompletion.kt:3`, `:6`, `:8` | 1 | level data (open question) |
| `C/screens/` | 32 | host UI (decision 8) |
| `C/drawables/backgrounds/` | 13 | host UI, built by `BackgroundLayerBuilder` |
| `C/controllers/ScreenController.kt:25`, `:28` | 1 | host UI |
| `MMG` | 1 | the host |
| `lwjgl3/.../DesktopLauncher.kt:15`, `:17`, `:18`, `:135`, `:167` | 1 | the launcher |
| Commented-out code only | 5 | `MechaDragon_OLD`, `ReactorMan_OLD`, `SphinxMiniBoss`, `MegamanTrailSprite`, `GravityCapsule`; not changed |

No file in `C/utils/`, `C/world/` (other than the rules), `C/drawables/` (other than the backgrounds), `C/entities/`
or `C/pathfinding/` (other than the factory) names the host. `016c` moved `MegaUtilMethods.delayRun`,
`GameEntityUtils` and `AnimatedBlock` to `MegaServices`, so the item `NOTES.md` list for `016d` ("`MegaUtilMethods.delayRun`
and the spawn-delay functions, `GameEntityUtils`, `AnimatedBlockAnimators`") is already done.

## 3. The reads

### 3.1 Contact rules

`megaContactRules(game: MegamanMaverickGame, debugFilter)` (`MegaContactRules.kt:20`) builds the rule table once. It
keeps `game` only inside 2 closures:

- `:40` `FeetSandRule { game.players.any { it.inPlay } }`: the rule asks on each `BEGIN` of a feet-sand contact whether
  a sand splash spawns (`FeetSandRule.kt:35`).
- `:55-56` `BodyGravityChangeRule { entity, direction -> if (entity.asPlayer != null) game.gravity.set(direction) else
  entity.direction = direction }`: a player in a gravity change area sets the world gravity (`WorldGravity.set` sets
  the direction of every player); any other `IDirectional` entity gets only its own direction.

Both closures read `game.players` and `game.gravity` when a contact happens, not when the table is built. Today this is
what makes the build order work.

Caller: `MMG:926` inside `createGameEngine()`, as `megaContactRules(this, CONTACT_LISTENER_DEBUG_FILTER)`.

### 3.2 Pathfinder factory

`MegaPathfinderFactory(private val game: MegamanMaverickGame)` (`MegaPathfinderFactory.kt:16`):

- `:26`, `:28`: `game.levelState.worldContainer` (read twice in the same call, the same value; the snapshot copy is
  made once per frame or when the container object changes).
- `:42`: `game.levelState.tiledMapLoadResult!!` (world width and height).

`getPathfinder` runs on the main thread, in `AbstractPathfindingSystem.update` (`engine/.../AbstractPathfindingSystem.kt:49`),
also for the async system (only the path search runs on a worker). Callers: `MMG:945` (async) and `MMG:951` (sync),
one of them by `params.pathfinding`.

### 3.3 `LevelDefinition.screenOnCompletion`

- Type: `val screenOnCompletion: (MegamanMaverickGame) -> ScreenEnum` (`LevelDefinition.kt:22`).
- Values (19 live entries): 9 robot master levels `{ game -> RobotMasterLevelOnCompletion.invoke(game) }` (`:32` to
  `:96`); 10 constants: `SAVE_GAME_SCREEN` 7 (the 3 Wily stages, `TEST_1`, `TEST_2`, `REFACTOR_TEST_029`,
  `REFACTOR_TEST_031`), `LEVEL_SCREEN` 1 (`SMB3_1`), `EXTRAS_SCREEN` 2 (the gauntlets). A 20th line is in
  commented-out code (`:190`).
- `RobotMasterLevelOnCompletion` (`C/levels/RobotMasterLevelOnCompletion.kt:8-10`) reads only
  `game.levelState.weaponsAttained`.
- Flow: `MMG:1013` copies the value into `MLS.screenOnCompletion` (`MLS:179`, same type); `MLS:878` calls
  `screenOnCompletion.invoke(game)` on `END_LEVEL`.
- `LevelState.currentLevel` is a `LevelDefinition`, and `MegaServices` holds `LevelState`. So through this one function
  type, the level data that entities read still names the host type.

## 4. Creation order

`create()` (`MMG:252-392`), in order: `settings.performance` (`:253`); launch options; log setup; `shapeRenderer`,
`batch`; input (`:291-297`, with `controllerPoller = FrameControllerPoller { players.inputs[PlayerSlot(0)] }`, a
closure that reads `players` late); `eventsMan` (`:299`); `diagnostics`; `engine = createGameEngine()` (`:305`);
`state = GameState()` (`:306`); cameras and viewports; fonts; `assMan` and the asset queue; log writers.

`postCreate()` (`MMG:394`, called from `render()` at `:510` when the assets finish loading): `audioMan` (`:397`);
`players = MegaPlayers()` (`:401`); `gravity = WorldGravity(players)` (`:402`); `MegaServices(...)` (`:404-420`);
`MegaEntityFactory.init`; `EntityFactories.initialize`; `Megaman(services)`, `init()`; `state.addListener(megaman)`;
`players.register(PlayerSlot(0), megaman)` (`:436`); screens.

Fields built with the game object (no `create()` needed): `levelState` (`:183`), `clock`, `camera`, `settings`
(`:184-186`), `updatables`, `runQueue`, `viewports`, `screens`.

Side effects of the two constructors:

- `MegaPlayers(capacity = 1, inputs = PlayerInputs(capacity))` (`C/players/MegaPlayers.kt:15`): the engine `Players`
  constructor allocates an array and checks the capacity (`engine/.../players/Players.kt:7-11`); `PlayerInputs` allocates
  a `LongArray` (`C/input/PlayerInputs.kt:8`); a `Signal` and a payload object. No registration, no listener, no
  `Gdx` call, no global state.
- `WorldGravity(players)` (`C/players/WorldGravity.kt`): stores the reference; `direction = UP`. No side effect.

Reads of `players` and `gravity` between `create()` and the end of `postCreate()`: none. The `controllerPoller` closure
runs only from `render()` after `postCreate()` (`MMG:561`, in the branch after the assets load). A read before
`postCreate()` would throw today (`lateinit`), and the game does not throw. `GameState` has no reference to `players`;
the `Megaman` listener and registration stay in `postCreate()`.

Writes: `players` and `gravity` are written only at `MMG:401-402`. No test writes them; tests that need them build a
real `MegaPlayers` (`TestServices.kt:29-30`, `MegaPlayersTest`) or stub the getter of a mocked game.

## 5. Tests

| Test | Host use | After this branch |
|---|---|---|
| `T/world/contacts/ContactRuleSelectionTest.kt:54` | `megaContactRules(mockk<MegamanMaverickGame>(relaxed = true))` | real `MegaPlayers()` and `WorldGravity(players)`; the reference file does not change |
| `T/entities/special/CartV2ItemTest.kt:84` | the same, to build a `WorldSystem` | the same objects as its `testServices(...)` |
| `T/screens/levels/MegaLevelScreenEventsTest.kt` | mocks the host for a screen | unchanged (decision 8) |
| `T/levels/LevelStateTest.kt:95`, `T/state/GameClockTest.kt:17`, `T/state/GameSettingsTest.kt:25`, `T/screens/levels/camera/GameCameraStateTest.kt:28` | build a real `MegamanMaverickGame()` to check a session field | unchanged; the same kind of test can check `players` and `gravity` |

No test covers the 2 closures of the contact rules, the pathfinder factory or `screenOnCompletion`. `MegaPlayersTest`
covers `WorldGravity.set`. `T/world/contacts/rules/` has rule tests for feet-block, gate, ice, player-item and water.

## 6. What still names the host after this branch (input for `001b`)

If the open questions are answered as recommended in `SCOPE.md`:

- `MMG` (the host).
- Screens, 32 files in `C/screens/`: `MLS` and its handlers (`LevelStateHandler`, the 4 event handlers in
  `screens/levels/events/`, `BossHealthHandler`, `PlayerStatsHandler`), the layer builders (`MegaMapLayerBuilders`,
  `SpawnersLayerBuilder`), the menu screens (9), the other screens (12), `GameSettingsPanel`.
- Backgrounds, 13 files in `C/drawables/backgrounds/`. They read `game.assMan` (10) and `game.isPixelPerfect()` (1,
  `Background.kt:73`).
- `C/controllers/ScreenController.kt`: `game.batch`, `game.assMan`, `game.controllerPoller`.
- `lwjgl3/.../DesktopLauncher.kt`: builds the game and its params; names `RunType`.
- Not `LevelDefinition` and `RobotMasterLevelOnCompletion`: `screenOnCompletion` takes `LevelState` (owner,
  2026-10-29; `SCOPE.md` open question 4).
- Commented-out code in 5 entity files (not code).

## 7. Manual check locations (live TMX files)

| What | Where (level, rooms with a player spawn) |
|---|---|
| `QuickSand` (feet-sand contact, splash) | `DESERT_MAN`: `room1`, `room2`, `room8`, `room13`, `room16`; `WILY_STAGE_1`: `room7`, `room8` |
| `GravityChange` (body-gravity-change contact) | none. `TEST_1` (`Test1.tmx`) has it in `room_7` (with `Met`, `Peat`, `ShieldAttacker`, `SniperJoe`), `room9`, `room12`; none of these rooms has a player spawn. `DecoratedGravityForce` (which builds a `GravityChange`) is in no live map |
| `GravitySwitcharoo` (uses `game.gravity` from an entity, not through the rules) | `REFACTOR_TEST_029`: `gravity_switch`; `WILY_STAGE_3`: `room2`, `room3`, `room5` |
| `Bat` (pathfinding) | `DESERT_MAN`: `room10` (20 `Bat`), `room8`, `room9`, `room13`; `REFACTOR_TEST_029`: `enemy_chase`; `TIMBER_WOMAN`: `room1` |
| `SubmarineJoe` (pathfinding) | `TEST_1` `room29`, `WILY_STAGE_2` `path1_room1`; neither has a player spawn |
| `FloatingCan`, `DeathBat` (pathfinding) | the gauntlets and `TEST_1` only, in rooms with no player spawn |

## 8. Other branches

No other code branch is active (`git worktree list`: the main checkout, the parent and this branch). `001b` waits for
`016`; section 6 is its input.
