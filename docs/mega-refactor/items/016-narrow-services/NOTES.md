# Notes: 016 Narrow services instead of `game` everywhere

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

No dedicated review yet. The host-contract research in `items/001-maverick-engine/NOTES.md` has the member counts and the split between generic and game-specific members.

## 2026-10-28: new scan (read-only agent, `master` at `c39eb9a55`)

Read-only scan. No file in the repo changed. Short forms: `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`,
`E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`,
`MMG` = `C/MegamanMaverickGame.kt`. Tags: [code] read in the repo, [inferred] reasoned, not checked by a build.

Method: `count_members.py` (in this folder) finds every `game.<member>` read in `core` main, `core` test, `lwjgl3`
and `engine`. It skips `import`, `package` and comment lines, and package strings such as
`com.megaman.maverick.game.x`. The raw rows are in `refs_clean.tsv` (module, top package, member, `file:line`). Other
counts come from `grep` and are given with their pattern where it matters.

### 1. Short answer

- The item text is out of date in its open question and in its list of services. `PlayerView` is gone (`061c`), the
  players service and world gravity exist as plain session fields (`029a`, `029d`), the poller is gone from gameplay
  (`031c`), and groups are an engine system that entities reach through `game.getSystem` (`064a`). The "58 property
  calls" number is still exact.
- Entities do most of the reads: 875 of the 1358 `game.` reads in `core` main (64%) are in `C/entities/`, in 416
  files. Screens and their handlers have 457 (34%). The other 26 are in `drawables`, `world`, `utils`,
  `pathfinding`, `controllers` and `levels`.
- Entities use 12 groups of things: assets, events, audio, the session profile (`GameState`), players, gravity, the
  game camera and its flags, level data, the clock (`Performance`), a scheduler (`updatables` + `runQueue`), the groups
  system, and a few host-only members (`paused`, `selectButtonAction`, `setCurrentScreen`, `getShapes`).
- Recommendation: one typed, session-lived service holder in `core` (working name `MegaServices`) that replaces
  `MegamanMaverickGame` as the entity constructor parameter. Level data goes into a session-lived `LevelState` inside
  it, because pooled entities outlive a level. No engine `LevelContext` now (no engine caller). Four or five small
  branches; the big one is a type-only change, proved by the compiler.

### 2. What the item text says, against today's code

| Item text (`ITEM.md`) | Today | Status |
|---|---|---|
| Open question: `LevelContext<out P : IPlayers<out PlayerView>>` | `PlayerView` has 0 references in `core` and `engine` (removed by `061c`, squash `a5dd1d99e`). `IPlayers` is `IPlayers<out E : IGameEntity>` (`E/players/IPlayers.kt:6`). `MegaPlayers : Players<MegaGameEntity>` (`C/players/MegaPlayers.kt:14-15`) | Wrong. The sketch does not compile. Drop it |
| `LevelContext` holds players | `game.players` is a plain field with the game session's lifetime (`MMG:218`, `029a` `SCOPE.md:100`), not level data. `game.gravity` (`WorldGravity`, `MMG:219`, `029d`) is the same | Wrong place. Players and gravity are session services |
| "`MegaPlayers` is a plain class with no game dependency, so a test builds a real one" | True (`C/players/MegaPlayers.kt:14`; `T/players/MegaPlayersTest.kt`) | Still true |
| `targetFor` stays a member of `MegaPlayers` | True (`MegaPlayers.kt:30`), with a `TODO` to item `047` | Still true. Targeting moves in `047`, not `016` |
| Services: level, rooms, world container, map load result | Still in the property bag or behind suppliers: `MMG:769-773`, `1000-1034` | Still to do |
| Services: random | `011` is planned, not done. About 184 lines in 58 files call a random function (`grep -E "getRandom\(|MathUtils\.random|Random\(\)|\.random\(\)|getRandomBool"`) | Not `016` work. Add the field when `011` lands |
| Services: texture cache | `028` is planned, not done. Entities read `game.assMan` 392 times; about 199 companion `ObjectMap<String, TextureRegion>` caches and 123 companion `region` fields are static | Not `016` work. `016` keeps `assMan` as the field; `028` replaces it |
| "Typed fields replace the `game` property bag (58 property calls)" | Exactly 58: `isProperty` 23, `putProperty` 21, `getProperty` 8, `getOrDefaultProperty` 3, `removeProperty` 2, `hasProperty` 1 | Still true |
| "Entities never take the host contract (`001b`)" | 456 files in `C/entities/` name `MegamanMaverickGame` | Still true and still to do |
| "Tests pass 2–3 small fakes instead of a relaxed mock of the whole game" | 16 test files use a relaxed `MegamanMaverickGame` mock and stub `engine`, `state`, `getCurrentLevel()`, `assMan.get`, `players` (section 6) | Still true |
| "Depends on: none" | True | Still true |
| "Related: `001`, `011`, `028`, `029`, `049`" | Also related now: `072` (depends on `016`), `047` (targeting), `012` (`getSystem(...).on`), `013` (level flow, `paused`), `031` (`selectButtonAction`, input settings), `064` (`GroupsSystem`), `021` (`MegaGameEntities`), `053`/`057` (events) | Out of date |
| `NOTES.md` points to the counts in `001` `NOTES.md` (`9f3917b6a`) | Several counts changed: the class has 1079 lines (was 1017); `game.megaman` (23) and the `megaman` extension are gone (`029f`); `controllerPoller` 54 → 9 (`031c`, menus only); `state` 130 → 143; `eventsMan` 139 → 137; `assMan` 482 → 476; `isCameraRotating` 42 → 40 | Out of date. Use the tables below |

What other items already did that `016` once had to do:

