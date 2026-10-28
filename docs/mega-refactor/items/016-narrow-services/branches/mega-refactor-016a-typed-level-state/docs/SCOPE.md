# Scope: mega-refactor-016a-typed-level-state

- Status: approved
- Approved: 2026-10-28
- Merge target: the parent branch `mega-refactor-016-master` (owner decision 2026-10-28, "Parent branches" in
  `docs/mega-refactor/CLAUDE.md` on `master`), not `master`.

## Goal

Typed fields replace the property bag of `MegamanMaverickGame`. Level values go into a session-lived `LevelState`.
The two camera flags and the weapons-attained set become typed fields of the game. The game stops being an
`IPropertizable`. Every typed field starts with the value that the bag gives today for a missing key, and is written
and cleared at the same moments. No gameplay change.

## Item

From `016` `ITEM.md`, "Proposed scope", branch 1: "`016a` `typed-level-state`: the property bag becomes typed
`LevelState` fields and camera flags."

Owner decisions that apply (`ITEM.md`, "Decisions", 2026-10-28):

- 3: "The level part is a session-lived `LevelState`, not a per-level `LevelContext`."
- 5: "Keep the name `game` and the old member names during the swap; rename later (`016e`)."
- 10: "`WEAPONS_ATTAINED` becomes a typed field that is not saved, outside `GameState` (the saved profile)."
- From "Proposed scope": "Pooled entities outlive a level, so the holder and every field live for the session. Values
  that change at run time (performance, `paused`, the current level and room) are read when used, not copied." and
  "A field only with a caller."

