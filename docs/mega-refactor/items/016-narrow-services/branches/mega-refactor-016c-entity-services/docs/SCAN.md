# Scan: `016c` (entities take `MegaServices`)

Base: the parent `mega-refactor-016-master` at `9ba76c833` (the branch start; it contains the merged `016a` and
`016b`). `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`,
`MMG` = `C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`. Line numbers are on the base.

Method: a script (scratch, not committed) lists every `game.<member>` read in `C/entities/`. It skips comment,
`import` and `package` lines and package paths. `grep` finds the constructor headers, the calls that pass `game` as
an argument, the reads through another reference (`entity.game`) and the callers of each game function. Two scratch
probes ran on the base and were deleted at once (`git status` clean after each): a test that builds every concrete
`MegaGameEntity` class by its primary constructor (section 7), and a compile of the `IMegaGameEntity` cleanup
(section 6).

Baseline on the base: `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
lwjgl3:jar` passes; 575 engine and 249 core tests, 0 failures; 81 unique warnings. Same as the parent baseline.

## 1. Short answer

- The swap is still a type change in about 450 files, but it is not type-only today. Entities read 8 kinds of game
  members that a holder with the same member names cannot answer with no edit, because decision 11 forbids forwards:
  the `016a` forwards (`getCurrentLevel()`, `getCurrentRoom()`, `getRooms()`, `getTiledMapLoadResult()`,
  `isCameraRotating()`, `setFocusSnappedAway()`), `getPerformance()`, and `getSystem(GroupsSystem::class)`. These need
  a real home first (section 4). `getGameCamera()` is not a forward (it reads the viewport); the holder can keep the
  name.
- Three values have no object home yet: the two camera flags (private fields of the game) and `weaponsAttained` (a
  `var` of the game that `Megaman` writes). Performance is a field of `params`. Each needs a home that the game, the
  screens and the holder can share (section 4).
- The `IMegaGameEntity` cleanup is small. A compile probe showed that only 2 lines break when `game` and the 3 defaults
  leave the interface and the extension receivers become `MegaGameEntity`; both compile with a generic bound and no
  call site change (section 6).
- All 436 concrete entity classes in `C/entities/` build with a one-argument primary constructor today. The only
  exception is `PushableBlock.InnerBlock` (private, 2 parameters, built directly, not pooled) (section 7).
- 17 test files build entities with a relaxed `MegamanMaverickGame` mock (section 8).

## 2. Where the game type is named

| Where | Count |
|---|---|
| `C/entities/` files that name `MegamanMaverickGame` in code | 451 (all with one `import` line) |
| `C/entities/` files that name it only in a comment (commented-out code) | 5 (`SphinxMiniBoss`, `ReactorMan_OLD`, `MechaDragon_OLD`, `GravityCapsule`, `MegamanTrailSprite`) |
| Code lines in `C/entities/` (not `import`) | 461 |
| One-line constructor headers `(game: MegamanMaverickGame)` | 442 |
| `(private val game: MegamanMaverickGame)` | 11 (the 9 old factories, `MegamanAnimations`, `ToxicWaterWaveAnimator`) |
| `MegaGameEntity(override val game: MegamanMaverickGame)` | 1 (`MegaGameEntity.kt:16`) |
| Other lines | `IMegaGameEntity.kt:13` (`val game`), multi-line base headers (`AbstractEnemy.kt:73`, `AbstractBoss.kt:47`, `AbstractHealthEntity.kt:20`, `AbstractProjectile.kt:13`), `PushableBlock.kt:71` (`InnerBlock`), `MegamanWeaponsHandler.kt:142` (a getter), `MegaEntityFactory.kt:22`, `:27`, `GameEntityUtils.kt:179`, `:189` (function parameters) |
| Outside `C/entities/`, needed by entities | `C/utils/MegaUtilMethods.kt:15` (`delayRun(game: MegamanMaverickGame, ...)`; only entities call it, 7 calls) |
| Outside `C/entities/`, host (keep, decision 8 and `016d`) | screens and their handlers (32 files), layer builders, 13 backgrounds, `ScreenController`, `RobotMasterLevelOnCompletion`, `LevelDefinition.screenOnCompletion`, `megaContactRules` (`016d`), `MegaPathfinderFactory` (`016d`), `DesktopLauncher` |

Calls that pass `game` on (they compile after the swap if the parameter type changes with them): `delayRun` 7
(`WilyFinalBoss` 3, `MegamanWeaponsHandler` 2, `CannoHoney` 2), `isNextPossibleSpawnDelayed` 2 and
`delayNextPossibleSpawn` 2 (`Jetto`, `BikerKibbo`), the 9 old factories (`EntityFactories.kt:15-23`), and direct
constructions of entity classes (`SigmaRatClaw(game)`, `GutsTankFist(game)`, `Bolt(game)`, `InnerBlock(game, body)`,
`ToxicWaterWaveAnimator(game)`, `ShieldEntity(entity.game)`, `Megaman(this)` at `MMG:413`).