- `029a`-`029f`: no global player. `game.megaman` and the `megaman` extension are gone. Code reaches a player through
  `game.players`.
- `029d`: world gravity is a typed field (`game.gravity`), not a property.
- `029e`: lives, tanks, enhancements and owned weapons live in `GameState` (`game.state`), the one session profile.
- `031a`-`031c`: gameplay reads input frames from the entity (`ControllerComponent`), not from `game.controllerPoller`.
  The 9 remaining poller reads are in menus and `ScreenController` (`FrameControllerPoller`, `MMG:301`).
- `061c`: player questions are entity queries (`C/entities/EntityQueries.kt`), not wrappers on a view.
- `062`: existence state is on the engine entity, not read through `game`.
- `064a`: groups are an engine `GroupsSystem`. Entities reach it through `game.getSystem(GroupsSystem::class)`
  (`C/entities/utils/GameEntityUtils.kt:81`). This is a new read of the host that `016` must handle.
- `064b`: the map id is a component (`MapObjectComponent`); `RANDOM_ID_POOL` is gone.

### 3. What `MegamanMaverickGame` exposes today [code]

`MMG` has 1079 lines. The declaration is `class MegamanMaverickGame(val params) : Game(), IEventListener,
IPropertizable` (`MMG:148-150`). It has 72 non-private members at class level and 24 private ones.

Services (an object that does work for others):

| Member | Line | Type | Created | Lifetime |
|---|---|---|---|---|
| `engine` | `:215` | `GameEngine` | `create()`, `:305` | session |
| `eventsMan` | `:212` | `EventsManager` | `create()`, `:299` | session |
| `assMan` | `:211` | `AssetManager` | `create()`, `:357` | session |
| `audioMan` | `:213` | `MegaAudioManager` | `postCreate()`, `:397` | session, after the asset load |
| `state` | `:216` | `GameState` | `create()`, `:306` | session |
| `players` | `:218` | `MegaPlayers` | `postCreate()`, `:413` (after `Megaman` is built at `:406`) | session |
| `gravity` | `:219` | `WorldGravity` | `postCreate()`, `:417` | session |
| `getGameCamera()` | `:972` | `RotatableCamera` from `viewports[GAME]` | `create()`, `:313` | session. The viewport object changes on `TOGGLE_PIXEL_PERFECT` (`:664-697`), the camera object does not |
| `getUiCamera()`, `viewports` | `:978`, `:191` | `OrthographicCamera`, `ObjectMap<String, Viewport>` | `create()` | session |
| `runQueue` | `:199` | `Queue<() -> Unit>`, drained LIFO (`:474-477`) | field | session |
| `updatables`, `disposables` | `:196-197` | `OrderedMap` | field | session |
| `getSystem(KClass)`, `getSystems()` | `:991-998` | systems by simple name, in the property bag | `createGameEngine()` | session |
| `batch`, `shapeRenderer` | `:201-202` | LibGDX | `create()` | session |
| `buttons`, `controllerPoller`, `gamepadInput` | `:204-206` | input storage and the menu facade | `create()` | session |
| `diagnostics` | `:246` | `RuntimeDiagnostics?` | `create()` | session |
| screen host: `screens`, `currentScreen`, `setCurrentScreen()`, `startLevel()`, `startExtraLevel()` | `:192-194`, `:944-970`, `:805` | | | session |
| save: `saveState()`, `hasSavedState()`, `loadSavedState()` | `:815-854` | `Gdx.app` preferences | | |

Global state (values that many readers share, mostly untyped):

| Member | Line | Kept where | Scope |
|---|---|---|---|
| `paused` | `:221` | field | session |
| `selectButtonAction` | `:209` | field | session (per slot after `031e`) |
| `params` (performance, flags) | `:149` | constructor | session |
| `getPerformance()`, `setPerformance()`, `isPerformanceSubpar()` | `:1064-1076` | `params.performance`, changed at run time by auto-perf (`:509-536`) | session, changes |
| `isPixelPerfect()`, `isVsync()`, `setVsync()` | `:1054-1062` | fields | session |
| `setCurrentLevel()`, `getCurrentLevel()`, `isInExtraLevel()` | `:769-774` | property bag, key `level_def` | level |
| `setCameraRotating()`, `isCameraRotating()` | `:974-976` | property bag, key `cam_rotation` | level |
| `setFocusSnappedAway()`, `isFocusSnappedAway()` | `:1050-1052` | property bag, key `focus_snapped_away` | level |
| `setRoomsSupplier()`, `getRooms()`, `setCurrentRoomSupplier()`, `getCurrentRoom()` | `:1000-1025` | property bag, suppliers set by the level screen | level |
| `setWorldContainer()`, `getWorldContainer()` | `:1027-1029` | property bag | level |
| `setTiledMapLoadResult()`, `getTiledMapLoadResult()` | `:1031-1034` | property bag | level |
| `getDrawables()`, `addDrawable()`, `getShapes()` | `:980-989` | property bag, render collectors | session |
| `properties` (`IPropertizable`) and the 58 external calls | `:189` | `Properties` | mixed: level (room transition, player beam, coins, points, floating points) and session (`WEAPONS_ATTAINED`) |
| `consumeLaunchRoom()` | `:760` | fields | launch |
| `resetPlayerProgress()`, `applyLevelOverride()` | `:780-803` | writes `state`, `players`, the bag | session |
| `showNotification()`, `setDebugText()`, `setDebugTextSupplier()` | `:1036-1045` | fields | session |

Property-bag keys that code outside `MMG` uses (58 calls) [code]:

