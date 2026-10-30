# Plan: mega-refactor-016a-typed-level-state

- Status: approved
- Approved: 2026-10-28

How to do the work in `SCOPE.md` (approved 2026-10-28) so that the checks in `CONTRACT.md` (approved 2026-10-28)
pass. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`,
`MMG` = `C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`.

Rules for every step:

- One commit per step (step 3 has one commit per key group). Each commit builds and passes all tests.
- The build command is `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
  lwjgl3:jar`. "Build green" below means: it passes, engine 571 tests, core 219 plus the new tests, 0 failures, no
  new unique `^w:` line against the 87 of the baseline.
- Each changed call site keeps its place in the code and its expression shape (a write stays a write at the same
  line; a read stays a read). No new reset, no reordering.
- No code comments. Small edits with the Edit tool; a script only for the 21 identical `ROOM_TRANSITION` reads.
- No test map: the manual checks use real rooms (`SCOPE.md` open question 12).

Checked during planning (2026-10-28): a real `MegamanMaverickGame()` builds in a plain `core` unit test with no
LibGDX context, and its camera-flag, current-room and extra-level functions run (a scratch test, run once and
deleted). So the temporary suite and the permanent tests use a real game object; no fallback is needed
(`SCOPE.md` open question 13).

## Steps

### 1. `LevelState` and the internal keys of the game

- Changes:
  - New `C/levels/LevelState.kt`: `class LevelState` with the 5 fields that the internal keys need now:
    `currentLevel`, `roomsSupplier`, `currentRoomSupplier`, `worldContainer`, `tiledMapLoadResult` (types and start
    values as in `SCOPE.md` item 1).
  - `MMG`: `val levelState = LevelState()`; private `cameraRotating`, `focusSnappedAway`; private typed `drawables`,
    `shapes`, `systems`, set in `createGameEngine` at the lines where the bag is written today. The bodies of
    `setCurrentLevel`, `getCurrentLevel`, `isInExtraLevel`, `setCameraRotating`, `isCameraRotating`,
    `setFocusSnappedAway`, `isFocusSnappedAway`, `getDrawables`, `getShapes`, `getSystems`, `setRoomsSupplier`,
    `getRooms`, `setCurrentRoomSupplier`, `getCurrentRoom`, `setWorldContainer`, `getWorldContainer`,
    `setTiledMapLoadResult`, `getTiledMapLoadResult` use the typed fields. Signatures do not change.
  - The game still implements `IPropertizable` in this step (the 58 external calls still use the bag).
- New API and first caller: `LevelState` and `levelState` (`getCurrentLevel`); the 5 fields (the functions above);
  the private fields (the functions above).
- Check: build green. `grep -nE "properties\.|(put|get|getOrDefault|remove|has|is)Property\(" MMG` shows only
  `removeProperty(WEAPONS_ATTAINED)` (`MMG:786`) and the `event.` reads.
- Done: yes (2026-10-28)

### 2. Temporary suite and permanent tests for step 1

- Changes:
  - `T/Temp016aFrozenGameBag.kt`: a frozen copy of the old bag access. A class that holds a `Properties` and has
    one function per old expression, copied from the base `44c6b5fa3` with the same keys and casts: the 11 internal
    functions of `MMG` (`level_def`, `cam_rotation`, `focus_snapped_away`, the suppliers, the world container, the map
    load result) and, for step 3, the 58 external expressions (section "Frozen expressions" below).
  - `T/Temp016aBagEquivalenceTest.kt`: seeded random sequences (fixed seeds, for example 200 sequences of 200
    operations) of the write moments of `SCAN.md` section 4. Each operation calls the frozen copy and the real game
    in the same way. After each operation, every read is equal on both sides (the beam center, the suppliers, the
    world container and the map load result by identity). In this step: the operations and reads of the 11 internal
    keys.
  - `T/levels/LevelStateTest.kt` (permanent): the start values of the 5 fields; on a real `MegamanMaverickGame`:
    `getCurrentLevel()` and `getTiledMapLoadResult()` throw before the first write, `isInExtraLevel()` is `false`,
    `getRooms(out)` returns `out` unchanged with no supplier, `getCurrentRoom()` is `null`, both camera flags are
    `false`; the suppliers are called at each read (not copied); the values stay after a new `setCurrentLevel`.
- Check: build green; the new tests run and pass. Mutation check (by hand, not committed): change the start value
  of one camera flag to `true`, run the two suites, see both fail, revert.
- Done: yes (2026-10-28)

### 3. The 58 external calls, one commit per key group

Each sub-step adds the fields of its group to `LevelState` (or the game), changes the call sites of the group, adds
the operations and reads of the group to the temporary suite, and adds the start values to `LevelStateTest`.

- 3a. `ROOM_TRANSITION` (26 calls): field `roomTransition`. Writes `MLS:319`, `:341`, `:393`, `:603`, `:1130`.
  The 21 reads `game.isProperty(ConstKeys.ROOM_TRANSITION, true)` become `game.levelState.roomTransition` (a `sed`
  over the 21 files, then a review of each line; `SpawnersLayerBuilder.kt:34` keeps its lambda). Suite: begin,
  continue and end transition, spawn init, screen reset.
- 3b. Beam flag and center (10 calls): fields `megamanBeam`, `megamanBeamCenter`. `PlayerSpawnEventHandler.kt:134`,
  `:143`, `:218`; `EndLevelEventHandler.kt:120`, `:126`, `:227` (the write at `:227` stays `true`); `DarknessV2.kt:372`,
  `:375`; `DarknessV4.kt:556`, `:558`. The handlers assign their own `beamCenter` object. Suite: spawn init, spawn
  frames, landing, level-end init, level-end frames, beam end; the center compared by identity.
- 3c. Coins, points and floating points (17 calls): fields `coins`, `levelPoints`, `timeSinceLastFloatingPoints`,
  `lastFloatingPointsType`. `MLS:415`, `:422` (HUD text), `:512-514` (level start), `:990-991` (each frame);
  `Coin.kt:78-79`; `QuestionBlock.kt:192-193`; `AbstractEnemy.kt:312`, `:316`, `:323-324`, `:328-329`. Suite: level
  start, coin, question-block coin, enemy floating points with the `AbstractEnemy` steps, frame `+ delta` with seeded
  `delta` values; the HUD text compared only after the first level start (`CONTRACT.md` "Unreachable differences").
- 3d. Weapons attained (5 + 1 calls): `var weaponsAttained: OrderedSet<MegamanWeapon>? = null` on the game.
  `Megaman.kt:921`; `RobotMasterLevelOnCompletion.kt:10` (`!= null`); `GetWeaponScreen.kt:146`, `:153`;
  `MainMenuScreen.kt:161`; `MMG:786` (`resetPlayerProgress`). Suite: level defeated (new and old), get-weapon read
  and remove, save load and remove, `resetPlayerProgress`; "set" and the set object compared.
- New API and first caller: each field at its first call site above (`MLS:319`, `PlayerSpawnEventHandler.kt:134`,
  `MLS:512`, `Megaman.kt:921`).
- Check per sub-step: build green; `grep` for the keys of the group with `game.*Property(` gives 0 lines; the suite
  passes. After 3d: `grep -rnE "game\.(is|put|get|getOrDefault|remove|has)Property\(" core/src lwjgl3/src` gives 0
  lines outside comments.
- Done: yes (2026-10-28)

### 4. The game is no longer an `IPropertizable`; unused `ConstKeys` go

- Changes: `MMG` loses `IPropertizable`, `override val properties` and the unused imports. `C/Constants.kt` loses
  `COINS`, `LEVEL_POINTS`, `TIME_SINCE_LAST_FLOATING_POINTS`, `LAST_FLOATING_POINTS_TYPE`, `WEAPONS_ATTAINED`,
  `ROOM_TRANSITION`, `TILED_MAP_LOAD_RESULT`, `WORLD_CONTAINER`, `DRAWABLES`, `SHAPES`, `SYSTEMS`. The frozen copy in
  the tests uses its own string literals for these keys (copied values), so it does not need the constants.
- Check: build green; the `ConstKeys` `grep` of `CONTRACT.md` gives 0 lines.
- Done: yes (2026-10-28)

### 5. Root `CLAUDE.md`

- Changes: "Game object": add `levelState: LevelState` (session-lived level values; the level screen sets them,
  callers read them when used: current level, rooms, room transition, beam, coins, points, floating points) and
  `weaponsAttained` (not saved). Say that the game has no property bag.
- Check: the text matches the code.
- Done: yes (2026-10-28)

### 6. Contract checks before the play test

- Run all automated checks of `CONTRACT.md`: the build, the warnings, the `grep` checks, the temporary suite, the
  permanent tests, the 8 `getCurrentLevel()` stub tests, the two smoke launches
  (`./run-game-alpha.sh --level SMB3_1 --room room2`, `./run-game-alpha.sh --level WILY_STAGE_1 --room room8`,
  about 15 to 20 seconds each).
- Write the results in the process log. STOP and report to the owner with the 10 manual checks of `CONTRACT.md` as a
  list of commands.
- Done: yes (2026-10-28)

### 7. After the play test passes: delete the temporary suite

- Changes: delete `T/Temp016aFrozenGameBag.kt` and `T/Temp016aBagEquivalenceTest.kt` in their own commit.
- Check: build green (core test count drops by the tests of the temporary suite only); the permanent tests stay.
  Update the process log. STOP and report. No merge.
- Done: yes (2026-10-28)

## Frozen expressions

The frozen copy holds these expressions exactly as on `44c6b5fa3` (with string literals for the keys):

- `isProperty("room_transition", true)`; `putProperty("room_transition", b)`.
- `isProperty("Megaman_beam", true)`; `putProperty("Megaman_beam", b)`; `getProperty("Megaman_beam_center",
  Vector2::class)`; `putProperty("Megaman_beam_center", v)`.
- `getOrDefaultProperty("coin", 0, Int::class)`, `putProperty("coin", n)`, HUD `"COINS: " + getProperty("coin",
  Int::class).toString()`; the same for `"level_points"` and `"POINTS: "`.
- `getProperty("time_since_last_floating_points", Float::class)!!`, `putProperty(..., f)`;
  `getProperty("last_floating_points_type", FloatingPointsType::class)`, `putProperty(..., t)`; the
  `spawnFloatingPoints` steps of `AbstractEnemy.kt:309-330` on these values (the type and points logic, not the
  sound and life side effects).
- `putProperty("weapons_attained", set)`, `hasProperty("weapons_attained")`, `getProperty("weapons_attained") as
  OrderedSet<MegamanWeapon>?`, `removeProperty("weapons_attained")`.
- The 11 internal functions of `MMG:769-1052` that use the bag.

Test-only names: `Temp016aFrozenGameBag`, `Temp016aBagEquivalenceTest` (temporary, deleted in step 7);
`LevelStateTest` (permanent).

## Risks in the plan

- The `sed` in step 3a could also change a comment line (`ReactorMan_OLD.kt:354`). Exclude that file, and review the
  diff line by line.
- A relaxed `MegamanMaverickGame` mock in a test returns a relaxed `LevelState` mock for `levelState`. If a test
  fails because of this, stub it in the test, not in production code, and log it.