Not in this branch (from the coordinator's brief): `MegaServices` and the entity constructor change (`016c`);
`DrWily`'s screen switch (`016b`).

## Current state

Full scan: `docs/SCAN.md`. Base `44c6b5fa3`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `MMG` =
`C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`.

- `MMG:150` implements `IPropertizable`; `MMG:189` holds the bag. It lives for the session and is never cleared.
- 58 external calls in 30 files (the item `NOTES.md` says 26; proved by a compiler probe, `SCAN.md` section 8), on 8 keys:
  `ROOM_TRANSITION` (26), `Megaman_beam` (6), `Megaman_beam_center` (4), `COINS` (6), `LEVEL_POINTS` (4),
  `TIME_SINCE_LAST_FLOATING_POINTS` (5), `LAST_FLOATING_POINTS_TYPE` (2), `WEAPONS_ATTAINED` (5). (Corrected 2026-10-28 in step 3a: the first
  draft copied 27 and 4 from the item `NOTES.md`; the total of 58 did not change.)
- 11 internal keys behind typed functions of the game: `level_def`, `cam_rotation`, `focus_snapped_away`, the rooms
  and current-room suppliers, `WORLD_CONTAINER`, `TILED_MAP_LOAD_RESULT`, and the session collectors `DRAWABLES`,
  `SHAPES`, `SYSTEMS`. Plus 1 internal `removeProperty(WEAPONS_ATTAINED)` (`MMG:786`).
- Defaults and moments per key: `SCAN.md` sections 2 to 4. The important facts:
  - Every `Boolean` key reads `false` when unset. `COINS` and `LEVEL_POINTS` read `0` in gameplay code and `null` in
    the HUD text; `MLS.start` sets both to `0` before the HUD can draw.
  - `TIME_SINCE_LAST_FLOATING_POINTS` has no unset default (`!!`); `MLS.start` sets `0f` before any reader runs.
  - `MLS.start` resets only coins, points, the floating-points timer, the camera rotation flag, the focus flag, the
    suppliers, the world container and the map load result. `LAST_FLOATING_POINTS_TYPE`, the beam flag and the beam
    center are never reset. No field is reset as a whole.
  - The beam center is a reference to a handler's own `Vector2`; `DarknessV4` keeps that reference.
  - `WEAPONS_ATTAINED` is never `null` when set (an empty set for a level with no reward); `hasProperty` is "set".

## In scope

Core:

1. New class `LevelState` in `C/levels/LevelState.kt`, a plain class with `var` fields and no logic. Each field
   starts with the unset default of its key today:

   | Field | Type | Start value | Replaces |
   |---|---|---|---|
   | `currentLevel` | `LevelDefinition?` | `null` | `level_def` |
   | `roomsSupplier` | `(() -> Array<RectangleMapObject>?)?` | `null` | `rooms_supplier` |
   | `currentRoomSupplier` | `(() -> RectangleMapObject?)?` | `null` | `room_supplier` |
   | `worldContainer` | `IWorldContainer?` | `null` | `WORLD_CONTAINER` |
   | `tiledMapLoadResult` | `TiledMapLoadResult?` | `null` | `TILED_MAP_LOAD_RESULT` |
   | `roomTransition` | `Boolean` | `false` | `ROOM_TRANSITION` |
   | `megamanBeam` | `Boolean` | `false` | `Megaman_beam` |
   | `megamanBeamCenter` | `Vector2?` | `null` (holds the writer's reference, not a copy) | `Megaman_beam_center` |
   | `coins` | `Int` | `0` | `COINS` |
   | `levelPoints` | `Int` | `0` | `LEVEL_POINTS` |
   | `timeSinceLastFloatingPoints` | `Float` | `0f` | `TIME_SINCE_LAST_FLOATING_POINTS` |
   | `lastFloatingPointsType` | `FloatingPointsType?` | `null` | `LAST_FLOATING_POINTS_TYPE` |

2. `MegamanMaverickGame`:
   - new `val levelState = LevelState()`, built with the game object (so it exists before `create()` and lives for
     the session; see open question 1);
   - new `var weaponsAttained: OrderedSet<MegamanWeapon>? = null`, next to `state`;
   - the camera flags become `private var cameraRotating = false` and `private var focusSnappedAway = false`;
   - the session collectors become private typed fields (`drawables`, `shapes`, `systems`), set in
     `createGameEngine` at the same points;
   - the existing functions keep their names, parameters and return types, and only change their bodies:
     `setCurrentLevel`, `getCurrentLevel` (`levelState.currentLevel!!`), `isInExtraLevel`, `setRoomsSupplier`,
     `getRooms`, `setCurrentRoomSupplier`, `getCurrentRoom`, `setWorldContainer`, `getWorldContainer`,
     `setTiledMapLoadResult`, `getTiledMapLoadResult` (`levelState.tiledMapLoadResult!!`), `setCameraRotating`,
     `isCameraRotating`, `setFocusSnappedAway`, `isFocusSnappedAway`, `getDrawables`, `getShapes`, `getSystems`,
     `resetPlayerProgress` (`weaponsAttained = null`). So their callers do not change;
   - `IPropertizable`, `properties` and the related imports go.
3. The 58 call sites read and write the typed fields directly, at the same place in the code, with the same
   expression shape:
   - `isProperty(ROOM_TRANSITION, true)` → `game.levelState.roomTransition`; `putProperty(ROOM_TRANSITION, x)` →
     `game.levelState.roomTransition = x` (21 reads, 5 writes);
   - beam flag and center → `megamanBeam`, `megamanBeamCenter` (the handlers assign their own `beamCenter` object);
   - `COINS`, `LEVEL_POINTS` → `coins`, `levelPoints` (`+1`, `+ points`, `= 0`; HUD `"COINS: " + coins.toString()`);
   - `TIME_SINCE_LAST_FLOATING_POINTS`, `LAST_FLOATING_POINTS_TYPE` → `timeSinceLastFloatingPoints`,
     `lastFloatingPointsType`;
   - `WEAPONS_ATTAINED` → `game.weaponsAttained` (`hasProperty` → `!= null`; `removeProperty` → `= null`).
4. `ConstKeys`: remove the 11 constants that have no user left (`COINS`, `LEVEL_POINTS`,
   `TIME_SINCE_LAST_FLOATING_POINTS`, `LAST_FLOATING_POINTS_TYPE`, `WEAPONS_ATTAINED`, `ROOM_TRANSITION`,
   `TILED_MAP_LOAD_RESULT`, `WORLD_CONTAINER`, `DRAWABLES`, `SHAPES`, `SYSTEMS`), if the build shows no other user
   (open question 9).

Docs: root `CLAUDE.md`, "Game object": add `levelState: LevelState` (session-lived level values; set by the level
screen at level start, read when used) and `weaponsAttained`, and say that the game has no property bag.

Tests:

5. Temporary `Temp016aBagEquivalenceTest` with `Temp016aFrozenGameBag` (core test sources). The frozen copy is a
   `Properties` with the old read and write expressions of each key, copied from the base (for example
   `isProperty(ROOM_TRANSITION, true)`, `getOrDefaultProperty(COINS, 0, Int::class)`, the HUD
   `getProperty(COINS, Int::class).toString()`, the `AbstractEnemy` floating-points steps, `hasProperty` and
   `removeProperty` for `WEAPONS_ATTAINED`, the `level_def`, `cam_rotation` and `focus_snapped_away` functions, the
   suppliers). The new side is a real `MegamanMaverickGame` object (its `levelState`, `weaponsAttained` and the
   kept functions). Seeded random sequences of the writer operations of `SCAN.md` section 4 (level start, spawn,
   room transition, coin, floating points, beam writes, level end, screen reset, weapons put and remove, level set,
   camera flags) run on both. After each operation, every read expression gives the same value on both sides
   (references compared by identity for the beam center and the suppliers), except the two reads that are
   unreachable in the game (open question 7), which the suite checks only after a level start.
6. Permanent `LevelStateTest` (and checks on a real `MegamanMaverickGame`): the start values in item 1; the game
   functions before the first write (`getCurrentLevel()` and `getTiledMapLoadResult()` throw, `isInExtraLevel()`
   is `false`, `getRooms(out)` returns `out` unchanged, `getCurrentRoom()` is `null`, both camera flags are
   `false`, `weaponsAttained` is `null`); the beam center keeps the reference that was set; values stay after a
   new level is set (no reset as a whole).
7. The 8 tests that stub `game.getCurrentLevel()` stay unchanged and green.

New API and first production caller:

- `LevelState` (class): `MegamanMaverickGame.levelState`.
- `MegamanMaverickGame.levelState`: the 58 call sites (first: `MLS:319`) and the kept game functions (first:
  `getCurrentLevel`).
- Each `LevelState` field: `currentLevel` (`setCurrentLevel`/`getCurrentLevel`), `roomsSupplier` (`getRooms`),
  `currentRoomSupplier` (`getCurrentRoom`), `worldContainer` (`getWorldContainer`), `tiledMapLoadResult`
  (`getTiledMapLoadResult`), `roomTransition` (`MLS:319`, `Saw.kt:133`), `megamanBeam` (`PlayerSpawnEventHandler`,
  `DarknessV2`), `megamanBeamCenter` (`PlayerSpawnEventHandler`, `DarknessV2`), `coins` (`Coin`, HUD),
  `levelPoints` (`AbstractEnemy`, HUD), `timeSinceLastFloatingPoints` (`AbstractEnemy`, `MLS.render`),
  `lastFloatingPointsType` (`AbstractEnemy`).
- `MegamanMaverickGame.weaponsAttained`: `Megaman.onAddLevelDefeated`, `GetWeaponScreen.show`,
  `RobotMasterLevelOnCompletion`, `MainMenuScreen`, `resetPlayerProgress`.
- Private fields `cameraRotating`, `focusSnappedAway`, `drawables`, `shapes`, `systems`: the kept game functions.

Removed API: `IPropertizable` on `MegamanMaverickGame` (`properties` and the 20 interface functions on the game), and
the 11 `ConstKeys` in item 4.

## Out of scope

- `MegaServices`, the entity constructor type and the `IMegaGameEntity` cleanup (`016c`). Callers keep `game.`.
- `DrWily`'s screen switch, `Starkner`'s `paused`, `selectButtonAction`, the debug `getShapes()` reads, a direct
  `GroupsSystem` field (`016b`). `getShapes()` keeps its callers here.
- Renames of the kept functions or of `game` (`016e`).
- A level-start reset function on `LevelState`. The writes stay at their lines in `MLS.start`.
- The `EndLevelEventHandler` beam write (`SCAN.md` section 9): a bug candidate in `TODO.md`, not changed.
- Thread safety of `getWorldContainer()` from the async pathfinding thread (no change in either direction).
- `IPropertizable` on entities, bodies, fixtures and events (not the game bag).

## Affected areas and risks

- Files: `MMG`, new `C/levels/LevelState.kt`, `C/Constants.kt`, and the 30 files of the 58 calls: `MLS`,
  `PlayerSpawnEventHandler`, `EndLevelEventHandler`, `SpawnersLayerBuilder`, `GetWeaponScreen`, `MainMenuScreen`,
  `RobotMasterLevelOnCompletion`, `AbstractEnemy`, `Coin`, `QuestionBlock`, `Megaman`, `ControllerComponent`,
  `MegamanAnimationKeySupplier`, `ClimbBehavior`, `DarknessV2`, `DarknessV4`, `PushableBlock`, `Saw`,
  `AsteroidsSpawner`, `InfernoMeteorShower`, `PipePuff`, `RisingLavaRiver`, and the 8 bosses (`InfernoMan`,
  `GlacierMan`, `DesertMan`, `ReactorMan`, `PreciousWoman`, `MoonMan`, `RodentMan`, `TimberWoman`). Root
  `CLAUDE.md`.
- Risk: a field that the old code reset (or did not reset) at a moment that the new code changes. Mitigation: each
  write stays at its line; no new reset; the temporary suite runs the moments of `SCAN.md` section 4.
- Risk: a copy in place of a reference (beam center, suppliers). Mitigation: assignments of the same object; the
  suite compares by identity.
- Risk: a wrong default. Mitigation: the table in item 1; permanent tests.
- Risk: tests with a relaxed game mock now read `game.levelState` as a relaxed `LevelState` mock (`false`, `0`,
  `null`), the same answers as an unstubbed bag today.
- Cost: a field read in place of an `ObjectMap` lookup with a string key on each read (`ROOM_TRANSITION` is read by
  every non-block spawner each update and by many sprites each frame). A small gain; no boxing of `Boolean`,
  `Int` and `Float`.

## Overlap with other branches

No other code branch is active. The parent `mega-refactor-016-master` (from `master` at `06901895e`) is the merge
target; the branch start `44c6b5fa3` is its ancestor. At the merge step, compare with
`git diff --stat mega-refactor-016a-typed-level-state...mega-refactor-016-master`. `016b` and `016c` will build on
`levelState` and `weaponsAttained`.

## Open questions

None. Resolved by the owner (2026-10-28, "Scope approved"): all 13 recommendations below are accepted as
written.

1. Where `LevelState` lives until `016c`: a field of `MegamanMaverickGame`, built with the game object
   (`val levelState = LevelState()`), like `players` and `gravity` are session fields of the game today.
   Recommendation: yes. `016c` moves the same object into `MegaServices`. Other options: a global `object` (no:
   global state), or a `lateinit` built in `create()` (no need: it has no dependency).
2. The name of the game field: `levelState` or `level`. Recommendation: `levelState`. `level` reads as "the current
   level" (`getCurrentLevel()`). `016e` can rename.
3. The 11 internal keys in scope, so that the game drops `IPropertizable`: the level ones go into `LevelState`, the
   camera flags and the 3 session collectors become private fields of the game. Recommendation: yes. The brief names
   only the 58 calls, but item text says "typed fields replace the `game` property bag", and with half of the bag
   left the game keeps the whole `IPropertizable` interface.
4. The existing typed functions of the game (`getCurrentLevel`, `getRooms`, `isCameraRotating`, ... the list in
   item 2) keep their signatures and only change their bodies. Recommendation: yes (decision 5; 24 + 40 + other
   callers and 8 test stubs do not change). `016c` decides which of them `MegaServices` forwards.
5. The 58 call sites use `game.levelState.<field>` directly, not new forwarding functions on the game.
   Recommendation: yes. New functions on the game would grow the host that `016` shrinks.
6. The camera flags: private fields of the game behind the 4 existing functions, not a new camera class now.
   Recommendation: yes. A camera class has no caller in `016a` other than the game; `016c` builds the camera
   service with the camera and both flags.
7. Two reads that differ only before the first level start, which the game cannot reach: the HUD text
   (`"POINTS: null"`/`"COINS: null"` today, `"POINTS: 0"`/`"COINS: 0"` new) and `timeSinceLastFloatingPoints`
   (today a `NullPointerException`, new `0f`). The HUD draws only in `MLS.render` after `MLS.start`, and both
   readers of the timer run only after `MLS.start` (`SCAN.md` 2.4 to 2.6). Recommendation: accept with plain
   non-null fields; list them in `CONTRACT.md` as unreachable differences, not as intended changes. The other option
   (`Int?`/`Float?` fields to copy the `null` exactly) adds `!!` and `?: 0` at every reader for no gameplay reason.
8. `WEAPONS_ATTAINED`: a public `var weaponsAttained` on the game next to `state`, not a `LevelState` field.
   Recommendation: yes. It is not level data: it is written at `END_LEVEL` and at a save load, and read by the
   get-weapon screen after the level screen is gone. `016c` puts it on `MegaServices` next to `state` (decision 10).
9. Remove the 11 `ConstKeys` constants that no code uses after the change. Recommendation: yes; a constant with no
   user is dead code. The Tiled maps do not use these constants (only code names them).
10. Field names: keep the key names (`roomTransition`, `megamanBeam`, `megamanBeamCenter`, `coins`, `levelPoints`,
    `timeSinceLastFloatingPoints`, `lastFloatingPointsType`, `currentLevel`, `roomsSupplier`,
    `currentRoomSupplier`, `worldContainer`, `tiledMapLoadResult`, `weaponsAttained`). Recommendation: yes
    (decision 5). `megamanBeam` stays a player-specific name; a rename for co-op is `047` or `016e` work.
11. Package of `LevelState`: `C/levels/`, next to `LevelDefinition`. Recommendation: yes.
12. Manual checks: real rooms, no `RefactorTest016` map. Recommendation: yes. Each behavior depends on real level
    content (robot master bosses and their gates, `SMB3_1` coins and points in a `MARIO_LEVEL`, `DarknessV2` in
    `WILY_STAGE_1`, `GravitySwitcharoo` in `MOON_MAN`, the get-weapon screen after a robot master).
13. The temporary suite: a frozen copy of the old bag expressions against a real `MegamanMaverickGame` object
    (item 5). It proves the store, the defaults and the moments. It does not run the 58 call sites themselves
    (they need a full level); the compiler, the review of each one-line change, and the play test cover them.
    Recommendation: yes. Building a real `MegamanMaverickGame` in a test is checked in the first plan step; if its
    constructor needs LibGDX, the new side uses `LevelState` plus a test copy of the private flag logic, and the
    plan says so.