| Key | Calls | Writers | Readers | Scope |
|---|---|---|---|---|
| `ROOM_TRANSITION` | 27 | `MegaLevelScreen.kt:319,341,393,603,1130` | 22 `isProperty` reads: 9 bosses, `Saw` (3), `PushableBlock` (2), `AsteroidsSpawner`, `InfernoMeteorShower`, `PipePuff`, `RisingLavaRiver`, `DarknessV2`, `DarknessV4`, `ClimbBehavior.kt:160`, `ControllerComponent.kt:76`, `MegamanAnimationKeySupplier.kt:28`, `SpawnersLayerBuilder.kt:34` | level |
| `Megaman_beam`, `Megaman_beam_center` | 10 | `PlayerSpawnEventHandler.kt:134,143,218`, `EndLevelEventHandler.kt:120,126,227` | `DarknessV2.kt:372,375`, `DarknessV4.kt:556,558` | level |
| `COINS` | 6 | `Coin.kt:79`, `QuestionBlock.kt:193`, `MegaLevelScreen.kt:512` | `Coin.kt:78`, `QuestionBlock.kt:192`, HUD `MegaLevelScreen.kt:422` | level |
| `LEVEL_POINTS` | 4 | `AbstractEnemy.kt:329`, `MegaLevelScreen.kt:513` | `AbstractEnemy.kt:328`, HUD `MegaLevelScreen.kt:415` | level |
| `TIME_SINCE_LAST_FLOATING_POINTS`, `LAST_FLOATING_POINTS_TYPE` | 7 | `AbstractEnemy.kt:323-324`, `MegaLevelScreen.kt:514,991` | `AbstractEnemy.kt:312,316`, `MegaLevelScreen.kt:990` | level |
| `WEAPONS_ATTAINED` | 4 (+1 in `MMG:786`) | `Megaman.kt:921` | `RobotMasterLevelOnCompletion.kt:10`, `GetWeaponScreen.kt:146,153`, `MainMenuScreen.kt:161` | session (crosses from the level to the get-weapon screen) |

### 4. Reads through `game.` [code]

Totals, `core` main: 1358 reads. `C/entities/` 875 (in 416 files); `C/screens/` 457 (in 32 files; 85 in
`MegaLevelScreen.kt`); `C/drawables/` 11; `C/utils/` 4; `C/world/` 4; `C/controllers/` 3; `C/pathfinding/` 3;
`C/levels/` 1. `core` test: 42 (all stubs and setup). `lwjgl3`: 7 (`paused`, `pause()`, `resume()`, `dispose()` in
`DesktopLauncher.kt`). `engine`: 0.

Per member, `core` main (entities = `C/entities/`; other = everything else):

| Member | All | Files | Entities | Other | Kind |
|---|---|---|---|---|---|
| `assMan` | 476 | 408 | 392 | 84 (screens 73, drawables 10, controllers 1) | service |
| `state` | 143 | 57 | 112 | 31 | service (profile) |
| `eventsMan` | 137 | 65 | 118 | 19 | service |
| `audioMan` | 92 | 38 | 16 | 76 | service |
| `getGameCamera()` | 73 | 56 | 71 | 2 | service |
| `isCameraRotating()` | 40 | 34 | 39 | 1 | level flag |
| `getUiCamera()` | 36 | 21 | 0 | 36 | service (UI) |
| `setCurrentScreen()` | 32 | 18 | 1 (`DrWily.kt:152`) | 31 | host |
| `gravity` | 29 | 20 | 26 | 3 (`MegaLevelScreen.kt:382-383`, `MegaContactRules.kt:56`) | service |
| `batch` | 24 | 13 | 0 | 24 | host (render) |
| `getCurrentLevel()` | 24 | 16 | 16 | 8 | level |
| property bag (6 functions) | 58 | 26 | 35 | 23 | global state |
| `paused` | 14 | 8 | 1 (`Starkner.kt:414`) | 13 | host |
| `players` | 13 | 9 | 2 (`IMegaGameEntity.kt:23,28`) | 11 | service |
| `getCurrentRoom()` | 13 | 10 | 12 | 1 | level |
| `selectButtonAction` | 13 | 4 | 3 (`ControllerComponent.kt:100`, `AirDashBehavior.kt:33,38`) | 10 | setting |
| `getPerformance()` | 11 | 9 | 8 (`.fixedStep` 6, `.fps` 1, plus `BigAssMaverickRobotHand`) | 3 | clock |
| `setFocusSnappedAway()` | 11 | 4 | 7 | 4 | level flag |
| `startLevel()` | 10 | 8 | 0 | 10 | host |
| `controllerPoller` | 9 | 8 | 0 | 9 | host (menus) |
| `updatables` | 9 | 4 | 7 (`ToxicWater` 3, `GameEntityUtils` 3, `MegamanWeaponsHandler` 1) | 2 (`MegaUtilMethods`) | scheduler |
| `viewports` | 9 | 7 | 0 | 9 | host |
| `runQueue` | 7 | 4 | 1 (`GameEntityUtils.kt:182`) | 6 | scheduler |
| `engine` | 7 | 5 | 2 (`MegaGameEntity.kt:16`, `WilyPlaneBomb.kt:143`) | 5 | service |
| `setCurrentLevel()` | 6 | 5 | 0 | 6 | host |
| `getSystem()` | 5 | 2 | 1 (`GameEntityUtils.kt:81`, `GroupsSystem`) | 4 (`MegaLevelScreen`, switch systems on and off) | systems |
| `resume()`, `pause()` | 7 | 3 | 0 | 7 | host |
| `isInExtraLevel()` | 4 | 2 | 0 | 4 | level |
| `getTiledMapLoadResult()` | 3 | 2 | 2 (`Megaman.kt:739-740`) | 1 (`MegaPathfinderFactory.kt:42`) | level |
| `getShapes()` | 3 | 3 | 2 (`Bat.kt:405`, `SubmarineJoe.kt:281`, debug only) | 1 | debug |
| `isPixelPerfect()`, `isVsync()`, `setVsync()`, `params`, `setPerformance()`, `resetAutoPerfGracePeriod()` | 14 | | 0 | 14 | settings |
| `getWorldContainer()` | 2 | 1 | 0 | 2 (`MegaPathfinderFactory.kt:26,28`) | level |
| `getRooms()` | 1 | 1 | 1 (`SwitchGate.kt:100`) | 0 | level |
| 18 other members (`addDrawable`, `saveState`, `consumeLaunchRoom`, the level setters, ...) | 26 | | 0 | 26 | host |