## 3. Entity reads of `game.<member>` today

| Member | Reads (files) | Answer after the swap |
|---|---|---|
| `assMan` | 391 (372) | same object, same name |
| `eventsMan` | 118 (55) | same object, same name |
| `state` | 110 (48) | same object, same name |
| `getGameCamera()` | 70 (54) | the camera object (section 4.6) |
| `isCameraRotating()` | 39 (33) | `016a` forward; needs a home (section 4.1) |
| `levelState` | 34 (22) | same object, same name |
| `gravity` | 26 (18) | same object, same name |
| `audioMan` | 16 (11) | same object, same name |
| `getCurrentLevel()` | 16 (10) | `016a` forward (section 4.3) |
| `getCurrentRoom()` | 12 (9) | `016a` forward (section 4.3) |
| `getPerformance()` | 8 (8) (`.fixedStep` 7, `.fps` 1) | value of `params`, changes at run time (section 4.2) |
| `setFocusSnappedAway()` | 7 (3) | `016a` forward (section 4.1) |
| `updatables` | 7 (3) | same object, same name |
| `players` | 5 (3) | same object, same name |
| `engine` | 2 (2) | same object, same name |
| `getTiledMapLoadResult()` | 2 (1) | `016a` forward (section 4.3) |
| `getSystem(GroupsSystem::class)!!` | 1 (`GameEntityUtils.kt:81`) | the `GroupsSystem` object (section 4.5) |
| `runQueue` | 1 | same object, same name |
| `clock` | 1 (`Starkner.kt:414`) | same object, same name |
| `getRooms(out)` | 1 (`SwitchGate.kt:100`) | `016a` forward (section 4.3) |
| `weaponsAttained` | 1 (`Megaman.kt:921`, a write) | a `var` of the game (section 4.4) |

Reads through another reference: `AbstractEnemy.kt:104` (`it.game.state`), `AnimatedBlock.kt:119`
(`block.game.assMan`), `MegamanJetpackHandler.kt:32`, `MegamanDamageNegotiator.kt:24`, `MegamanWeaponsHandler.kt:143`
(`megaman.game`), `GameEntityUtils.kt:51` (after an `is MegaGameEntity` check), `:79` (a cast),
`FreezableEntityHandler.kt:53` and `LightSourceUtils.kt:56` (through an interface type), `IMegaGameEntity.kt:23`,
`:28`, `:31`, and outside `C/entities/`: `C/world/body/IBodyExtensions.kt:19`, `:33` (`getEntity().game`, reads
`isCameraRotating()` and `getGameCamera()`).

## 4. Values and forwards that need a home

### 4.1 Camera flags

- `MMG:195-196`: `private var cameraRotating`, `private var focusSnappedAway`, behind `setCameraRotating`,
  `isCameraRotating`, `setFocusSnappedAway`, `isFocusSnappedAway` (`MMG:1018-1022`, `:1087-1091`).
- Writers: `MMG:320-324` (the camera's `onJustFinishedRotating` clears both), `MLS:440` (level start, rotating
  `false`), `MLS:918` (`START_GAME_CAM_ROTATION`, rotating `true`), `MLS:488`, `:603`, `:992`, `:1138` (focus `false`),
  and the entities `Megaman.kt:211`, `:504`, `:508`, `ClimbBehavior.kt:142`, `:215`, `PipePortal.kt:128`, `:150`
  (focus).
- Readers: `isCameraRotating()` 39 code reads in entities (one more inside a log string,
  `ControllerComponent.kt:80`; 2 more in comments in `GravityCapsule.kt`) and `IBodyExtensions.kt:20`;
  `isFocusSnappedAway()` 2 (`MLS:280`, `:988`).
- Both are level values: the level start clears them, as it clears the other `LevelState` values.
- `RotatableCamera` (`C/screens/levels/camera/RotatableCamera.kt`) has its own `rotFinished`, but it is not the same
  value as `cameraRotating` (the flag is set by the event and cleared at level start, also when a rotation did not
  finish).

### 4.2 Performance

- `MMG:1102` `getPerformance() = params.performance`. Written at launch (`DesktopLauncher.kt:137`, before the game
  object exists) and by `setPerformance` (`MMG:1104-1112`: auto-perf at `MMG:526-528` and the settings panel
  `GameSettingsPanel.kt:299`, `:306`). `setPerformance` also sets the FPS, the `WorldSystem` fixed step, submits
  `CHANGE_PERFORMANCE_MODE` and resets the auto-perf timers.
