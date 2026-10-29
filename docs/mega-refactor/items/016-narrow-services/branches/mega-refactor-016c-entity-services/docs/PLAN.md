# Plan: mega-refactor-016c-entity-services

- Status: approved
- Approved: 2026-10-29 (owner, "plan approved", with the enum move: "I approve moving the enum out.")

How to do the work in `SCOPE.md` (approved 2026-10-29) so that the checks in `CONTRACT.md` (approved 2026-10-29)
pass. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`, `T/` = `core/src/test/kotlin/com/megaman/maverick/game/`,
`MMG` = `C/MegamanMaverickGame.kt`, `MLS` = `C/screens/levels/MegaLevelScreen.kt`, `S/` =
`/tmp/claude-1000/-home-johnlavender-IdeaProjects-Megaman-Maverick/a600e326-cd21-4d95-a3bf-326f727a8247/scratchpad/016c/`
(scratch scripts; not committed).

Rules for every step:

- One commit per step. Each commit builds and passes all tests.
- The build command is `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test
  lwjgl3:jar`. "Build green" below means: it passes, engine 575 tests, core 249 plus the new tests, 0 failures, no new
  unique `^w:` line against the 81 of the baseline.
- Each changed site keeps its place in the code and its expression shape (a read stays a read at the same line, a
  write stays a write). No new reset, no reordering, except the creation order of step 3.
- No code comments. A script only for a repeated pattern (steps 2, 4 and 5), each with a check script in `S/` that
  reads `git diff -U0` and proves that every changed line is one of the listed rewrites; its output goes in the process
  log. Small or one-time edits with the Edit tool.
- No test map: the manual checks use real rooms (`SCOPE.md` open question 12).

Checked during planning (2026-10-29, by reading the code and the scan probes):

- A real `MegamanMaverickGame()` builds in a unit test with no LibGDX backend (`LevelStateTest`, `GameClockTest`), so
  the temporary suite and the permanent tests can use one.
- `setPerformance` calls `Gdx.graphics`, so no unit test calls it; tests write the performance value directly.
- 2 entity files import the nested enum `MegamanMaverickGame.Performance` (`FloatingCan.kt:44`, `Bat.kt:47`). Step 2
  moves the enum out of the game class (owner-approved scope addition, below), so the swap needs no exception.
- `EntityQueriesTest.kt:33` and `PlayerItemRuleTest.kt` define or build entities with the game type, so they are in
  the swap and the helper steps. `ContactRuleSelectionTest`, `MegaLevelScreenEventsTest`, `GameClockTest` and
  `LevelStateTest` keep the host.

## Scope addition (owner-approved 2026-10-29)

Owner (verbatim): "I approve moving the enum out." The nested enum `MegamanMaverickGame.Performance` becomes a
top-level `Performance` in `C/state/Performance.kt`, next to `GameSettings`, with the same entries and values. Its 5
user files change their import or qualifier: `Bat.kt`, `FloatingCan.kt`, `MMG`, `GameSettingsPanel.kt`,
`DesktopLauncher.kt`. The contract check "no code line in `entities/` names `MegamanMaverickGame`" stays strict, with
no exception. Done in step 2.

## Steps

### 1. Temporary forwards suite on the old code

- Changes (test sources only):
  - `T/Temp016cFrozenGameForwards.kt`: a copy of the old game functions from the base `9ba76c833`, over their old
    storage: a `LevelState` (`currentLevel`, the 2 suppliers, `worldContainer`, `tiledMapLoadResult`), 2 private
    flags, a `params` and a `weaponsAttained` field. Functions: `setCurrentLevel`, `getCurrentLevel`,
    `isInExtraLevel`, `setRoomsSupplier`, `getRooms`, `setCurrentRoomSupplier`, `getCurrentRoom`, `setWorldContainer`,
    `getWorldContainer`, `setTiledMapLoadResult`, `getTiledMapLoadResult`, `setCameraRotating`, `isCameraRotating`,
    `setFocusSnappedAway`, `isFocusSnappedAway`, `getPerformance`, `weaponsAttained`.
  - `T/Temp016cForwardsEquivalenceTest.kt`: 200 seeded sequences of write operations (set level, among them extra and
    non-extra levels; set or replace each supplier with a supplier over a changing room list; set the world container
    and the map load result; set each flag; write the performance value; set or clear `weaponsAttained`). Each
    operation runs on the frozen copy and on a real `MegamanMaverickGame()`. After each operation, each old read
    equals the new read (references by identity; `getRooms` into a new array, compared element by element by
    identity). Before the first level and map write, the old `getCurrentLevel()` and `getTiledMapLoadResult()` and
    the new reads throw the same exception type. On this step the new side calls the same game functions, so it
    passes trivially; step 2 moves the new side to the new homes.
- Check: build green; the suite passes.
- Done: yes (2026-10-29, `3a40b28f1`)

### 2. Real homes; the forwards leave the game (decision 11)

- Changes:
  - New `C/screens/levels/camera/GameCameraState.kt`: `class GameCameraState { var rotating = false; var
    focusSnappedAway = false }`. State only (owner note, `SCOPE.md`; item `051`).
  - New `C/state/Performance.kt`: the enum `Performance` moved out of `MMG` with the same entries and values
    (`VERY_LOW(30, 1f / 75f)`, `LOW(45, 1f / 112.5f)`, `MEDIUM(60, 1f / 150f)`, `HIGH(90, 1f / 225f)`). The users
    change only the import or the qualifier: `Bat.kt`, `FloatingCan.kt`, `MMG` (the self-import goes),
    `GameSettingsPanel.kt` (`MegamanMaverickGame.Performance.entries` → `Performance.entries`), `DesktopLauncher.kt`.
  - New `C/state/GameSettings.kt`: `class GameSettings(var performance: Performance)`.
  - `C/levels/LevelState.kt`: `var weaponsAttained: OrderedSet<MegamanWeapon>? = null`;
    `val currentRoom: RectangleMapObject? get() = currentRoomSupplier?.invoke()`;
    `fun getRooms(out)` (the body of the old game function); `val isInExtraLevel get() = currentLevel?.type ==
    LevelType.EXTRA_LEVEL`.
  - `MMG`: `val camera = GameCameraState()` and `val settings = GameSettings(params.performance)` next to
    `levelState` and `clock`. Remove the members listed in `SCOPE.md` item 1 and the fields `cameraRotating`,
    `focusSnappedAway`, `weaponsAttained`. `drawables` and `shapes` become `lateinit var ... private set` (public read);
    `getSystems()` goes (`getSystem` reads `systems`). Inside `MMG` the same rewrites as below (the camera callback in
    `create()`, `createGameEngine` (`settings.performance`, `levelState.worldContainer`), auto-perf,
    `setPerformance` (`settings.performance` read and write), `isPerformanceSubpar`, `consumeLaunchRoom`,
    `resetPlayerProgress` (`levelState.weaponsAttained = null`), `startLevel`, `postCreate`, `TOGGLE_PIXEL_PERFECT`).
  - Script `S/step2_rewrite.py` on `core/src` and `lwjgl3/src` (code lines only), patterns:
    - `game.getCurrentLevel()` → `game.levelState.currentLevel!!`
    - `game.setCurrentLevel(x)` → `game.levelState.currentLevel = x` (statement lines only)
    - `game.isInExtraLevel()` → `game.levelState.isInExtraLevel`
    - `game.getCurrentRoom()` → `game.levelState.currentRoom`
    - `game.getRooms(` → `game.levelState.getRooms(`
    - `game.setRoomsSupplier {` → `game.levelState.roomsSupplier = {`, the same for the current-room supplier
    - `game.getWorldContainer()` → `game.levelState.worldContainer`; `game.setWorldContainer(x)` →
      `game.levelState.worldContainer = x`
    - `game.getTiledMapLoadResult()` → `game.levelState.tiledMapLoadResult!!`; the setter as above
    - `game.isCameraRotating()` → `game.camera.rotating`; `game.setCameraRotating(x)` → `game.camera.rotating = x`
    - `game.isFocusSnappedAway()` → `game.camera.focusSnappedAway`; `game.setFocusSnappedAway(x)` →
      `game.camera.focusSnappedAway = x`
    - `game.getPerformance()` → `game.settings.performance`
    - `game.weaponsAttained` → `game.levelState.weaponsAttained`
    - `game.getDrawables()` → `game.drawables`; `game.getShapes()` → `game.shapes`
  - A setter call that is an expression (for example the last line of a lambda that returns a value) cannot become an
    assignment; the build shows each one, and it gets a hand edit with the same effect. Each hand edit is listed in the
    process log.
  - Tests: the stubs `every { game.getCurrentLevel() } returns LevelDefinition.TEST_1` become
    `every { game.levelState } returns LevelState().apply { currentLevel = LevelDefinition.TEST_1 }` (or a shared
    `LevelState` when the test already stubs one). `LevelStateTest`: the game-function cases become cases on the
    `LevelState` members with the same expectations (the throw of `currentLevel!!` and `tiledMapLoadResult!!` before a
    write; the empty values; the suppliers read at each read, not copied; the values stay when a new level is set),
    plus `weaponsAttained` starts `null`. The temporary suite: the new side reads and writes the new homes; the frozen
    side does not change.
  - Permanent tests: `T/screens/levels/camera/GameCameraStateTest.kt` (both flags start `false`; a game's `camera` is
    the same object at each read); `T/state/GameSettingsTest.kt` (a game built with `params.performance = LOW` has
    `settings.performance == LOW`; with the default params, the default value).
- Check: build green. The game has none of the removed members (the build proves that no caller is left). The check
  script `S/step2_check.py` passes: each changed line in `core` and `lwjgl3` outside `MMG`, the 3 new or changed
  holder classes and the tests is one of the patterns above or a listed hand edit. The temporary suite passes
  unchanged on the frozen side.
- New API and first caller: `Performance` (moved; `MMG` auto-perf, `Bat`), `GameCameraState` (`MMG.camera`; read by
  `AbstractProjectile`), `GameSettings`
  (`MMG.settings`; read by `SpiderWeb`), `LevelState.weaponsAttained` (`Megaman`), `LevelState.currentRoom`
  (`Death`), `LevelState.getRooms` (`SwitchGate`), `LevelState.isInExtraLevel` (`LevelPauseScreen`), `MMG.drawables`
  (`MLS`), `MMG.shapes` (`MLS`).
- Done: yes (2026-10-29, `b0469276d`)

### 3. Creation order in `postCreate`

- Changes: `MMG.postCreate`: `players = MegaPlayers()` and `gravity = WorldGravity(players)` move to right after
  `audioMan` and its volumes, before `MegaEntityFactory.init(this)`. `players.register(PlayerSlot(0), megaman)` and the
  `TODO` above it stay after `state.addListener(megaman)`. The old `gravity` line goes from after `register`.
- Check: build green. One smoke launch (`./run-game-alpha.sh --level INTRO_STAGE --room room1`, 15 to 20 seconds): no
  exception.
- Done: yes (2026-10-29, `66fd31000`)

### 4. `IMegaGameEntity` cleanup (decisions 6 and 15)

- Changes:
  - `IMegaGameEntity`: remove `val game`, `getGameCamera`, `playSoundNow`, `stopSoundNow` and the unused import; the
    interface keeps `IGameEntity`, `ITaggable`, `ITypable<EntityType>`. The extensions stay in the file with new
    receivers: `val MegaGameEntity.players`, `val MegaGameEntity.targetPlayer` (the `TODO` for `047` stays), and
    `fun <T> T.overlapsGameCamera() where T : MegaGameEntity, T : IBodyEntity = game.getGameCamera()...` (no cast).
  - `MegaGameEntity(val game: MegamanMaverickGame)` (no `override`). The defaults are not added here.
  - `FreezableEntityHandler<T>(private val entity: T, ...) ... where T : MegaGameEntity, T : IFreezableEntity`.
  - `LightSourceUtils`: `fun <T> sendLightSourceEvent(lightSource: T) where T : MegaGameEntity, T : ILightSource`;
    `sendLightSourceEvent(entity: MegaGameEntity, ...)`.
  - Script `S/step4_rewrite.py` on `core/src` (code lines only): a bare `playSoundNow(` → `game.audioMan.playSound(`
    (40), a bare `stopSoundNow(` → `game.audioMan.stopSound(` (2), a bare `getGameCamera()` →
    `game.getGameCamera()` (4), and `(entity as MegaGameEntity).getGameCamera()` →
    `(entity as MegaGameEntity).game.getGameCamera()` (2, `GameEntityUtils.kt:58`, `:64`).
- Check: build green. `grep -rnE "playSoundNow|stopSoundNow" core/src` finds 0 lines; `IMegaGameEntity` has no `game`
  and no default; `S/step4_check.py` passes (each changed line outside the 4 files above is one of the 4 rewrites).
- Done: yes (2026-10-29, `6fee3e683`)

### 5. `MegaServices` and the mechanical swap

- Changes:
  - New `C/MegaServices.kt`:

    ```kotlin
    class MegaServices(
        val engine: GameEngine,
        val eventsMan: EventsManager,
        val assMan: AssetManager,
        val audioMan: MegaAudioManager,
        val state: GameState,
        val players: MegaPlayers,
        val gravity: WorldGravity,
        val levelState: LevelState,
        val clock: GameClock,
        val camera: GameCameraState,
        val settings: GameSettings,
        val updatables: OrderedMap<String, Updatable>,
        val runQueue: Queue<() -> Unit>,
        val groups: GroupsSystem,
        private val gameCamera: RotatableCamera
    ) {
        fun getGameCamera() = gameCamera
    }
    ```

  - `MMG.postCreate`: after `gravity`, `val services = MegaServices(engine, eventsMan, assMan, audioMan, state,
    players, gravity, levelState, clock, camera, settings, updatables, runQueue, getSystem(GroupsSystem::class)!!,
    getGameCamera())`; `MegaEntityFactory.init(services)`, `EntityFactories.initialize(services)`,
    `Megaman(services)`.
  - `GameEntityUtils.kt:81`: `game.getSystem(GroupsSystem::class)!!` → `game.groups`.
  - Script `S/step5_swap.py`: in every file under `C/entities/`, in `C/utils/MegaUtilMethods.kt`, and in the 17 entity
    test files (`SCAN.md` section 8): on code lines (not comments), the import line
    `import com.megaman.maverick.game.MegamanMaverickGame` becomes `import com.megaman.maverick.game.MegaServices`,
    and every other `MegamanMaverickGame` token becomes `MegaServices`. Parameter names stay `game`. In the 17
    tests this makes `mockk<MegaServices>(relaxed = true)` with the same stubs (one commit only; step 6 replaces it).
- Check: build green. `S/step5_check.py` passes: each changed line is the old line with the token replaced, except
  `C/MegaServices.kt` (new), the listed `postCreate` lines in `MMG` and `GameEntityUtils.kt:81`. The temporary suite
  passes. Smoke launches: `./run-game-alpha.sh --level INTRO_STAGE --room room1` and `./run-game-alpha.sh --level
  MOON_MAN --room room6`.
- New API and first caller: `MegaServices` and each member (`SCOPE.md`, "New API").
- Done: yes (2026-10-29, `d07a433eb`)

### 6. Test helper, test migration, permanent tests

- Changes (test sources only):
  - `T/TestServices.kt`: `fun testServices(...)`, each argument with a default: real `GameState()`, `MegaPlayers()`,
    `WorldGravity(players)`, `EventsManager()`, `LevelState().apply { currentLevel = LevelDefinition.TEST_1 }`,
    `GameClock()`, `GameCameraState()`, `GameSettings(MEDIUM)`, `OrderedMap()`, `Queue()`, `GroupsSystem()`, a real
    `RotatableCamera()` (or a relaxed mock if it does not build in a unit test; the test log says which); relaxed mocks
    of `GameEngine`, `AssetManager`, `MegaAudioManager`.
  - The 17 tests use `testServices(...)` in place of the relaxed holder mock. A stub becomes an argument (for example
    `engine = engine`, `players = players`) or a write on a real object (for example `levelState.currentLevel`). The
    `assMan.get(...)` atlas stubs stay on the `AssetManager` mock. A read that the helper does not answer fails, and is
    fixed in the test only. No assert changes.
  - `T/entities/EntityConstructorsTest.kt`: walks the main class output of `com/megaman/maverick/game/entities` (the
    class path entry that holds `MegaGameEntity`, so test classes are not in it), loads each class with no
    initialization, keeps the concrete `MegaGameEntity` subclasses, skips `PushableBlock.InnerBlock` by name, and for
    each: the primary constructor exists, has exactly one required parameter, of type `MegaServices`, and the call
    with `testServices()` gives an instance. It also checks that it found more than 400 classes.
  - `T/entities/MegaEntityFactoryTest.kt`: `MegaEntityFactory.reset()`, `init(services)`, `fetch(Met::class)!!.game`
    is the same object as `services`; `reset()` at the end.
- Check: build green; the 17 tests pass with no assert change; the new tests pass.
- Done: yes (2026-10-29, `ebdd560d7`)

### 7. Docs

- Changes: root `CLAUDE.md`:
  - "Game object": `camera: GameCameraState` (the camera flags, state only until `051`), `settings: GameSettings` (the
    performance; `setPerformance` changes it), `levelState.weaponsAttained`, the `LevelState` reads (`currentLevel`,
    `currentRoom`, `getRooms`, `isInExtraLevel`); the game functions that went.
  - New lines: entities take `MegaServices` (`MegaServices.kt`), built once in `postCreate` from the game's session
    objects; screens keep the game.
  - "Entity base classes": `IMegaGameEntity` holds only the tag and the type; `players` and `targetPlayer` are
    extensions on `MegaGameEntity`; sound through `game.audioMan`, the camera through `game.getGameCamera()`.
  - "Factories": the primary constructor takes the holder.
  - "Build, run, test": the test helper `testServices()` in place of the relaxed game mock.
  - "Engine ECS", groups: `game.groups` for entities.
- Check: the text matches the code (read).
- Done: yes (2026-10-29, `2de63cb0d`)

### 8. Automated contract checks

- Run every automated check in `CONTRACT.md`. The 3 smoke launches. Write the results in
  `CONTRACT.md` and the process log.
- Then STOP for the owner's play test (the manual checks of `CONTRACT.md`).
- Done: yes (2026-10-29); the owner's play test is next

### 9. After the play test: delete the temporary suite

- Changes: delete `T/Temp016cForwardsEquivalenceTest.kt` and `T/Temp016cFrozenGameForwards.kt` in their own commit.
- Check: build green; `grep -rn "Temp016c" core/src` finds 0 lines; the automated checks again. Process log updated.
- At the merge (coordinator, in the parent's worktree): `.git-blame-ignore-revs` gets the full SHA of the `016c`
  squash commit in a separate commit.
- Done: yes (2026-10-29, `22338b9e5`); the `.git-blame-ignore-revs` entry is for the merge