What entities do with the big services:

- `assMan`: `getTextureAtlas` 285, `getTextureRegion` 171, `get` 2 (whole module). Almost all in `init()` or in
  companion-cache fills.
- `state` in entities: `getDifficultyMode` 45, `hardMode` 39, `containsEnhancement` 8, `containsHeartTank` 3,
  `isLevelDefeated` 2, `addLife` 2, 1 each for currency, weapon, health tank. So 84 of 112 are difficulty reads.
- `eventsMan`: `addListener` 44, `removeListener` 48, `submitEvent` 42 (whole module).
- `audioMan` in entities: `playSound` 8, `isSoundPlaying` 3, `playMusic` 3, `stopSound` 1, `getCurrentMusicAsset` 1.
- `gravity`: `direction` 22, `set` 9.
- Game camera: `getRotatedBounds` 37, `overlaps` 9, `toGameRectangle` 1, `direction` 1.
- Level identity checks (`getCurrentLevel() == MOON_MAN`, `type == MARIO_LEVEL`, ...): 16 in entities. These are
  `025` (level manifest) material; `016` only moves the read.

Reads that go through the `IMegaGameEntity` members and extensions, not through `game.` (072 counted these at
`97771d127`; this scan agrees):

| Member | Uses (lines, `core` main) | Reads `game` inside |
|---|---|---|
| `targetPlayer` (`IMegaGameEntity.kt:27-28`) | about 470 in 147 files | `game.players.targetFor(this)` |
| `overlapsGameCamera()` (`:30-31`) | 92 in 78 files | `(this as MegaGameEntity).game.getGameCamera()` |
| `players` extension (`:22-23`) | about 61 in 55 files | `game.players` |
| `playSoundNow` / `stopSoundNow` (`:17`, `:19`) | 40 + 2 | `game.audioMan` |
| bare `getGameCamera()` (`:15`) | 4 bare + 2 through a cast (`GameEntityUtils.kt:58,64`) | `game.getGameCamera()` |

Reads of `game` through another reference (not `this`): `FreezableEntityHandler.kt:53` (`ShieldEntity(entity.game)`,
`entity: IFreezableEntity`), `LightSourceUtils.kt:56` (`entity.game.eventsMan`, `entity: IMegaGameEntity`),
`IMegaGameEntity.kt:31` (after a cast), `GameEntityUtils.kt:51` (`entity.game.eventsMan` after an `is` check),
`GameEntityUtils.kt:80` (cast), `AnimatedBlock.kt:119`, `AbstractEnemy.kt:104`, `MegamanJetpackHandler.kt:32`,
`MegamanDamageNegotiator.kt:24`, `MegamanWeaponsHandler.kt:143`, and the background builders
(`BackgroundLayerBuilder.kt:34-234`, `params.game`).

### 5. How code reaches `game` [code]

| Path | Count | Notes |
|---|---|---|
| Constructor parameter `game: MegamanMaverickGame` | 517 lines in 506 files of `core` main; 456 files in `C/entities/` | By package: enemies 127, projectiles 71, hazards 48, decorations 45, special 38, blocks 37, bosses 28, explosions 23, backgrounds 13, screens/other 12, factories 11, levels 10, menus 9, items 9, sensors 6, contracts 6, megaman 5 |
| `class X(game: MegamanMaverickGame) : Y(game...)` | 445 one-line headers | The super call passes it on: `MegaGameEntity(game)` (167 direct), `AbstractEnemy(game)`, `AbstractBoss(game)`, `AbstractProjectile(game)`, ... |
| `MegaGameEntity(override val game: MegamanMaverickGame) : GameEntity(game.engine), IMegaGameEntity` | `MegaGameEntity.kt:16` | The only place that keeps it as a property. `canSpawn` reads `game.getCurrentLevel()` and `game.state` (`:33-36`), so every spawn reads the host |
| `IMegaGameEntity.game` | `IMegaGameEntity.kt:13` | Through the interface type: 3 reads (section 4) |
| `MegaEntityFactory` (Kotlin `object`) | `C/entities/MegaEntityFactory.kt:22-34,67-72` | `init(game)` stores it in a `lateinit` field; each pool supplier calls `primaryConstructor!!.call(game)`. So every pooled class has a one-parameter primary constructor. 307 `MegaEntityFactory.fetch` lines in 181 files |
| `EntityFactories` (deprecated `object`) | `C/entities/factories/EntityFactories.kt:12-24` | `initialize(game)` builds 9 factories with `game` (`MMG:404`). 73 `EntityFactories.fetch` lines in 50 files still use it |
| Direct construction outside a factory | `Megaman(this)` (`MMG:406`), `ShieldEntity(entity.game)` (`FreezableEntityHandler.kt:53`), screens `X(this)` (`MMG:419-436`), `MegaLevelScreen` builders | |
| Engine assembly with `this` | `megaContactRules(this, ...)` (`MMG:888`; reads `game.players` and `game.gravity` lazily, `MegaContactRules.kt:40,56`), `MegaPathfinderFactory(this)` (`MMG:906-907`) | Built in `create()`, before `players` exists, so these must read late or the order must change |
| Functions with a `game` parameter | `MegaUtilMethods.delayRun(game, ...)` (`C/utils/MegaUtilMethods.kt:15`), `delayNextPossibleSpawn`/`isNextPossibleSpawnDelayed` (`GameEntityUtils.kt:179-193`), `RobotMasterLevelOnCompletion` (`(MegamanMaverickGame) -> ScreenEnum`) | |
| Launcher | `DesktopLauncher.kt:160` | |