- Readers: 8 entities, `MMG` 4 (auto-perf, `createGameEngine` 3, `isPerformanceSubpar`), `GameSettingsPanel` 3.
- `GameClock` (`C/state/GameClock.kt`, from `016b`) holds `paused`. The item text names `clock` as the home of the
  performance read.

### 4.3 The `016a` level forwards

Callers in `core` main (and `core` test):

| Function | Body | Callers |
|---|---|---|
| `getCurrentLevel()` | `levelState.currentLevel!!` | 26 (16 entities incl. `MegaGameEntity.canSpawn`, `MMG` 2, `MLS` 3, `EndLevelEventHandler`, `LevelPauseScreen`, `SaveGameScreen`, `RobotMasterIntroScreen`, `RobotMasterIntroScreenV2`) (12 test stubs) |
| `setCurrentLevel(x)` | `levelState.currentLevel = x` | 9 (`MMG` 3, 5 screens) (2 test) |
| `isInExtraLevel()` | `levelState.currentLevel?.type == EXTRA_LEVEL` | 4 (`LevelPauseScreen`, `SaveGameScreen` 3) (2 test) |
| `getCurrentRoom()` | `levelState.currentRoomSupplier?.invoke()` | 13 (12 entities, `SpawnersLayerBuilder`) (3 test) |
| `getRooms(out)` | adds the supplier's rooms to `out` | 1 (`SwitchGate.kt:100`) (4 test) |
| `setRoomsSupplier`, `setCurrentRoomSupplier` | write the supplier | 1 each (`MLS:441-442`) |
| `getWorldContainer()`, `setWorldContainer` | `levelState.worldContainer` | 3 (`MMG:920`, `MegaPathfinderFactory.kt:26`, `:28`), 1 (`MLS`) (1 test) |
| `getTiledMapLoadResult()`, `setTiledMapLoadResult` | `levelState.tiledMapLoadResult!!` | 3 (`Megaman.kt:739-740`, `MegaPathfinderFactory.kt:42`), 1 (`MLS:445`) (1 test) |

`T/levels/LevelStateTest.kt:41-108` tests these game functions (`016a` permanent test).

### 4.4 `weaponsAttained`

- `MMG:227` `var weaponsAttained: OrderedSet<MegamanWeapon>? = null` (`016a`, decision 10: typed, not saved, outside
  `GameState`).
- Writers: `Megaman.kt:921` (an entity, at level defeat), `GetWeaponScreen.kt:152`, `MainMenuScreen.kt:161`,
  `MMG:796` (`resetPlayerProgress`), all clear or set. Readers: `RobotMasterLevelOnCompletion.kt:9`,
  `GetWeaponScreen.kt:145`.
- An entity writes it and screens read it, so its home must be an object that both reach. It is the reward of the
  level just beaten; it crosses from the level screen to the get-weapon screen.

### 4.5 Groups

- `GameEntityUtils.kt:81` `game.getSystem(GroupsSystem::class)!!`. `GroupsSystem` is built once in
  `createGameEngine` (`MMG:971`); the engine keeps its systems for the session.

### 4.6 Game camera

- `MMG:1016` `getGameCamera() = viewports.get(GAME).camera as RotatableCamera`. The camera object is built once in
  `create()` (`MMG:320`); `TOGGLE_PIXEL_PERFECT` makes new viewports with the same camera (`MMG:681-698`). So a
  direct reference is safe for the session.
- Readers: 70 entity reads through `game`, 4 bare calls of the `IMegaGameEntity` default, 2 through a cast
  (`GameEntityUtils.kt:58`, `:64`), `IBodyExtensions.kt:34`, `MLS:235`, `MMG:681`.
- It is not a `016a` forward (the viewport map is the home of the viewports, and the camera belongs to the viewport).

### 4.7 Host accessors of private fields

`getDrawables()` (2 callers), `getShapes()` (1), `getSystems()` (1, inside `getSystem`) return private fields of the
game. No entity calls them. Decision 11 names "the other functions that now read `levelState` or private fields".

## 5. Creation order

`postCreate` today (`MMG:400-424`): `audioMan` (`:404`) → `MegaEntityFactory.init(this)` (`:408`, builds and `init()`s
25 `Bullet` and 25 `Block`) → `EntityFactories.initialize(this)` (`:411`) → `Megaman(this)` and `init()` (`:413-415`) →
`state.addListener(megaman)` → `players = MegaPlayers()` (`:420`) → `players.register(...)` (`:423`) →
`gravity = WorldGravity(players)` (`:424`).

