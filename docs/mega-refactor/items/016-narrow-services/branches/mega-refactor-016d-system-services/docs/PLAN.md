# Plan: mega-refactor-016d-system-services

- Status: approved
- Approved: 2026-10-29 (owner, verbatim: "plan approved"; with the step 5 proof through an uncommitted test adapter)

How to do the work in `SCOPE.md` (approved 2026-10-29) so that the checks in `CONTRACT.md` (approved 2026-10-29)
pass. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`,
`MMG` = `C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`, `S/` =
`/tmp/claude-1000/-home-johnlavender-IdeaProjects-Megaman-Maverick/a600e326-cd21-4d95-a3bf-326f727a8247/scratchpad/016d/`
(scratch files; not committed).

Rules for every step:

- One commit per step. Each commit builds and passes all tests.
- The build command is `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
  lwjgl3:jar`. "Build green" below means: it passes, engine 575 tests, core 259 plus the new tests, 0 failures, no new
  unique `^w:` line against the 81 of the baseline.
- Small edits with the Edit tool; no script (no repeated pattern in this branch). No code comments; the 2 existing
  `TODO` comments in `MegaContactRules.kt` stay.
- Each changed read keeps its place and its expression shape (only its receiver changes). No reordering, except the
  creation of `players` and `gravity` in step 2.
- No temporary old-versus-new suite (`SCOPE.md` open question 5).
- Smoke launches: 15 to 20 seconds each, at most.

Checked during planning (2026-10-29, by reading the code):

- A real `MegamanMaverickGame()` builds in a unit test with no LibGDX backend (`GameCameraStateTest`,
  `LevelStateTest`), so the game field test can use one.
- `MegaPathfinderFactory.getPathfinder` reads `Gdx.graphics.frameId`; its test sets a mocked `Gdx.graphics` and
  restores the old value after each case.
- `FeetSandRule` spawns the splash through `EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SPLASH)`;
  its test replaces the `EntityFactories` object with `mockkObject` and unmocks it after each case.
- `WorldPathfinder` keeps its world size private; the factory test observes it through the result of the pathfinder
  (a target outside the old map and inside the new one, with out-of-bounds moves not allowed).
- The 10 constant `screenOnCompletion` lambdas (`{ ScreenEnum.X }`) compile with any one parameter, so they do not
  change.
- `Test1.tmx` `room_7` has a working setup of 3 `GravityChange` areas (`direction` `up`, `down`, `up`; full room
  height, 6 tiles wide each) with a `Met`. The new room copies this pattern.

## New API and first production caller

| API | Step | First production caller |
|---|---|---|
| `REFACTOR_TEST_016` (`LevelDefinition` entry, `TEST_LEVEL`) and `assets/tiled_maps/tmx/RefactorTest016.tmx` | 1 | the launcher `--level` option |
| `MegamanMaverickGame.players`, `gravity` as `val` fields built with the game (were `lateinit var`) | 2 | `postCreate` (`MegaServices`, `players.register`), then `createGameEngine` (step 3) |
| `megaContactRules(players: MegaPlayers, gravity: WorldGravity, debugFilter)` (changed signature) | 3 | `MMG.createGameEngine` |
| `MegaPathfinderFactory(levelState: LevelState)` (changed constructor) | 4 | `MMG.createGameEngine` (async and sync) |
| `LevelDefinition.screenOnCompletion: (LevelState) -> ScreenEnum`, `RobotMasterLevelOnCompletion : (LevelState) -> ScreenEnum`, `MLS.screenOnCompletion` (changed types) | 5 | `MLS.onEvent` (`END_LEVEL`) |

No other new type, function or field.

## Steps

### 1. Test map `RefactorTest016.tmx` and the level entry

- Changes:
  - A subagent with the `level-builder` skill builds `assets/tiled_maps/tmx/RefactorTest016.tmx` with one room
    `gravity_change` (a `game_rooms` object named `gravity_change`), as rough as the skill allows. Brief for the
    subagent:
    - One screen high (14 tiles), about 48 tiles wide. Floor and ceiling blocks over the full width, walls at both
      ends, and a `tiles` tile layer with a tile on each block cell (as `Test1.tmx` and `RefactorTest029.tmx`).
    - A player spawn (`player` layer) near the left end, on the floor.
    - Player areas (`specials` layer, objects named `GravityChange`, full room height, about 6 tiles wide each, next to
      each other): area A from about tile 10 with `direction` = `down`, then area B with `direction` = `up` (as
      `Test1.tmx` `room_7`, 6784/6984). Walking right turns the gravity down in A and back up in B.
    - A `Met` area at the right end, with free floor between it and area B: one `GravityChange` with `direction` =
      `down` (full room height, about 6 tiles wide), and a `Met` (`enemies` layer) on the floor inside it.
    - Copy the layer names, the map properties and the tile set from `RefactorTest029.tmx`.
  - `C/levels/LevelDefinition.kt`: a new entry `REFACTOR_TEST_016` after `REFACTOR_TEST_031`, the same shape as
    `REFACTOR_TEST_029` (`type = LevelType.TEST_LEVEL`, `tmxMapSource = "RefactorTest016.tmx"`, `music =
    MusicAsset.MM3_SNAKE_MAN_MUSIC`, `screenOnCompletion = { ScreenEnum.SAVE_GAME_SCREEN }`), with no
    `levelOverride` (no weapon is needed).
  - The process log names the subagent's output and any hand fix.
- Check: build green (core 259). Baseline smoke launch on the code before the other steps:
  `./run-game-alpha.sh --level REFACTOR_TEST_016 --room gravity_change` loads the room with no exception in the log.
  A screenshot or a log line is not needed; the owner sees the room in the play test.
- Done: yes

### 2. `players` and `gravity` built with the game object

- Changes:
  - `MMG`: `lateinit var players: MegaPlayers` and `lateinit var gravity: WorldGravity` become
    `val players = MegaPlayers()` and `val gravity = WorldGravity(players)`, in the same place in the class (after
    `state`). The 2 lines `players = MegaPlayers()` and `gravity = WorldGravity(players)` leave `postCreate()`.
    Nothing else in `postCreate()` changes.
  - New permanent test `T/players/GamePlayersFieldsTest.kt` (in the style of `GameCameraStateTest`): a new
    `MegamanMaverickGame()` with no `create()` has `players` and `gravity`; each read gives the same object;
    `gravity.direction` is `UP`; after `game.players.register(PlayerSlot(0), entity)` with an `IDirectional` test
    entity (as in `MegaPlayersTest`), `game.gravity.set(Direction.DOWN)` gives the entity `DOWN`.
- Check: build green (core 260). Smoke launch `./run-game-alpha.sh --level DESERT_MAN --room room1` with no exception.
- Done: yes

### 3. Contact rules take `players` and `gravity`

- Changes:
  - `C/world/contacts/MegaContactRules.kt`: `fun megaContactRules(players: MegaPlayers, gravity: WorldGravity,
    debugFilter: (IFixture, IFixture) -> Boolean)`; `:40` `FeetSandRule { players.any { it.inPlay } }`; `:56`
    `if (entity.asPlayer != null) gravity.set(direction) else entity.direction = direction`. The
    `MegamanMaverickGame` import goes; imports of `MegaPlayers` and `WorldGravity` come in.
  - `MMG:926`: `megaContactRules(players, gravity, CONTACT_LISTENER_DEBUG_FILTER)`.
  - `T/world/contacts/ContactRuleSelectionTest.kt:54`: `val players = MegaPlayers()`;
    `megaContactRules(players, WorldGravity(players)) { _, _ -> false }`; the `MegamanMaverickGame` and (if unused)
    `mockk` imports go.
  - `T/entities/special/CartV2ItemTest.kt`: in `beforeEach`, `val players = MegaPlayers()` and
    `val gravity = WorldGravity(players)` before the `WorldSystem`; `megaContactRules(players, gravity) { _, _ -> false }`;
    `testServices(engine = engine, players = players, gravity = gravity)`. No assert changes. The `MegamanMaverickGame`
    import goes.
  - New permanent test `T/world/contacts/MegaContactRulesTest.kt`. It builds the table with real `MegaPlayers` and
    `WorldGravity`, gets each rule with `table.select(ProcessState.BEGIN, a, b)`, and calls the rule with the selected
    roles (as `ContactRuleSelectionTest` does with real `Body` and `Fixture` objects):
    - Sand, `BEGIN`, no player registered: no call to `EntityFactories.fetch` (`mockkObject(EntityFactories)`), and the
      feet body senses `FEET_ON_SAND`.
    - Sand, `BEGIN`, a player registered and in play: `EntityFactories.fetch(EntityType.DECORATION,
      DecorationsFactory.SPLASH)` is called once, and the returned splash gets one `spawn` with the bottom center of
      the feet fixture, `SplashType.SAND` and the sound flag.
    - Sand, `BEGIN`, a player registered but not in play: no splash.
    - Gravity change, a player entity in a `direction` area: `gravity.direction` and the player's direction become the
      area's direction.
    - Gravity change, a non-player `IDirectional` entity: its direction becomes the area's direction;
      `gravity.direction` stays `UP` and a registered player keeps `UP`.
    The test entities are small `MegaGameEntity` or engine `GameEntity` subclasses in the test file, built with
    `testServices()` where a `MegaGameEntity` is needed; "in play" uses the same `inPlay` read as production.
- Check: build green (core 260 plus the new cases); `git diff --stat` shows no change to
  `core/src/test/resources/contact-rule-selection.txt`; `grep -n MegamanMaverickGame` finds nothing in
  `MegaContactRules.kt`, `ContactRuleSelectionTest.kt`, `CartV2ItemTest.kt`. Smoke launch
  `./run-game-alpha.sh --level DESERT_MAN --room room1` and `./run-game-alpha.sh --level REFACTOR_TEST_016 --room
  gravity_change` with no exception.
- Done: yes

### 4. Pathfinder factory takes `levelState`

- Changes:
  - `C/pathfinding/MegaPathfinderFactory.kt`: `class MegaPathfinderFactory(private val levelState: LevelState)`;
    `:26`, `:28` `levelState.worldContainer`; `:42` `levelState.tiledMapLoadResult!!`. The `MegamanMaverickGame`
    import goes; the `LevelState` import comes in.
  - `MMG:945`, `:951`: `MegaPathfinderFactory(levelState)`.
  - New permanent test `T/pathfinding/MegaPathfinderFactoryTest.kt` (a mocked `Gdx.graphics` with a settable
    `frameId`; restored after each case):
    - A factory built with an empty `LevelState`; then the world container and a map load result are set; the next
      `getPathfinder` call gives a pathfinder that uses them: the filter gets a non-null container (the snapshot), and
      a target inside the map is found with out-of-bounds moves not allowed.
    - After a new map load result with a smaller world (as at a new level start), a target outside the new world is
      not reached (out of bounds), in the same factory object.
    - After a new world container object in the same frame, the filter gets a new snapshot (the `source !==` rule
      stays).
- Check: build green; `grep -n MegamanMaverickGame` finds nothing in `C/pathfinding/`. Smoke launches
  `./run-game-alpha.sh --level DESERT_MAN --room room10` and `./run-game-alpha.sh --pathfinding sync --level DESERT_MAN
  --room room10` with no exception.
- Done: yes

### 5. `screenOnCompletion` takes `LevelState`

- Changes:
  - `C/levels/LevelDefinition.kt:22`: `val screenOnCompletion: (LevelState) -> ScreenEnum`. The 9 robot master entries
    (`:32` to `:96`): `screenOnCompletion = RobotMasterLevelOnCompletion`. The 10 constant entries and the new
    `REFACTOR_TEST_016` entry do not change. The `MegamanMaverickGame` import goes.
  - `C/levels/RobotMasterLevelOnCompletion.kt`: `object RobotMasterLevelOnCompletion : (LevelState) -> ScreenEnum`;
    `override fun invoke(levelState: LevelState) = if (levelState.weaponsAttained != null)
    ScreenEnum.GET_WEAPONS_SCREEN else ScreenEnum.SAVE_GAME_SCREEN`. The `MegamanMaverickGame` import goes.
  - `MLS:179`: `lateinit var screenOnCompletion: (LevelState) -> ScreenEnum`; `MLS:878`:
    `screenOnCompletion.invoke(game.levelState)`. `MMG:1013` does not change.
  - New permanent test `T/levels/LevelCompletionScreensTest.kt`: a map from each `LevelDefinition` entry to its
    expected screen with `weaponsAttained` `null` and with a non-empty `OrderedSet` (values in `CONTRACT.md`,
    "Automated checks", 20 entries with `REFACTOR_TEST_016`). It checks that the map has every entry of
    `LevelDefinition.entries`, and for each entry calls `screenOnCompletion` with a fresh `LevelState()` in both cases.
- Check: build green; `grep -rn MegamanMaverickGame core/src/main/kotlin/com/megaman/maverick/game/levels/` finds
  nothing. Before the change, run the new test once on the old code with a small adapter in the test (call the old
  function with a relaxed game mock whose `levelState` is the test `LevelState`), and record in the process log that it
  passed; then remove the adapter in the same step, so only the new form is committed. This shows that the table is
  the current behavior.
- Done: yes

### 6. Root `CLAUDE.md`

- Changes: "Game object": the `players`/`gravity` line says they are built with the game object; the `MegaServices`
  line drops "after `players` and `gravity`" and its last sentence becomes "Screens, layer builders, backgrounds,
  `ScreenController` and the launcher keep the game; the contact rules take `players` and `gravity`, and the pathfinder
  factory takes `levelState`." "Levels and Tiled pipeline": `screenOnCompletion` takes the `LevelState`. "Engine ECS"
  (contact rules line): no change, except the test name list if it names the new test. "Test maps" are not listed in
  root `CLAUDE.md`, so no line for `REFACTOR_TEST_016`.
- Check: build green (docs only, so the build is the same as step 5); the edited lines read as true against the code.
- Done: yes

### 7. Contract checks and report (gate 4)

- Changes: none in code. `CONTRACT.md` gets a "Results" paragraph; the process log gets the step entries.
- Check: every automated check in `CONTRACT.md`:
  - clean build, test counts, warnings;
  - `grep -rn MegamanMaverickGame` over `core/src/main` and `lwjgl3/src/main` (code lines only): only `MMG`, the 32
    screen files, the 13 backgrounds, `ScreenController`, the launcher, and the 5 commented-out entity files;
  - the `MMG` field and `postCreate()` order check by reading the diff;
  - no test mocks the host for the contact rules; the reference file has no diff;
  - the 4 smoke launches of the contract.
  Then STOP and report to the coordinator with the results and the manual checks from `CONTRACT.md` (with the `cd`
  line).
- Done: yes

### 8. After the play test

- Changes: no temporary suite to delete. The process log gets the owner's result, quoted.
- Check: the automated checks again (clean build, counts, warnings). STOP and report.
- Done: yes