Static and singleton access that does not go through `game` (global state) [code]:

| Global | Uses (`core` main) | What it holds | Owner item |
|---|---|---|---|
| `MegaEntityFactory` | 307 `fetch` lines, 181 files | pools per class and the `game` | `045`, `016` (holds the services, not the game) |
| `EntityFactories` | 73 `fetch` lines, 50 files | deprecated factories with the `game` | (migration to `MegaEntityFactory`, no item) |
| `MegaGameEntities` | 64 lines, 44 files | registry by map id, tag and type; written in `MegaGameEntity.onSpawn`/`onDestroy` | `021` |
| `GameObjectPools` | 362 lines, 180 files | scratch object pools | `045` |
| `GameLogger` (engine `object`) | 1947 lines, 399 files | log receivers, tag filter; `MegamanMaverickGame.TAGS_TO_LOG` (`MMG:176`) | `044` |
| Companion texture caches | about 199 `ObjectMap<String, TextureRegion>` + 123 `region` fields | regions loaded once from `game.assMan` | `028` |
| `Gdx.*` reads | 36 lines in 17 files (for example `GameEntityUtils.kt:175` `Gdx.graphics.deltaTime`) | platform | `032` |
| Other stateful objects | `AnimatedBlockAnimators` (reads `block.game.assMan`, `AnimatedBlock.kt:114-119`), `LevelCompletionMap`, `ControllerUtils` | | |

`016` needs to change only the first two rows (they hold the `game`). The others do not reach `game`; they are other
items' work.

### 6. Tests [code]

- 39 test files in `T/`; 16 name `MegamanMaverickGame`.
- All 16 build `mockk<MegamanMaverickGame>(relaxed = true)` (or take one), and 15 stub members by hand:
  - `engine`: 15 stubs (every test that builds a `MegaGameEntity`, because `MegaGameEntity.kt:16` reads
    `game.engine`).
  - `state` (`GameState()`): 10 tests; `getCurrentLevel()` (`TEST_1`): 8 tests, because `MegaGameEntity.canSpawn`
    reads both (`:33-36`).
  - `assMan.get(...)` returns an atlas: 4 (`LaserIgnoreTest`, `CartV2ItemTest`, `SpiderWebDestroyTest`,
    `StarknerAliveTest`).
  - `players` or `players.targetFor`: 4.
- `ContactRuleSelectionTest.kt:54` passes a relaxed game to `megaContactRules`.
- 4 tests already avoid the game: they extend the engine `GameEntity(mockk(relaxed = true))` (`PlayPhasesTest`,
  `TeleportsTest`, `MegaLocksTest`, `EntityQueriesTest`).
- Risk in the relaxed mock: any member that a test does not stub returns a mock or a default without a failure, so the
  tests do not show which members an entity needs.

### 7. Constraints that the design must respect [code] / [inferred]

1. Pooled entities outlive a level. `MegaEntityFactory` pools live for the session (`object`), and `postCreate`
   preloads 25 `Bullet` and 25 `Block` (`MegaEntityFactory.kt:30-45`). So anything that an entity gets at
   construction must be session-lived. A per-level object injected at construction would be stale in the next level.
   Level data must be a session-lived holder whose values change at level start (as the property bag does today), or
   be read through it.
2. Values that change at run time must be read when used, not copied: `getPerformance()` (auto-perf, `MMG:509-536`),
   `paused`, current level, current room, room transition, camera rotation, `selectButtonAction`.
3. Creation order in `postCreate` (`MMG:394-417`): `audioMan` (`:397`) → `MegaEntityFactory.init` (`:401`, which
   builds and `init()`s 50 entities) → `EntityFactories.initialize` (`:404`) → `Megaman(this)` (`:406`) →
   `players = MegaPlayers()` (`:413`) → `register` (`:416`) → `gravity` (`:417`). A services object that holds
   `players` and `gravity` must exist before `:401`, so `MegaPlayers()` and `WorldGravity(players)` must be built
   earlier. `MegaPlayers()` has no side effect at construction [inferred from `MegaPlayers.kt:14-15`; check
   `PlayerInputs`]. Registration stays after `Megaman` is built.
4. `megaContactRules` and `MegaPathfinderFactory` are built in `create()` (`MMG:305`, inside `createGameEngine`),
   before `postCreate`. If they take services in place of `game`, they must read them late (a supplier) or the engine
   assembly must move after the services exist.
5. The game camera object is stable for the session (`MMG:313`; `TOGGLE_PIXEL_PERFECT` makes new viewports with the
   same camera, `:672-692`). So a direct `RotatableCamera` reference is safe.
6. `MegaEntityFactory` calls the primary constructor with one argument through reflection. If the argument type
   changes, every pooled class must change its constructor type in the same commit. A wrong type fails only at run
   time, at the first `fetch` of that class (an `IllegalArgumentException` from `call`). A test that calls
   `primaryConstructor` for every class in `EntityType` packages would catch it before a play test (this is close to
   `004`, the entity smoke test).
