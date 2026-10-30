# Scan: `016b` (a home for the host-only reads in entities)

Base: the parent `mega-refactor-016-master` at `07307f156` (the branch start; it contains the merged `016a`). `C/` =
`core/src/main/kotlin/com/megaman/maverick/game/`, `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `MMG` =
`C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`. Line numbers are on the base.

Method: a script lists every `game.<member>` read in `C/entities/`. It skips comment, `import` and `package` lines and
package paths such as `com.megaman.maverick.game.screens.x`. `grep` finds the calls that pass `game` as an argument
and the reads through another reference (`entity.game.x`). Each site in sections 2 and 3 was read in the code.

## 1. Short answer

- The item list (`ITEM.md`, branch 2) has 7 host-only reads plus `WEAPONS_ATTAINED`. All 7 are still in the code at
  the same places. `WEAPONS_ATTAINED` is done: `016a` made it the typed session field `game.weaponsAttained`
  (`MMG:224`), not saved, outside `GameState`. Its one entity write (`Megaman.kt:921`) needs no change in `016b`; in
  `016c` the holder gets the same field.
- The 7 reads left: `DrWily.kt:152` (`setCurrentScreen`), `Starkner.kt:414` (`paused`), `ControllerComponent.kt:99`
  and `AirDashBehavior.kt:33`, `:38` (`selectButtonAction`), `Bat.kt:405` and `SubmarineJoe.kt:281` (`getShapes()`).
- The scan also found entity reads of session members that are not host-only, but that no planned holder object
  answers yet: the clock (`getPerformance()`, 8), the scheduler (`updatables` 7, `runQueue` 1), the groups system
  (`getSystem(GroupsSystem::class)`, 1), and 11 calls that pass `game` to a function that takes
  `MegamanMaverickGame` (`delayRun`, `delayNextPossibleSpawn`, `isNextPossibleSpawnDelayed`). The item `NOTES.md`
  (section 10) put these in `016b`; `ITEM.md` does not. `SCOPE.md` asks the owner (open question 1).
- Every other entity read goes to an object that the holder will hold with the same name (`assMan`, `eventsMan`,
  `state`, `players`, `gravity`, `levelState`, `audioMan`, `engine`, `weaponsAttained`), or to a game function that
  `016c` forwards (`getGameCamera()`, `isCameraRotating()`, `setFocusSnappedAway()`, `getCurrentLevel()`,
  `getCurrentRoom()`, `getRooms()`, `getTiledMapLoadResult()`; `016a` `SCOPE.md` open question 4).

## 2. The host-only reads

### 2.1 `DrWily` switches the screen

- `DrWily.kt:146-153`, in `update(delta)`, state `LAND`: when `stillTimer.isJustFinished()` and
  `targetPlayer.existenceState.liveAfterUpdate`, it calls `game.setCurrentScreen(ScreenEnum.ENDING_SCREEN.name)`.
  `update` is the entity's updatable, so the call runs inside `engine.update` (`UpdatablesSystem`), inside
  `MLS.render` (`MLS:956`).
- `MMG:953-965` `setCurrentScreen`: `hide()` and `reset()` of the current screen (the level screen; `MLS.reset` at
  `MLS:1100-1134` calls `engine.reset()`, so the engine resets inside its own update), then `show()` and `resize()` of
  the ending screen, then `resume()` if the game is paused.
- `DrWily` is spawned only by `WilyFinalBoss.onDestroy` (`WilyFinalBoss.kt:293`), after phase 3, if the player
  lives. No map places it. Live map: `WilyStage3_v4.tmx` (`WILY_STAGE_3`), boss in `wily_room`.
- It is the only entity call to `setCurrentScreen`. Screens make the other 31.
- Owner decision 9 (`ITEM.md`): it becomes a `LevelState` callback that the level screen sets, not an event. An event
  would run in `eventsMan.run()` after `screen.render`, so the switch would come later in the frame.
- `LevelState` (`C/levels/LevelState.kt`) is a session field of the game (`MMG:189`). It has no callback field yet.
- `MLS.init` (`MLS:223-427`) runs once (flag `initialized`); `MLS.start` (`MLS:429`) runs at each level start and
  throws if `init` did not run. So a callback that `MLS.init` sets exists before any level entity updates.

### 2.2 `Starkner` reads `paused`

- `Starkner.kt:402-423`, the sprite `preProcess`: `if (!game.paused)` the sprite turns (`rawRotation += ...`) or snaps
  to `direction.rotation`; when paused, `rawRotation` stays. Then `sprite.rotation` is set from `rawRotation`.
- Why the check is needed: during a pause `LevelStateHandler.pause` (`C/screens/levels/LevelStateHandler.kt:13-22`)
  turns off every system except `SpritesSystem`, so the sprite `preProcess` still runs with the frame delta.
- `paused`: `MMG:229-230`, `var paused = false; private set`. Written only by `pause()` and `resume()`
  (`MMG:715-727`), which also call the current screen's `pause()`/`resume()`. Session value, changes at run time.
- Other readers (not entities): `MLS` 7 (`:643`, `:930`, `:936`, `:940`, `:966`, `:1005`, `:1058`), 6 menu and intro
  screens (`RobotMasterIntroScreen:226`, `RobotMasterIntroScreenV2:240`, `temp_BossIntroScreen:143`,
  `MainMenuScreen:249`, `LevelSelectScreen:379`, `LevelSelectScreenV2:245`), `MMG:964`, and the launcher 3
  (`DesktopLauncher.kt:165`, `:169`, `:173`).
- Live map: `MoonMan_16x14_v5.tmx` (`MOON_MAN`), `room12` (2) and `room13` (1).
- Test: `StarknerAliveTest` builds a `Starkner` with a relaxed game mock; it does not run the sprite `preProcess`.
- `013` (level flow, pause as an overlay) plans to remove the `game.paused` checks later.

### 2.3 `selectButtonAction`

- Field: `MMG:216`, `lateinit var selectButtonAction: SelectButtonAction`, set to `NEXT_WEAPON` in `create()`
  (`MMG:305`). Session value, changes at run time.
- Entity readers (all are parts of `Megaman`):
  - `ControllerComponent.kt:98-117`: the `select` actuator, on release: `NEXT_WEAPON` goes to the next weapon.
  - `AirDashBehavior.kt:33`, `:38`: with `AIR_DASH`, the air dash uses `SELECT`; else it uses `A`.
- Other readers and writers (screens): `ControllerSettingsScreen.kt:220` (load from prefs), `:248` (reset to
  `NEXT_WEAPON`), `:319` (text), `:341-342`, `:347-348` (change and save); `MLS:918` (with `START`, `SELECT` pauses).
- The value is saved in the controller prefs (`C/controllers/ControllerUtilsExtensions.kt:79-95`), not in the save
  file (`GameState`).
- Plans in `031` (`ITEM.md` decisions 2026-10-25, `NOTES.md` case 10): `selectButtonAction` is one setting per
  player, kept in a settings object per slot; "Keep it as one game setting until co-op. In `047` it moves to the
  slot." `MegaPlayers` (`C/players/MegaPlayers.kt:14`) is the players service; it holds `inputs` (per slot) and is a
  planned holder field.
- `players` is `lateinit`, built in `postCreate` (`MMG:421`; `gravity` at `:425`). `create()` (which writes the default today)
  runs before `postCreate`. No code reads `selectButtonAction` before `postCreate`: its readers are screens built in
  `postCreate` and `Megaman`.

### 2.4 Debug shapes in `Bat` and `SubmarineJoe`

- `Bat.kt:405`, `SubmarineJoe.kt:281`: `shapes = if (DEBUG_PATHFINDING) game.getShapes() else null`, an argument of
  `MegaPathfinderResultConsumer.consume`.
- `DEBUG_PATHFINDING` is `private const val ... = false` in both companions (`Bat.kt:78`, `SubmarineJoe.kt:90`). So
  the expression is always `null` and `game.getShapes()` never runs.
- `MegaPathfinderResultConsumer.consume` (`C/pathfinding/MegaPathfinderResultConsumer.kt:20-71`): parameter
  `shapes: Array<IDrawableShape>? = null` (`:33`); when not `null`, it adds a `GameLine` per path segment (`:49-55`).
  4 calls in `core` main; only these 2 pass `shapes`.
- `getShapes()` (`MMG:999`) returns the session shape collector (`MMG:195`, built in `createGameEngine` `:885-886`;
  `DrawableShapesSystem` adds to it, `:925`). `MLS:232` also reads it (it draws the shapes). The launcher flag
  `--debugShapes` (`params.debugShapes`) is a different switch: it controls `DrawableShapesSystem`, not these lines.
- Live maps: `Bat` in many levels (for example `TIMBER_WOMAN` `room1`); `SubmarineJoe` in `WilyStage2_v2.tmx`
  (`path1_room1`) and `Test1.tmx` (`room29`).

### 2.5 `WEAPONS_ATTAINED` (done in `016a`)

- `MMG:224` `var weaponsAttained: OrderedSet<MegamanWeapon>? = null`; entity write `Megaman.kt:921`. Readers are
  screens and `RobotMasterLevelOnCompletion`. No `016b` change; `016c` puts the same field on the holder (decision
  10).

## 3. Other entity reads with no holder object yet

These are not host-only, but `016c` cannot make them compile as a type-only change unless the holder has a member
with the same name and type.

| Read | Sites | Today | Planned holder field (`ITEM.md`) |
|---|---|---|---|
| `game.getPerformance()` (`.fixedStep` 7, `.fps` 1) | `SigmaRatClaw.kt:312`, `BigAssMaverickRobotHand.kt:384`, `FallingLeaf.kt:166`, `RotationAnchor.kt:123`, `SpiderWeb.kt:175`, `DragonFly.kt:164`, `AngryFlameBall.kt:101`, `SwingingPlatform.kt:115` | `params.performance` (`MMG:1067`); auto-perf and the settings panel change it at run time (`MMG:1069-1077`) | `clock` (read-through) |
| `game.updatables` | `ToxicWater.kt:111`, `:112`, `:122`; `GameEntityUtils.kt:183`, `:186`, `:191`; `MegamanWeaponsHandler.kt:691` | `MMG:203`, session `OrderedMap<String, Updatable>` | `scheduler` |
| `game.runQueue` | `GameEntityUtils.kt:182` | `MMG:206`, session `Queue<() -> Unit>`, drained LIFO | `scheduler` |
| `game.getSystem(GroupsSystem::class)!!` | `GameEntityUtils.kt:81` | the systems map (`MMG:1001-1007`); `GroupsSystem` built in `createGameEngine` | `groups` |
| `game` passed to `MegaUtilMethods.delayRun(game: MegamanMaverickGame, ...)` | `WilyFinalBoss.kt:972`, `:1962`, `:1974`; `MegamanWeaponsHandler.kt:655`, `:699`; `CannoHoney.kt:148`, `:316` | the function reads `updatables` and `runQueue` (`C/utils/MegaUtilMethods.kt:15-31`); only entities call it | `scheduler` |
| `game` passed to `delayNextPossibleSpawn` / `isNextPossibleSpawnDelayed` (`GameEntityUtils.kt:179-193`) | `Jetto.kt:136`, `:185`; `BikerKibbo.kt:104`, `:133` | same | `scheduler` |
| `ToxicWaterWaveAnimator(private val game: MegamanMaverickGame)` | `ToxicWater.kt:40`, `:87` | a private class in the entity file; reads `game.assMan` | type swap with the entity |

All of these are session objects (or a read-through of a session value). None is host-only. Each fits a holder field
that `ITEM.md` already names. The `game`-taking functions in `utils/` and `entities/utils/` have no caller outside
`C/entities/`.

## 4. Reads that need no `016b` work

| Member | Reads (files) | Holder member in `016c` |
|---|---|---|
| `assMan` | 391 (372) | same object |
| `eventsMan` | 117 (55) | same object |
| `state` | 110 (48) | same object |
| `getGameCamera()` | 70 (54) | forward (camera) |
| `isCameraRotating()` | 39 (33) | forward (camera flag) |
| `levelState` | 34 (22) | same object |
| `gravity` | 26 (18) | same object |
| `audioMan` | 16 (11) | same object |
| `getCurrentLevel()` | 16 (10) | forward (`levelState`) |
| `getCurrentRoom()` | 12 (9) | forward (`levelState`) |
| `setFocusSnappedAway()` | 7 (3) | forward (camera flag) |
| `engine` | 2 (2) | same object |
| `players` | 2 (1) | same object |
| `getTiledMapLoadResult()` | 2 (1) | forward (`levelState`) |
| `getRooms()` | 1 (1) | forward (`levelState`) |
| `weaponsAttained` | 1 (1) | same field |

Reads through another reference (`AbstractEnemy.kt:104` `it.game.state`, `AnimatedBlock.kt:119` `block.game.assMan`,
`GameEntityUtils.kt:51` `entity.game.eventsMan`, `:79` `(entity as MegaGameEntity).game`,
`FreezableEntityHandler.kt:53` `ShieldEntity(entity.game)`, `IMegaGameEntity.kt:31`) use the same members. `016c`
handles them with the `IMegaGameEntity` cleanup.

Three comment lines name `game.setDebugText` (`ElecDevil.kt:558`, `AnimationsComponent.kt:34`, `UnderwaterFan.kt:254`);
they are not code.

## 5. The drawables map (owner-approved small change)

- `MMG:194`: `private lateinit var drawables: ObjectMap<DrawingSection, java.util.Queue<IComparableDrawable<Batch>>>`.
- `MMG:881-883` (`createGameEngine`): a local map of the same declared type; each `DrawingSection` gets a new
  `InsertionOrderPriorityQueue()`. These are the only puts. So every value is an `InsertionOrderPriorityQueue`
  (`E/common/objects/InsertionOrderPriorityQueue.kt`): a `java.util.Queue` that sorts by priority and keeps the
  insertion order for equal priorities. The draw order depends on this.
- `MMG:991` `getDrawables()` (return type inferred from the field); `MMG:993-997` `addDrawable` adds to the queue of
  the section (callers: `FontsSystem`, `SpritesSystem` through `this::addDrawable`, `PlayerSpawnEventHandler`,
  `EndLevelEventHandler`).
- `MLS:195`: `private lateinit var drawables: ObjectMap<DrawingSection, Queue<IComparableDrawable<Batch>>>`
  (`Queue` from `import java.util.*`, `MLS:110`); set from `game.getDrawables()` (`MLS:231`); drawn with
  `isEmpty()`/`poll()` loops (`MLS:1023-1041`).
- The type change is a declaration change only: same objects, same calls. `InsertionOrderPriorityQueue<T :
  Comparable<T>>`; `IComparableDrawable<Batch>` must be `Comparable<IComparableDrawable<Batch>>` (it is today, since
  the map is filled with that type).

## 6. Findings that are not part of the change

- `InsertionOrderPriorityQueue.size` (`E/common/objects/InsertionOrderPriorityQueue.kt`) is
  `override val size = queue.size`, a value fixed at construction (`0`), not a getter. `isEmpty()` is correct. No
  code reads `size` of a drawables queue (the draw loops use `isEmpty()`/`poll()`), so no effect is visible. Bug
  candidate in `TODO.md`.
- `MLS.reset` runs inside `engine.update` when `DrWily` switches the screen (section 2.1): the engine resets during
  its own update. This is today's behavior; `016b` keeps it.

## 7. Live content for the checks

- `DrWily`: `WILY_STAGE_3`. The boss room is `wily_room` (`event=boss`). The room before it is `room9`, which has no
  player spawn; the nearest room before it with a spawn is `room8`.
- `Starkner`: `MOON_MAN` `room12`, `room13`.
- `Bat`: for example `TIMBER_WOMAN` `room1`, `RODENT_MAN`, `DESERT_MAN` `room10`; `SubmarineJoe`: `WILY_STAGE_2`
  `path1_room1`, `TEST_1` `room29`.
- `selectButtonAction`: any level (needs the controller settings screen and, for `NEXT_WEAPON`, weapons:
  `--weapons all`).

The exact rooms and their player spawns go in `CONTRACT.md`.

## 8. Overlap with other branches

No other code branch is active (`git worktree list`: the main checkout, the parent, this branch). The parent
`mega-refactor-016-master` is the merge target. `031` (active item, no active branch) plans per-slot settings with
`selectButtonAction` (section 2.3). `013` (planned) owns pause later (section 2.2).