- A holder that holds `players` and `gravity` must exist before `:408`. `MegaPlayers()` builds a `PlayerInputs` and
  nothing else; `WorldGravity(players)` stores the reference. No side effect moves.
- No code reads `game.players` or `game.gravity` during `:408-415` today: the fields were `lateinit` and unset, so a
  read would throw. So building them earlier changes nothing for that code.
- `create()` (`MMG:312`) builds the engine with `megaContactRules(this, ...)` and `MegaPathfinderFactory(this)`
  (`016d`). They read `players`, `gravity` and the level values late, through the game. `016c` changes only the
  `016a` forward calls inside them (`getWorldContainer()`, `getTiledMapLoadResult()`).

## 6. `IMegaGameEntity` cleanup (compile probe)

Probe on the base (reverted): remove `val game` and the 3 defaults from `IMegaGameEntity`; add the 3 defaults to
`MegaGameEntity` with the same bodies; `MegaGameEntity(val game: ...)` with no `override`; the extension receivers
`IMegaGameEntity.players` and `IMegaGameEntity.targetPlayer` become `MegaGameEntity`; `IBodyEntity.overlapsGameCamera()`
becomes `fun <T> T.overlapsGameCamera() where T : MegaGameEntity, T : IBodyEntity` with no cast. Result: 2 compile
errors, `FreezableEntityHandler.kt:53` and `LightSourceUtils.kt:56`. With
`class FreezableEntityHandler<T>(private val entity: T, ...) where T : MegaGameEntity, T : IFreezableEntity` and
`fun <T> sendLightSourceEvent(lightSource: T) where T : MegaGameEntity, T : ILightSource` plus
`sendLightSourceEvent(entity: MegaGameEntity, ...)`, `core` main and test compile. No call site changes (61
`FreezableEntityHandler(` calls, 14 `sendLightSourceEvent(` calls, 92 `overlapsGameCamera()` calls, about 470
`targetPlayer`, about 60 `players`).

Users of the moved defaults: `playSoundNow` 40, `stopSoundNow` 2, bare `getGameCamera()` 4 (plus 2 through a cast).

## 7. Constructor probe

A scratch test walked the compiled `com.megaman.maverick.game.entities` package on the test class path and called the
primary constructor of every concrete `MegaGameEntity` class with a relaxed game mock (engine stubbed with a relaxed
mock). Result on the base: 436 classes of `C/entities/` built with no exception. Exceptions: `PushableBlock.InnerBlock`
(2 parameters; private; `PushableBlock.kt:123` builds it directly; never pooled), and 2 test classes on the same
package path (`GroupsTestEntity`, an anonymous class in `EntityExistenceTest`). So a permanent test that builds every
pooled class is practical; it must read only the main class output and skip `InnerBlock` by name.

## 8. Tests

21 test files name `MegamanMaverickGame`:

- 17 build entities with `mockk<MegamanMaverickGame>(relaxed = true)` and stubs: `engine` (16), `state` (13),
  `getCurrentLevel()` (9), `assMan.get(...)` (5), `players` or `players.targetFor` (6), `clock` (1), `eventsMan` (1):
  `StarknerAliveTest`, `StarknerPausedTest`, `EntityQueriesTest`, `PlayerItemRuleTest`, `MegamanHeartTankTest`,
  `SpawnerReleaseTest`, `DrWilyTest`, `EntityExistenceTest`, `MegaPlayersTest`, `SpiderWebDestroyTest`,
  `EntityGroupsTest`, `MegamanWeaponsHandlerOwnershipTest`, `MegamanBehaviorsTest`, `MapObjectComponentTest`,
  `CartV2ItemTest`, `MegamanStunTargetTest`, `LaserIgnoreTest`.
- 4 use the host and stay: `GameClockTest`, `LevelStateTest` (real `MegamanMaverickGame()`), `MegaLevelScreenEventsTest`
  (screen, decision 8), `ContactRuleSelectionTest` (`megaContactRules`, `016d`).
- `MegaServices` is a final class. MockK mocks final classes, so a relaxed holder mock works as a first step; a real
  holder needs mocks only for `GameEngine`, `AssetManager` and `MegaAudioManager`.

## 9. Blame

`.git-blame-ignore-revs` lists 2 squash commits (`001a`, `063b`). `063b` listed its squash, which also held a small
non-mechanical change. `016c` squashes into the parent; the parent merges into `master` with `--no-ff`, so the squash
SHA stays valid on `master`. The `016c` squash will hold the mechanical swap and also real changes (homes, creation
order, cleanup, tests).

## 10. Overlap

`git worktree list`: the main checkout, the parent `mega-refactor-016-master`, and this branch. `fix/pool-identity-freeset`
exists as a branch but is merged into `master`. No other code branch is active.