7. Screens are the host's UI. `001` says that entities and systems must not take the host contract; it does not say
   this of screens. Screens can keep the host.

### 8. Proposed service split

#### 8.1 Services and their users

| Service (working name) | Holds today | Entity uses | Other uses | Lifetime | Notes |
|---|---|---|---|---|---|
| `engine` | `GameEngine` | base constructor + 1 (`WilyPlaneBomb.kt:143`) | 5 | session | needed by `GameEntity(engine)` |
| `events` | `EventsManager` | 118 + `LightSourceUtils`, `GameEntityUtils` | 19 | session | `053`/`057` later |
| `assets` | `AssetManager` | 392 | 84 | session | `028` replaces it with a texture cache |
| `audio` | `MegaAudioManager` | 16 + 42 (`playSoundNow`/`stopSoundNow`) | 76 | session (after asset load) | |
| `profile` | `GameState` | 112 (84 difficulty) | 31 | session | a `Difficulty` view is possible later; no caller needs it alone now |
| `players` | `MegaPlayers` | 2 + ~61 extension + ~470 `targetPlayer` | 11 + contact rules | session | targeting moves in `047` |
| `gravity` | `WorldGravity` | 26 | 3 | session | |
| `camera` | `RotatableCamera` + rotating flag + focus-snapped flag | 71 + 4 + 92 + 2; rotating 39; focus 7 | 7 | session (flags are level values) | today the flags are in the bag; they become typed fields |
| `level` (`LevelState`) | current level, rooms supplier, current-room supplier, map load result, world container, room transition, player beam, coins, points, floating points | 16 + 12 + 1 + 2 + 35 bag calls | 23 bag calls + level screen setters | session holder, values per level | constraint 1 |
| `clock` | `params.performance` (`fixedStep`, `fps`) | 8 | 3 | session, changes | `010` (fixed tick) owns the real clock later; `016` gives only a read |
| `scheduler` | `updatables` + `runQueue` | 8 | 2 (`MegaUtilMethods`) + screens | session | the `post(runnable)` of `001b` |
| `groups` | `GroupsSystem` | 1 (`GameEntityUtils.kt:81`) | 0 | session | a direct reference, not `getSystem` |

Host-only reads in entities, each needs its own decision before the type swap (7 sites):

| Site | Read | Proposed home |
|---|---|---|
| `DrWily.kt:152` | `game.setCurrentScreen(ENDING_SCREEN)` | an event (`EventType`) that the level screen handles, or a `levelFlow` callback in `LevelState`; the frame timing must stay the same (screen switch inside the update today) |
| `Starkner.kt:414` | `game.paused` | a read-only `paused` on `clock` or `LevelState` (`013` owns pause later) |
| `ControllerComponent.kt:100`, `AirDashBehavior.kt:33,38` | `game.selectButtonAction` | a per-slot input setting next to `players.inputs` (`031e` plans settings per slot) |
| `Bat.kt:405`, `SubmarineJoe.kt:281` | `game.getShapes()` (debug, behind `DEBUG_PATHFINDING`) | a `debugShapes` collector field, or delete the debug path (owner choice) |
| `Megaman.kt:921` | `game.putProperty(WEAPONS_ATTAINED, ...)` | a typed session field (not saved), next to `GameState` or on the services |

#### 8.2 How an entity gets the services: options

(a) One typed holder, passed in place of the game (recommended).

```kotlin
// core, built once in postCreate before MegaEntityFactory.init
class MegaServices(
    val engine: GameEngine,
    val eventsMan: EventsManager,
    val assMan: AssetManager,
    val audioMan: MegaAudioManager,
    val state: GameState,
    val players: MegaPlayers,
    val gravity: WorldGravity,
    val camera: GameCameraState,   // camera + rotating + focusSnappedAway
    val level: LevelState,         // session-lived, values set by the level screen
    val clock: ...,                // read-through of params.performance
    val scheduler: ...,            // updatables + runQueue
    val groups: GroupsSystem,
)
abstract class MegaGameEntity(val game: MegaServices) : GameEntity(game.engine), IMegaGameEntity
```

- The parameter keeps the name `game` in the swap branch, and the holder keeps the member names that entities call
  today (`assMan`, `eventsMan`, `state`, ...), or forwards (`getGameCamera()`, `getCurrentLevel()`,
  `isCameraRotating()`). Then about 875 entity reads compile with no edit; only the 445+ constructor headers change
  their type (a mechanical edit). A later mechanical commit can rename `game` → `services` and `assMan` → `assets`
  (in `.git-blame-ignore-revs`).
- Pros: smallest diff; the compiler proves the swap; one reflection argument, so `MegaEntityFactory` and the pools do
  not change shape; tests build a real holder from plain classes (`GameState()`, `MegaPlayers()`, `EventsManager()`,
  `WorldGravity`) and 2-3 mocks (`GameEngine`, `AssetManager`, `MegaAudioManager`); the holder has no screen, save
  or launch functions.
- Cons: it is still one object that gives every entity every service. It can grow into a new god object. Rule against
  that: a field only with a caller; no host function (screen switching, save, launch).
- Size: about 460 files with a type-only change in the swap branch; a few dozen hand edits for the forwards and the 7
  host-only sites.

(b) Constructor injection of each service.

- Each entity lists what it uses (`class Met(assets, events, audio, players, ...)`). `MegaEntityFactory` must resolve
  parameters by type (a small DI container over `KParameter`). Base classes (`AbstractEnemy` uses events, assets,
  audio, state, level, ...) force every subclass to pass all of them.
- Pros: each class shows its needs; tests pass only what the class uses.
- Cons: hand edits in about 460 files with no gameplay gain; reflection by type is slower and fails at run time; the
  rule "write code only when production code calls it" argues against a container.
