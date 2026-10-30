# Scan: `016a` (typed level state in place of the `game` property bag)

Base: `master` at `44c6b5fa3` (the branch start). `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `MMG` =
`C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`, `PSEH` =
`C/screens/levels/events/PlayerSpawnEventHandler.kt`, `ELEH` = `C/screens/levels/events/EndLevelEventHandler.kt`.
Line numbers are on the base.

Method: `grep -rnE "game\.(is|put|get|getOrDefault|remove|has)Property\("` over `core`, `lwjgl3` and `engine` (main
and test) finds the external calls. This pattern also matches `params.game.` and other receivers that end in `game`.
`grep` for `Property(` with any other receiver, and for `properties` in `MMG`, finds the internal uses. A local
compiler probe confirms the list (section 8). No test, `engine` or `lwjgl3` code calls the bag of the game. No code
passes the game as an `IPropertizable`.

## 1. The bag

- `MMG:150`: `class MegamanMaverickGame(...) : Game(), IEventListener, IPropertizable`. `MMG:189`:
  `override val properties = Properties()`. The game object is created once by the launcher, so the bag lives for the
  session. Nothing ever clears it (`clearProperties` has no caller).
- `Properties` (`engine/.../common/objects/Properties.kt`) wraps a LibGDX `ObjectMap<Any, Any?>`. Read rules that the
  typed fields must copy:
  - `isProperty(key, value)` = `map.get(key) == value`. With `true`: `false` when the key is not set.
  - `getProperty(key, T::class)` = the value cast to `T`, or `null` when the key is not set.
  - `getOrDefaultProperty(key, d, T::class)` = `d` when the key is not set.
  - `hasProperty(key)` = `containsKey(key)`. A key put with a `null` value is "set".
  - `removeProperty(key)` removes the key.
- Each key is a different string (section 7), so no two uses share a key.
- 58 external calls in 30 files: `isProperty` 23, `putProperty` 21, `getProperty` 8, `getOrDefaultProperty` 3,
  `removeProperty` 2, `hasProperty` 1. One more line is in a comment (`ReactorMan_OLD.kt:354`); it is not code.
- Internal: 11 more keys, used only inside `MMG` (section 3).

## 2. External keys (the 58 calls)

"Unset default" is what the readers get before the first write. "Set" and "cleared" give the moments.

### 2.1 `ROOM_TRANSITION` (`"room_transition"`), 26 calls

- Type: `Boolean`. Unset default: `false` (all readers use `isProperty(ROOM_TRANSITION, true)`).
- Writers (5, all in `MLS`):
  - `true`: `MLS:319` (`cameraManagerForRooms.beginTransition`, after the enemies and projectiles are destroyed and
    `BehaviorsSystem` is off); `MLS:341` (`continueTransition`, each update of the transition).
  - `false`: `MLS:393` (`endTransition`, after `BehaviorsSystem` is on again); `MLS:603` (`PLAYER_SPAWN_INIT` event);
    `MLS:1130` (`MLS.reset()`, when the level screen is left).
  - Never removed. Not written at `MLS.start`. After the first room transition the key stays set (`true` or `false`)
    for the session.
- Readers (21): `InfernoMan.kt:441`, `GlacierMan.kt:397`, `DesertMan.kt:505`, `ReactorMan.kt:496`,
  `PreciousWoman.kt:663`, `MoonMan.kt:493`, `RodentMan.kt:623`, `TimberWoman.kt:1050` (sprite hidden during a room
  transition); `Saw.kt:133`, `:174`, `:259` (sprites hidden); `PushableBlock.kt:209`, `:215` (no motion);
  `AsteroidsSpawner.kt:130`, `InfernoMeteorShower.kt:145` (no update); `PipePuff.kt:154`, `RisingLavaRiver.kt:310`
  (timer paused); `ControllerComponent.kt:76` (no shot on release); `MegamanAnimationKeySupplier.kt:28` (keep the
  prior animation); `ClimbBehavior.kt:160`; `SpawnersLayerBuilder.kt:34` (the spawn predicate of every spawner that
  is not a block, hazard, special or decoration, read each spawner update).
- Lifetime: session. Kind: level flow (room transition state). Class: `LevelState`.

### 2.2 `Megaman_beam` (`"${Megaman.TAG}_${ConstKeys.BEAM}"`), 6 calls

- Type: `Boolean`. Unset default: `false` (`isProperty(..., true)`).
- Writers:
  - `true`: `PSEH:134` (`PSEH.init`, at each player spawn, also after a death); `ELEH:120` (`ELEH.init`, the level
    end); `ELEH:227` (`ELEH.beamEnd`, at the end of the beam-out, just before `END_LEVEL` is submitted).
  - `false`: `PSEH:218` (`PSEH.beamTrans`, when the beam-in lands).
  - Never removed, not reset at level start. After a level end it stays `true` until the next spawn lands. Note: the
    write at `ELEH:227` puts `true`, not `false` (section 9).
- Readers: `DarknessV2.kt:372`, `DarknessV4.kt:556`.
- Class: `LevelState`.

### 2.3 `Megaman_beam_center` (`"${Megaman.TAG}_${ConstKeys.BEAM}_${ConstKeys.CENTER}"`), 4 calls

- Type: `Vector2?`. Unset default: `null` (`getProperty(..., Vector2::class)`; both readers check for `null`).
- Writers: `PSEH:143` (`PSEH.update`, each frame of the spawn handler) and `ELEH:126` (`ELEH.update`, each frame of
  the end handler). Each puts its **own** `beamCenter` object (a `private val beamCenter = Vector2()` of the
  handler) after it sets the value. So the bag holds a reference to one of two handler vectors, not a copy.
- Readers: `DarknessV2.kt:375` copies the value (`center.set(beamCenter)`). `DarknessV4.kt:558-561` keeps the
  reference (`lightSourceDef.center = beamCenter`) and only reads `x` and `y` later (`:580-581`); no code writes
  through it. The typed field must hold the same reference, not a copy, so that `DarknessV4` sees the same object.
- Never removed, not reset at level start.
- Class: `LevelState`.

### 2.4 `COINS` (`"coin"`), 6 calls

- Type: `Int`. Unset default: `0` for `Coin.kt:78` and `QuestionBlock.kt:192` (`getOrDefaultProperty(..., 0)`);
  `null` for the HUD (`MLS:422`, `getProperty(COINS, Int::class).toString()` gives `"null"`).
- Writers: `MLS:512` (`MLS.start`, `0`, at each level start); `Coin.kt:79` (`+1`, `contactWithPlayer`);
  `QuestionBlock.kt:193` (`+1`, when it spawns a coin).
- The HUD draws only in a `MARIO_LEVEL` (`MLS:1047-1049`), in `MLS.render`, and `MLS.start` always runs before the
  first render of a level (`MMG.startLevel`, `MMG:958-970`). So the HUD never shows `"null"` in play. A typed `Int`
  with default `0` gives the same text in every reachable case.
- Class: `LevelState`.

### 2.5 `LEVEL_POINTS` (`"level_points"`), 4 calls

- Type: `Int`. Unset default: `0` for `AbstractEnemy.kt:328`; `null` for the HUD (`MLS:415`).
- Writers: `MLS:513` (`MLS.start`, `0`); `AbstractEnemy.kt:329` (`spawnFloatingPoints`, `+ points`).
- Same HUD rule as `COINS`. Class: `LevelState`.

### 2.6 `TIME_SINCE_LAST_FLOATING_POINTS` (`"time_since_last_floating_points"`), 5 calls

- Type: `Float`. Unset default: none. Both readers use `getProperty(..., Float::class)!!` (`AbstractEnemy.kt:312`,
  `MLS:990`), so an unset key throws a `NullPointerException`. Neither reader can run before `MLS.start`.
- Writers: `MLS:514` (`MLS.start`, `0f`); `MLS:991` (each `MLS.render` frame that is not paused, `+ delta`, after
  `engine.update`); `AbstractEnemy.kt:324` (`spawnFloatingPoints`, `0f`).
- The typed field: `Float`, default `0f`. The sum `value + delta` is the same `Float` addition.
- Class: `LevelState`.

### 2.7 `LAST_FLOATING_POINTS_TYPE` (`"last_floating_points_type"`), 2 calls

- Type: `FloatingPointsType?`. Unset default: `null` (`AbstractEnemy.kt:316` checks for `null`).
- Writer: `AbstractEnemy.kt:323` only. Never removed and **not** reset at level start. So the type of the last
  floating points of the former level is kept. It has an effect only if a new level starts and an enemy gives
  points within `AGGREGATE_FLOATING_POINTS_DUR` of the level start (the timer is `0f` at start).
- Class: `LevelState`, with no reset (same as today).

### 2.8 `WEAPONS_ATTAINED` (`"weapons_attained"`), 5 external calls (+1 internal)

- Type: `OrderedSet<MegamanWeapon>`. Unset default: "not set" (`hasProperty` is `false`, `getProperty` is `null`).
- Writer: `Megaman.kt:921` (`onAddLevelDefeated`, a `GameState` listener) puts `LevelRewards.weaponsFor(level)`, a
  new set. The set is never `null`; it is empty for a level with no reward. `GameState.addLevelDefeated` calls the
  listener only when the level is new. Callers of `addLevelDefeated`: `MLS:876` (`END_LEVEL`, not for an
  `EXTRA_LEVEL`) and `GameState.fromString` (`GameState.kt:370`, a save load, once per level in the save).
- Removed: `MMG:786` (`resetPlayerProgress`); `GetWeaponScreen.kt:153` (`show`, after the read);
  `MainMenuScreen.kt:161` (after a successful save load).
- Readers: `RobotMasterLevelOnCompletion.kt:10` (`hasProperty`: go to `GET_WEAPONS_SCREEN`, else
  `SAVE_GAME_SCREEN`); `GetWeaponScreen.kt:146` (`getProperty`, then `toGdxArray()`; `null` or empty goes to
  `SAVE_GAME_SCREEN` with an error log).
- A typed `OrderedSet<MegamanWeapon>?` with `null` for "not set" is the same, because no writer puts `null`.
- Lifetime: session. It crosses from the level screen to the get-weapon screen. Owner decision 10 (`ITEM.md`): a
  typed field that is not saved, outside `GameState`. Class: not level data; a session field (see `SCOPE.md`).

## 3. Internal keys (in `MMG` only)

These are behind typed functions today. Their callers do not use the bag directly.

| Key | Type | Unset default | Set | Cleared | Readers | Class |
|---|---|---|---|---|---|---|
| `level_def` (`MMG:769-774`) | `LevelDefinition` | `getCurrentLevel()` throws (`!!`); `isInExtraLevel()` is `false` | `setCurrentLevel`: `MMG.startLevel` (`:959`, `:967`), the launch `--screen` staged level (`:455`), `MainMenuScreen:156`, `LevelSelectScreen:382`, `LevelSelectScreenV2:248`, `SaveGameScreen:91`, `SelectDifficultyScreen:91`, `:111` | never | `getCurrentLevel()` 24 reads (16 in entities, also `MegaGameEntity.canSpawn` at each spawn), `isInExtraLevel()` 4, `consumeLaunchRoom` | `LevelState` |
| `cam_rotation` (`MMG:974-976`) | `Boolean` | `false` | `true`: `MLS:912` (`START_GAME_CAM_ROTATION`); `false`: the game camera callback at the end of a rotation (`MMG:314`), `MLS.start` (`:439`) | never | `isCameraRotating()` 40 reads (39 in entities) | camera flag |
| `focus_snapped_away` (`MMG:1050-1052`) | `Boolean` | `false` | `true`: `Megaman.kt:211`, `ClimbBehavior.kt:142`, `PipePortal.kt:150`; `false`: `MMG:315` (camera callback), `MLS:487` (`start`), `MLS:602` (`PLAYER_SPAWN_INIT`), `MLS:986` (focus reached), `MLS:1133` (`reset`), `Megaman.kt:504`, `:508`, `ClimbBehavior.kt:215`, `PipePortal.kt:128` | never | `isFocusSnappedAway()`: `MLS:279` (room interpolation), `MLS:982` | camera flag |
| `rooms_supplier` (`MMG:1000-1012`) | `() -> Array<RectangleMapObject>?` | `getRooms(out)` returns `out` unchanged | `MLS:440` (`start`) | never | `getRooms`: `SwitchGate.kt:100` | `LevelState` |
| `room_supplier` (`MMG:1015-1025`) | `() -> RectangleMapObject?` | `getCurrentRoom()` is `null` | `MLS:441` (`start`) | never | `getCurrentRoom()` 13 reads | `LevelState` |
| `WORLD_CONTAINER` (`"world_graph_map"`, `MMG:1027-1029`) | `IWorldContainer?` | `null` | `MLS:480` (`start`, a new container for each level) | never | `WorldSystem` supplier (`MMG:886`, each world step); `MegaPathfinderFactory.kt:26`, `:28` (also from the async pathfinding thread when `--pathfinding async`) | `LevelState` |
| `TILED_MAP_LOAD_RESULT` (`MMG:1031-1034`) | `TiledMapLoadResult` | `getTiledMapLoadResult()` throws (`null as TiledMapLoadResult`) | `MLS:444` (`start`) | never | `Megaman.kt:739-740` (out-of-map death check), `MegaPathfinderFactory.kt:42` | `LevelState` |
| `DRAWABLES` (`MMG:874`, `:980-987`) | `ObjectMap<DrawingSection, InsertionOrderPriorityQueue<...>>`, read as `java.util.Queue` | – | `createGameEngine` (once, in `create()`) | never | `getDrawables()`: `MLS:231`, `addDrawable` (`FontsSystem`, `SpritesSystem`, `PSEH:164`, `ELEH:128`) | session render collector |
| `SHAPES` (`MMG:877`, `:989`) | `Array<IDrawableShape>` | – | `createGameEngine` | never | `getShapes()`: `MLS:232`, `Bat.kt:405`, `SubmarineJoe.kt:281` (debug) | session render collector |
| `SYSTEMS` (`MMG:939`, `:991-998`) | `ObjectMap<String, GameSystem>` | – | `createGameEngine` | never | `getSystems()`, `getSystem()`: `MMG:652`, `:661`, `:1070`, `GameEntityUtils.kt:81`, `MLS:318`, `:392`, `:721`, `:734` | session |

`WEAPONS_ATTAINED` is also removed internally at `MMG:786` (section 2.8).

## 4. When values are set and cleared, summary

| Moment | Writes |
|---|---|
| Game construction | nothing (the bag is empty) |
| `create()` | `DRAWABLES`, `SHAPES`, `SYSTEMS` (`createGameEngine`); camera callback registered (no write yet) |
| `setCurrentLevel` (menus, launch) | `level_def` |
| `MLS.start` (each level start) | `cam_rotation=false` (`:439`), rooms and room suppliers (`:440-441`), map load result (`:444`), world container (`:480`), `focus_snapped_away=false` (`:487`), `COINS=0`, `LEVEL_POINTS=0`, `TIME_SINCE_LAST_FLOATING_POINTS=0f` (`:512-514`, the last lines of `start`) |
| `PLAYER_SPAWN_INIT` (each spawn) | `focus_snapped_away=false`, `ROOM_TRANSITION=false` (`MLS:602-603`); `Megaman_beam=true` in `PSEH.init` before the event is submitted |
| Spawn handler frames | `Megaman_beam_center` (own vector), `Megaman_beam=false` at the landing |
| Room transition | `ROOM_TRANSITION=true` at begin and each continue update, `false` at end |
| Each level frame (not paused) | `TIME_SINCE_LAST_FLOATING_POINTS += delta` |
| Enemy floating points | `LAST_FLOATING_POINTS_TYPE`, `TIME_SINCE_LAST_FLOATING_POINTS=0f`, `LEVEL_POINTS += n` |
| Level end | `Megaman_beam=true` (`ELEH.init` and `ELEH.beamEnd`), `Megaman_beam_center` each frame; at `END_LEVEL`, `WEAPONS_ATTAINED` (new robot master level only) |
| `MLS.reset()` (leave the level screen) | `ROOM_TRANSITION=false`, `focus_snapped_away=false` |
| Get-weapon screen, save load, `resetPlayerProgress` | `WEAPONS_ATTAINED` removed |

Values that no moment resets: `LAST_FLOATING_POINTS_TYPE`, `Megaman_beam`, `Megaman_beam_center`, `level_def`,
the suppliers, the world container and the map load result (each is replaced at the next level start). The typed
fields must keep these values across levels in the same way. No `LevelState` field is reset as a whole.

## 5. Read-through and copies

- No reader copies a bag value to keep it, except `DarknessV4` (keeps the beam center reference, section 2.3) and
  `MegaPathfinderFactory` (a world container snapshot per frame, keyed by the reference, `:24-34`). Both keep the
  same reference if the typed field holds the same object.
- Rooms and the current room are suppliers that the level screen sets; the readers call the supplier each time. The
  typed fields must keep the suppliers (read at use), not copy the rooms.

## 6. Tests

- 0 tests call the bag of the game.
- 8 tests stub `game.getCurrentLevel()` on a relaxed `MegamanMaverickGame` mock (`CartV2ItemTest`,
  `StarknerAliveTest`, `SpawnerReleaseTest`, `EntityExistenceTest`, `SpiderWebDestroyTest`, `EntityGroupsTest`,
  `MapObjectComponentTest`, `LaserIgnoreTest`). If `getCurrentLevel()` stays a function of the game, these stubs do
  not change.
- On a relaxed mock, an unstubbed bag read returns a mock default today. If the code reads a typed field through a
  `LevelState` property of the game, the relaxed mock returns a relaxed `LevelState` mock (`false`, `0`, `null`).

## 7. Keys and `ConstKeys`

`ConstKeys` constants that only the bag uses (count of `ConstKeys.X` references in `core` and `lwjgl3`): `COINS` 6,
`LEVEL_POINTS` 4, `TIME_SINCE_LAST_FLOATING_POINTS` 5, `LAST_FLOATING_POINTS_TYPE` 2, `WEAPONS_ATTAINED` 6,
`ROOM_TRANSITION` 27 (26 calls and 1 commented line in `ReactorMan_OLD.kt`), `TILED_MAP_LOAD_RESULT` 2, `WORLD_CONTAINER` 2, `DRAWABLES` 2, `SHAPES` 2, `SYSTEMS` 2. All of
these references are bag calls. `LEVEL`, `DEF`, `CAM`, `ROTATION`, `ROOM`, `ROOMS`, `SUPPLIER`, `BEAM` and `CENTER`
have other users.

## 8. Compiler probe

One local build of `core:compileKotlin` (reverted at once, `git status` clean after it): in `MMG`, the internal bag
calls went through `properties` directly, and `MMG` overrode the 7 bag functions that callers use (`putProperty`,
`getProperty` (2 forms), `getOrDefaultProperty`, `hasProperty`, `isProperty`, `removeProperty`) with
`@Deprecated(level = DeprecationLevel.ERROR)`. Result: exactly 58 errors, all "is deprecated", and the list of
`file:line` equals the `grep` list of section 2 (a `diff` of the two lists is empty). So no other code calls the
bag of the game, through any receiver.

(A first try that removed `IPropertizable` stopped at the errors in `MMG` and reported no other file.)

## 9. Findings that are not part of the change

- `ELEH:227` puts `Megaman_beam=true` at the end of the beam-out, where `false` is the likely intent. Effect: after a
  level end, `DarknessV2`/`DarknessV4` would add a beam light at the last beam center until the next player spawn
  lands. The level screen is left at once (`END_LEVEL`), and the next spawn sets `true` again, so no effect is
  visible. Kept as is; noted in `TODO.md` as a bug candidate.
- `LAST_FLOATING_POINTS_TYPE` is not reset at level start (section 2.7). Kept as is.
- `WEAPONS_ATTAINED` can stay set after a save load in a path that does not pass `MainMenuScreen:161` (only
  `MainMenuScreen` calls `loadSavedState`, so no such path exists today).
- `getWorldContainer()` is read from the async pathfinding thread. A typed field read is no less safe than the
  `ObjectMap` read today; neither is synchronized. Kept as is.

## 10. Live content for the checks

From the live TMX files (`LevelDefinition`):

- `DarknessV2`: `DesertMan_16x14_v4.tmx` (outside every room), `WilyStage1_v2.tmx` (`room8`). No live map uses
  `DarknessV4`.
- `Coin` and `QuestionBlock`: `SMB3_1.tmx` (`MARIO_LEVEL`; `room1`, `underground_room_1`, `room2`, `room3`). Player
  spawn only in `room2`.
- `GravitySwitcharoo` (camera rotation): `MoonMan_16x14_v5.tmx` (`room8`, `room9`, `room10`).
- `AsteroidsSpawner`: `MoonMan_16x14_v5.tmx`. `InfernoMeteorShower`, `RisingLavaRiver`: `InfernoMan_16x14_v3.tmx`.
- Each robot master boss sprite reads `ROOM_TRANSITION`.

The exact rooms for the manual checks go in `CONTRACT.md`.

## 11. Overlap with other branches

No other code branch is active. The parent branch `mega-refactor-016-master` (created from `master` at
`06901895e`) is the merge target of this branch. The branch start `44c6b5fa3` is an ancestor of the parent.