- Not recommended now. It can follow per class later if `048` needs it.

(c) Services as components (`AudioServiceComponent(audio)` on each entity).

- Cons: a component per entity that holds a session object is the same god object, per entity; a map lookup per read
  on hot paths (camera overlap, gravity direction); components are per-entity data, services are not.
- Not recommended.

(d) Static access (`Services.audio`, a Kotlin `object` or a `companion`).

- Cons: global state, which the mega refactor removes (`029` removed `game.megaman`); tests must reset globals;
  co-op split screen (two cameras) breaks a global camera.
- Not recommended.

(e) Move the work into systems (for example camera overlap as a system query, sounds as `AudioComponent` requests).

- This is the `017`/`048` direction. It removes reads, not the way to reach a service. Not `016` work.

Recommendation: (a), with these rules: session-lived fields only; level values in a session-lived `LevelState`;
values that change are read through, not copied; no field without a caller; screens and the launcher keep the host.

#### 8.3 Factory and pooling

- `MegaEntityFactory.init(game)` → `init(services)`; the pool supplier calls `call(services)`. `EntityFactories`
  and its 9 factories take `services` in the same commit (type only).
- No change to pool keys, preload amounts or the pool runnable (`GameEntityPoolCreator.kt:13-16`).
- `onSpawnRejected` in `MMG:929-934` stays in the game (it is engine assembly).

#### 8.4 Tests

- One test helper (test sources) that builds a `MegaServices` from real plain objects and a few mocks. The 12 tests
  that mock the game (16 files) move to it. Unstubbed reads then fail loudly in place of a silent relaxed answer, which can show
  hidden dependencies; fix each one in the test, not in production code.
- A test that builds every pooled class by its primary constructor with a test `MegaServices` (catch constraint 6).
  This overlaps `004`; it can be the first real caller of `004`'s idea, or wait for `004`.

### 9. Impact on `072` and on `001b`

`072` (remove `IMegaGameEntity`, depends on `016`):

- After the swap branch, `IMegaGameEntity` must lose `val game` (it would be `MegaServices`, still a game service
  bag on an interface), and with it the 3 default methods (`:15-19`) and the extensions (`:22-31`), because they read
  `game` through the interface.
- The 3 reads through an interface type get a home with no cast:
  - `FreezableEntityHandler.kt:18-19,53`: a generic bound
    `class FreezableEntityHandler<T>(private val entity: T, ...) where T : MegaGameEntity, T : IFreezableEntity`.
    Callers pass `this`, so the 61 call sites do not change (type inference) [inferred; check by compile].
  - `LightSourceUtils.kt:50-56`: take the `EventsManager` or a `MegaGameEntity` receiver; the `ILightSource` overload
    (`:42`) gets the same bound.
  - `overlapsGameCamera()`: receiver `MegaGameEntity` with an `IBodyEntity` bound, or `fun <T> T.overlapsGameCamera()
    where T : MegaGameEntity, T : IBodyEntity`.
- `players` and `targetPlayer` extensions: retype the receiver to `MegaGameEntity` (all receivers are
  `MegaGameEntity`, per `072` `NOTES.md` section 2). `targetPlayer` itself goes in `047`.
- Then `IMegaGameEntity` holds only `ITaggable` and `ITypable<EntityType>`, and `072` is the type-only change that its
  notes describe (about 20 files, about 40 lines).

`001b` (`IMaverickGame`, blocked on `016`):

- After `016`, entities, contact rules, the pathfinder factory and the entity utils no longer name
  `MegamanMaverickGame`. Only screens, screen handlers, layer builders, backgrounds and the launcher do.
- The generic part of `MegaServices` (engine, events, assets, audio, camera, scheduler, clock) is the list of services
  that `IMaverickGame` exposes in `001b` `SCOPE.md`. The game-specific part (`state`, `players`, `gravity`,
  `LevelState`) stays in `core`.
- `016` must not add an engine interface (no engine caller yet). `001b` decides which of these services the engine
  host owns.
- The `001b` `SCOPE.md` "Current state" still names `megaman`; that text is out of date (`029f`).

### 10. Proposed branch split

Each branch is small, has one kind of change, and has its own proof that the game plays the same.

| Branch | Content | Proof that it plays the same |
|---|---|---|
| `016a` `typed-level-state` | Replace the 58 external property-bag calls and the internal bag keys (`MMG:769-1034`) with typed fields: a `LevelState` (current level, room and rooms suppliers, map load result, world container, room transition, player beam and center, coins, points, floating points) and camera flags (rotating, focus snapped away), plus a typed `weaponsAttained`. `MegamanMaverickGame` stops being `IPropertizable` if nothing else uses it. Callers keep calling `game.` (the fields are on the game for now, as `players` and `gravity` are) | Compile; grep: 0 `game.(is|put|get|getOrDefault|remove|has)Property(`; same defaults as the bag (`Boolean` false when unset, `Int` from the level start at `MegaLevelScreen.kt:512-514`); play: room transitions with a boss and `Saw`, the beam with `DarknessV2`/`V4`, coins and points in a Mario level, the get-weapon screen after a robot master, camera rotation with `GravitySwitcharoo` |
| `016b` `entity-host-reads` | Give the 7 host-only entity reads a typed home (section 8.1): `DrWily` screen switch, `Starkner` pause, `selectButtonAction`, debug shapes, `WEAPONS_ATTAINED` (if not in `016a`), `GroupsSystem` as a direct field, the clock read, the scheduler for `updatables`/`runQueue` | Compile; play: the Wily ending, `Starkner` during pause, air dash with each select action, a `GROUP` near-camera spawn |
| `016c` `entity-services` | Add `MegaServices`, built in `postCreate` before `MegaEntityFactory.init` (move `MegaPlayers()` and `WorldGravity` up). Change the type of the entity constructor parameter in about 460 files (type only, name `game` kept). `MegaEntityFactory`, `EntityFactories`, `Megaman`, `ShieldEntity` get the services. `IMegaGameEntity` loses `game`, the 3 defaults (to `MegaGameEntity`) and the extensions (retyped receivers). Tests move to the helper | The compiler (the holder forwards to the same objects); a test that builds every pooled class through its primary constructor; grep: 0 `MegamanMaverickGame` in `C/entities/` except allowed sites; a short play pass of 2-3 stages. Do it alone, with no other active code branch (like `001a`) |
| `016d` `system-services` | Non-entity code takes services: `megaContactRules` (players, gravity), `MegaPathfinderFactory` (level), `MegaUtilMethods.delayRun` and the spawn-delay functions (scheduler), `GameEntityUtils`, `AnimatedBlockAnimators`, drawables and backgrounds if they are entity-like | Compile; `ContactRuleSelectionTest` with a services object; play: sand, gravity change, pathfinding enemies (`Bat`, `SubmarineJoe`) |
| `016e` `service-names` (optional) | Mechanical rename: `game` → `services` in entity code, `assMan` → `assets`, `eventsMan` → `events`, ... Add the commit to `.git-blame-ignore-revs` | Compile only; no logic line changes (a diff check that ignores the renamed names) |

Order: `016a` → `016b` → `016c` → `016d` → (`016e`). `016a` and `016b` can swap. After `016c`, `072` can start. After
`016d`, `001b` can start (with `010` and `032` as `001` says).

### 11. Risks

- `016c` touches about 460 files. Merge conflicts with any active code branch (`031d`, `031e` are next for `031`;
  `008` and `019` are active). Do it when no other code branch is active, and rebase it at once.
- Reflection fails at run time, not at compile time, if one pooled class keeps the old type (constraint 6). The
  constructor test removes this risk.
- Creation order: building `MegaPlayers` and `WorldGravity` earlier; any side effect at construction would move too.
- Stale values: a service that copies a value that changes (performance, pause, level) would change gameplay. Read
  through.
- Level values in a session holder: a value that the level start does not reset today keeps its old value across
  levels; the typed fields must reset exactly the same keys as today (the bag keeps everything that is not put again).
- Defaults: `isProperty(key, true)` is false when the key is not set; `getOrDefaultProperty(..., 0)` is 0. Typed
  fields must start with the same values.
- New god object: `MegaServices` can grow. The rule "a field only with a caller" and a review of each new field.
- Test churn: moving from relaxed mocks to a real holder can show hidden reads in tests; each needs a stub.

### 12. Open questions for the owner, with recommendations

1. Delivery: one typed holder passed in place of the game (a), or a service per constructor parameter (b)?
   Recommendation: (a). It is a type-only change for most files, the compiler proves it, and the factory keeps one
   argument.
2. Name of the holder: `MegaServices`, `GameContext`, `EntityContext`? Recommendation: `MegaServices` (it holds
   services, not a context of one level; it matches `MegaPlayers` and `MegaAudioManager`).
3. `LevelContext` from the item text: keep the name for the level part? Recommendation: call the level part
   `LevelState` and make it session-lived, with values set at level start. A per-level object does not work with
   pooled entities (constraint 1).
4. Drop the open question `LevelContext<out P : IPlayers<out PlayerView>>` and any engine-side context in `016`?
   Recommendation: yes. `PlayerView` is gone, and no engine code needs a context. `001b` decides what the engine host
   exposes.
5. In `016c`, keep the parameter name `game` and the old member names (`assMan`, `eventsMan`) so that the body of
   each entity does not change, and rename in a separate mechanical branch or commit? Recommendation: yes; the rename
   (`016e`) is optional and can wait.
6. (`072` open question) Do the 3 default methods `getGameCamera`, `playSoundNow`, `stopSoundNow` go in `016` or in
   `072`? Recommendation: in `016` (`016c`). They read `game` through the interface, so they must move when `val game`
   leaves `IMegaGameEntity`. Move them to `MegaGameEntity` as members that read the services; all 50 callers are
   `MegaGameEntity` subclasses or already cast to it (`072` `NOTES.md` section 5a), so no call site changes. `072` then
   only deletes the empty interface.
7. Random (`011`) and texture cache (`028`) in the `016` scope? Recommendation: no. They become fields of the holder
   when those items land.
8. Do screens keep the host (`MegamanMaverickGame`)? Recommendation: yes in `016`. Screens are the host's UI; `001b`
   decides their contract.
9. `DrWily` switches the screen from an entity (`DrWily.kt:152`). Event, or a level-flow callback? Recommendation: a
   callback on `LevelState` that the level screen sets, so the switch happens at the same point in the frame as today.
   An event would run later in the frame (after `screen.render`), which can change one frame of behavior.
10. Debug shapes in `Bat` and `SubmarineJoe` (`getShapes()` behind `DEBUG_PATHFINDING`): keep as a services field, or
    delete the debug path? Recommendation: keep a `debugShapes` field only if the owner uses it; else delete the two
    debug lines in `016b`.
11. `WEAPONS_ATTAINED`: a typed field on `GameState` (not saved) or on the services? Recommendation: a typed field next
    to `GameState` on the host and the services, not in `GameState`, because `GameState` is the saved profile and this
    value is not saved.
12. The constructor test (every pooled class built with a test holder): add it in `016c`, or leave it to `004`?
    Recommendation: add it in `016c` as the proof of the swap; `004` can extend it later.
