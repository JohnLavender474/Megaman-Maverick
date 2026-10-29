# Scope: mega-refactor-016c-entity-services

- Status: approved
- Approved: 2026-10-29 (owner, "Approved", with changes to open questions 1, 2 and 11; `016` decisions 13-15)
- Merge target: the parent branch `mega-refactor-016-master` (owner decision 2026-10-28), not `master`.

## Goal

Entities take one typed holder, `MegaServices`, in place of `MegamanMaverickGame`. The factories pass it. A test helper
builds a real holder. `game` and the 3 default methods leave `IMegaGameEntity` (the defaults are removed, decision 15). The `016a` forwards on the game go,
and every caller reads the real home (decision 11). A test builds every pooled class through its primary constructor.
No gameplay change.

## Item

From `016` `ITEM.md`, "Proposed scope", branch 3: "`016c` `entity-services`: the type swap in about 460 files, the
factory, a test helper, and the `IMegaGameEntity` cleanup (`game`, `getGameCamera`, `playSoundNow`, `stopSoundNow` and
the extensions leave the interface). A test builds every pooled class through its primary constructor. No other code
branch active."

Also from "Proposed scope": the holder fields `engine`, `events`, `assets`, `audio`, `profile` (`GameState`),
`players`, `gravity`, `camera` (camera plus the rotating and focus-snapped flags), `level` (a session-lived
`LevelState`), `clock`, `scheduler` (`updatables` plus `runQueue`), `groups` (`GroupsSystem`); "A field only with a
caller."; "Pooled entities outlive a level, so the holder and every field live for the session. Values that change at
run time (performance, `paused`, the current level and room) are read when used, not copied."; "`postCreate` builds
the holder first; `MegaPlayers()` and `WorldGravity` move earlier."

Owner decisions that apply (`ITEM.md`, "Decisions"):

- 1: "One holder, not one service per constructor parameter."
- 2: "Name: `MegaServices`."
- 5: "Keep the name `game` and the old member names during the swap; rename later (`016e`)."
- 6 (changed by 15 for the default methods): "`getGameCamera`, `playSoundNow` and `stopSoundNow` leave
  `IMegaGameEntity` in `016c` (they read `game` through the interface); they move to `MegaGameEntity`, so no call site
  changes."
- 8: "Screens keep the host."
- 10: "`WEAPONS_ATTAINED` becomes a typed field that is not saved, outside `GameState` (the saved profile)."
- 11: "No forwarding members. Callers read the real home (for example `game.clock.paused`,
  `game.players.selectButtonAction`). The forwards that `016a` kept under decision 4 (`getCurrentLevel()`,
  `getRooms()`, `isCameraRotating()` and the other functions that now read `levelState` or private fields) are
  replaced by direct reads in `016c`, when the holder gets its real fields."
- 13 (2026-10-29, from this scope): "The camera flags (`cameraRotating`, `focusSnappedAway`) do not go into
  `LevelState`. They describe the camera. [...] So a small camera state or manager object next to the camera (not fields
  of the camera class) holds them; the holder field `camera` reaches it."
- 14 (2026-10-29): "Performance is a setting, not time: a small settings object on the holder holds the performance
  value (`setPerformance` stays as an action; read when used). Not `GameClock`."
- 15 (2026-10-29): "Decision 6 is replaced for the default methods: `getGameCamera`, `playSoundNow` and `stopSoundNow`
  are removed from `IMegaGameEntity` and not moved to `MegaGameEntity`; their callers read the real home (decision
  11)."

Not in this branch: contact rules, the pathfinder factory and other systems (`016d`); renames of `game` and the member
names (`016e`); deleting `IMegaGameEntity` (`072`); targeting (`047`).

## Current state

Full scan: `docs/SCAN.md`. Base `9ba76c833`.

- 451 entity files name `MegamanMaverickGame` in code (461 lines; 454 of them one-line constructor headers). Plus
  `MegaUtilMethods.delayRun`, which only entities call.
- Entities read 21 game members. 13 are objects that the holder can hold with the same name. The others need a home
  first, because decision 11 forbids forwards: the `016a` forwards (`getCurrentLevel()` 16, `getCurrentRoom()` 12,
  `isCameraRotating()` 39, `setFocusSnappedAway()` 7, `getTiledMapLoadResult()` 2, `getRooms()` 1),
  `getPerformance()` 8 (a value of `params`), `getSystem(GroupsSystem::class)` 1, and the `weaponsAttained` write 1.
- The camera flags and `weaponsAttained` are fields of the game; the performance is a field of `params`. None is in
  an object that the holder can share with the game and the screens.
- `postCreate` builds `players` and `gravity` after the entity pools and `Megaman`; the holder needs them first. Moving
  them up has no side effect.
- The `IMegaGameEntity` cleanup compiles with 2 generic bounds and no call site change (probe, `SCAN.md` section 6).
- 436 entity classes build by their one-argument primary constructor; only `PushableBlock.InnerBlock` (private, not
  pooled) has 2 parameters (probe, `SCAN.md` section 7).
- 17 test files build entities with a relaxed game mock.

## In scope

The work, in the order that keeps each commit building (`PLAN.md` gives the steps):

1. Real homes (decision 11), while entities still take the game:
   - New class `GameCameraState` (`C/screens/levels/camera/GameCameraState.kt`, next to `RotatableCamera`) with
     `var rotating = false` and `var focusSnappedAway = false` (decision 13). The game gets
     `val camera = GameCameraState()`, built with the game object (like `levelState` and `clock`). Nothing else goes
     in it now: the camera reference stays with the viewport and the holder (open question 5), and no other camera
     value has an entity caller.
     Note (owner, 2026-10-29, "yes"; not a scope change): the owner wants the camera to become "a raw camera with
     components attached to it that affect is behavior and rules" (item `051`, recorded on `master` in items `051` and
     `058`). So `GameCameraState` is the read side for entities. Until `051` it holds state only (the two flags); no
     camera behavior or logic moves into it.
   - New class `GameSettings` (`C/state/GameSettings.kt`, next to `GameClock`) with `var performance`, start value
     `params.performance` (decision 14). The game gets `val settings = GameSettings(params.performance)`. Nothing else
     goes in it now: `vsync` and `pixelPerfect` have no entity caller, and `selectButtonAction` is in `MegaPlayers`
     (`016b`).
   - `LevelState` gets `weaponsAttained` (open question 3), and the derived reads `currentRoom`, `getRooms(out)` and
     `isInExtraLevel` (open question 4).
   - The game loses `getCurrentLevel`, `setCurrentLevel`, `isInExtraLevel`, `getRooms`, `setRoomsSupplier`,
     `getCurrentRoom`, `setCurrentRoomSupplier`, `getWorldContainer`, `setWorldContainer`, `getTiledMapLoadResult`,
     `setTiledMapLoadResult`, `setCameraRotating`, `isCameraRotating`, `setFocusSnappedAway`, `isFocusSnappedAway`,
     `getPerformance`, the `cameraRotating`, `focusSnappedAway` and `weaponsAttained` fields, and (open question 8)
     `getDrawables`, `getShapes`, `getSystems`.
   - Every caller (entities, screens, `MMG`, `MegaPathfinderFactory`, `IBodyExtensions`, the launcher if needed,
     tests) reads or writes the home: for example `game.levelState.currentLevel!!`, `game.camera.rotating`,
     `game.camera.focusSnappedAway = false`, `game.settings.performance.fixedStep`. A pattern edit with a check
     script that proves each changed line is one of the listed rewrites.
   - `setPerformance` stays as an action (it also sets the FPS, the world fixed step, the event and the timers); it
     writes `settings.performance`. `isPerformanceSubpar`, `resetPlayerProgress`, `consumeLaunchRoom`, `startLevel` and
     the other actions stay.
2. Creation order: `players = MegaPlayers()` and `gravity = WorldGravity(players)` move before
   `MegaEntityFactory.init`. `players.register(...)` stays after `Megaman` is built and initialized.
3. `IMegaGameEntity` cleanup, while the type is still the game (decisions 6 and 15):
   - `val game` and the 3 defaults leave the interface; `MegaGameEntity(val game: ...)` keeps `game`. The defaults are
     not moved. Their callers read the real home: `playSoundNow(k, l)` becomes `game.audioMan.playSound(k, l)` (40),
     `stopSoundNow(k)` becomes `game.audioMan.stopSound(k)` (2), a bare `getGameCamera()` becomes
     `game.getGameCamera()` (4, plus 2 after a cast in `GameEntityUtils.kt:58`, `:64`). A pattern edit with a check
     script.
   - Receivers: `MegaGameEntity.players`, `MegaGameEntity.targetPlayer`, and
     `fun <T> T.overlapsGameCamera() where T : MegaGameEntity, T : IBodyEntity` (the hidden cast goes).
   - `FreezableEntityHandler<T>` with `where T : MegaGameEntity, T : IFreezableEntity`;
     `LightSourceUtils.sendLightSourceEvent` with a `MegaGameEntity` entity and a bound for the `ILightSource`
     overload. No call site changes.
   - `IMegaGameEntity` keeps `IGameEntity`, `ITaggable`, `ITypable<EntityType>` for `072`.
4. The holder and the mechanical swap (one commit):
   - New class `MegaServices` (open question 6 for the package) with these members, each the same session object as
     the game's: `engine`, `eventsMan`, `assMan`, `audioMan`, `state`, `players`, `gravity`, `levelState`, `clock`,
     `camera` (`GameCameraState`), `settings` (`GameSettings`), `updatables`, `runQueue`, `groups` (`GroupsSystem`),
     and the game camera read `getGameCamera()` over its own private field (open question 5). No function other
     than the camera read; no host function (screen, save, launch, settings).
   - `postCreate` builds it after `audioMan`, `players` and `gravity`, before `MegaEntityFactory.init`, as a local
     value (open question 7), and passes it to `MegaEntityFactory.init`, `EntityFactories.initialize` and `Megaman`.
   - The token `MegamanMaverickGame` becomes `MegaServices` in every entity file, `MegaEntityFactory`,
     `EntityFactories` and its 9 factories, `GameEntityUtils` (2 parameters), `MegaUtilMethods.delayRun`, and the 17
     tests (a relaxed holder mock for one commit). Parameter names stay `game` (decision 5). Comment lines do not
     change.
   - Lines that are not a token change, listed for the check script: `MegaServices.kt` (new), the `postCreate` lines,
     `GameEntityUtils.kt:81` (`game.getSystem(GroupsSystem::class)!!` becomes `game.groups`).
   - A check script proves that every other changed line is the token change only (open question 9 for the blame
     file).
5. Tests:
   - A test helper (test sources) builds a real `MegaServices`: real `GameState`, `MegaPlayers`, `WorldGravity`,
     `EventsManager`, `LevelState` (current level `TEST_1`, as the stubs give today), `GameClock`, `GameCameraState`, `GameSettings`, `OrderedMap`,
     `Queue`, `GroupsSystem`, `RotatableCamera` if it builds in a unit test; relaxed mocks for `GameEngine`,
     `AssetManager` and `MegaAudioManager`. Each argument can be replaced.
   - The 17 tests move from the relaxed holder mock to the helper. A read that the helper does not answer fails, and is
     fixed in the test, not in production code.
   - Permanent `EntityConstructorsTest`: every concrete `MegaGameEntity` class in the main output of the `entities`
     package has a primary constructor with one required parameter of type `MegaServices`, and the call with a helper
     holder succeeds. `PushableBlock.InnerBlock` is the one named exception.
   - Permanent `MegaEntityFactoryTest`: `init(services)` then `fetch(X::class)` gives an entity whose `game` is the same
     holder (identity).
   - `LevelStateTest`: the tests of the removed game functions become tests of the `LevelState` members (same
     expectations); a new case for the `weaponsAttained` start value.
   - `GameCameraStateTest`: both flags start `false`; a game's `camera` is one object for the session.
   - `GameSettingsTest`: a game's `settings.performance` starts as `params.performance`.
   - Temporary suite (open question 10).
6. Docs: root `CLAUDE.md` ("Game object", "Entity base classes", "Factories", "Build, run, test" test line): entities
   take `MegaServices`; the factory passes it; `camera` (`GameCameraState`), `settings` (`GameSettings`) and
   `levelState.weaponsAttained`; the test helper in place of the relaxed game mock.

New API and first production caller:

| API | First production caller |
|---|---|
| `MegaServices` (class) | `postCreate` builds it; `MegaGameEntity(game: MegaServices)` |
| `MegaServices.engine` | `MegaGameEntity` (`GameEntity(game.engine)`) |
| `MegaServices.eventsMan` | `AbstractBoss.onSpawn` (`addListener`) |
| `MegaServices.assMan` | `AbstractEnemy` and every `init()` atlas read |
| `MegaServices.audioMan` | every former `playSoundNow` caller (for example `Met`) |
| `MegaServices.state` | `MegaGameEntity.canSpawn` |
| `MegaServices.players` | the `MegaGameEntity.players` extension |
| `MegaServices.gravity` | `GravitySwitcharoo` |
| `MegaServices.levelState` | `MegaGameEntity.canSpawn` |
| `MegaServices.clock` | `Starkner` (`paused`) |
| `MegaServices.camera` | `AbstractProjectile` (`camera.rotating`) |
| `MegaServices.settings` | `SpiderWeb` (`settings.performance`) |
| `MegaServices.updatables`, `runQueue` | `MegaUtilMethods.delayRun`, `delayNextPossibleSpawn` |
| `MegaServices.groups` | `getUpdateWhenGroupNearCameraPredicate` |
| `MegaServices.getGameCamera()` | `overlapsGameCamera`, `AbstractEnemy` |
| `GameCameraState` (class), `MegamanMaverickGame.camera` | `MLS`, the camera's `onJustFinishedRotating` in `MMG.create` |
| `GameCameraState.rotating` | `AbstractProjectile`, `IBodyExtensions.defaultDoUpdate`, `MLS` (writes) |
| `GameCameraState.focusSnappedAway` | `Megaman`, `PipePortal`, `MLS` |
| `GameSettings` (class), `MegamanMaverickGame.settings` | `MMG` auto-perf, `GameSettingsPanel` |
| `GameSettings.performance` | `SpiderWeb`, `MMG.createGameEngine`, `MMG.setPerformance` |
| `LevelState.weaponsAttained` | `Megaman` (write), `GetWeaponScreen`, `RobotMasterLevelOnCompletion` |
| `LevelState.currentRoom`, `getRooms`, `isInExtraLevel` | `Death`, `SwitchGate`, `LevelPauseScreen` |

Removed API: the game members listed in item 1; `IMegaGameEntity.game` and its 3 defaults (`getGameCamera`,
`playSoundNow`, `stopSoundNow`; not moved, decision 15); the
`IMegaGameEntity`-typed receivers of `players` and `targetPlayer`; the `IBodyEntity` receiver of `overlapsGameCamera`.

Scope addition (owner, 2026-10-29, verbatim: "I approve moving the enum out."): the nested enum
`MegamanMaverickGame.Performance` becomes a top-level `Performance` next to `GameSettings` (`C/state/`), with the same
entries and values. Its 5 user files change the import or the qualifier (`Bat.kt`, `FloatingCan.kt`, `MMG`,
`GameSettingsPanel.kt`, `DesktopLauncher.kt`). Then no code line in `entities/` names `MegamanMaverickGame`.

## Out of scope

- `megaContactRules(this)` and `MegaPathfinderFactory(this)` in `create()` keep the game (`016d`). They only get the
  forward rewrites of item 1.
- Screens, screen handlers, layer builders, backgrounds, `ScreenController`, `RobotMasterLevelOnCompletion` and the
  launcher keep the host (decision 8); they only get the item 1 rewrites.
- Renames: `game` to `services`, `assMan` to `assets`, and so on (`016e`).
- Deleting `IMegaGameEntity` (`072`); moving `targetPlayer` (`047`).
- Random (`011`), the texture cache (`028`).
- Commented-out code that names the game (5 files): not changed.

## Affected areas and risks

- Files: about 457 main files (451 entity files, `MegaServices.kt`, `GameCameraState.kt`, `GameSettings.kt`, `MMG`,
  `MegaUtilMethods`, `LevelState`, `IBodyExtensions`, `LightSourceUtils`, `MegaPathfinderFactory`) and about 15 screen files for the
  item 1 rewrites; about 20 test files; root `CLAUDE.md`.
- Risk: a holder field that is a copy, not the same object, or a value copied at build time. Mitigation: every field is
  a session object built before the holder; run-time values (`paused`, `performance`, level values, camera flags) stay
  inside those objects and are read when used. The factory test checks identity of the holder.
- Risk: a pooled class keeps a wrong constructor type, and fails only at its first fetch (an
  `IllegalArgumentException` from `call`). Mitigation: `EntityConstructorsTest`.
- Risk: creation order. `players` and `gravity` exist earlier; no code reads them in that span today (a read would have
  thrown). Mitigation: a smoke launch and the play test.
- Risk: the performance home. The start value must be `params.performance`; the auto-perf and the settings panel must
  change the value that entities and the world system read. Mitigation: `GameSettingsTest`, the check script, and a
  play check of a performance change.
- Risk: a removed default whose caller has a different `game` in scope (for example a nested class with its own
  `game`). Mitigation: the compiler (each caller is inside a `MegaGameEntity` subclass today; no caller is qualified),
  and the check script for the rewrite.
- Risk: a missed forward caller. Mitigation: the game members are removed, so the compiler finds every caller in
  `core`, `lwjgl3` and the tests.
- Risk: the swap commit hides a non-type edit. Mitigation: the check script and the short list of allowed lines.
- Risk: test churn. The real holder can show hidden reads in tests; each needs a test fix.
- Risk: a large merge conflict with any other branch. Mitigation: no other code branch is active (`SCAN.md` section
  10); the coordinator keeps it so until the merge.

## Overlap with other branches

No other code branch is active. `016d` builds on this branch (contact rules and the pathfinder factory take the
holder). `072` can start after this branch: `IMegaGameEntity` then holds only `IGameEntity`, `ITaggable` and
`ITypable<EntityType>`.

## Open questions

None. Resolved by the owner on 2026-10-29 ("Approved"). Questions 3 to 10 and 12 are accepted as recommended;
questions 1, 2 and 11 are changed by the owner (`016` decisions 13-15).

1. Home of the camera flags. Recommended: fields of `LevelState`. Changed by the owner (verbatim): "I agree with your
   assertion. I think it's worth keeping in mind though that it might be best for a \"camera
   service/component/helper/manager\" to handle things like these fields instead of directly tying them to the camera
   itself." Result: a new class `GameCameraState` next to `RotatableCamera`, with `rotating` and `focusSnappedAway`;
   the game and the holder field `camera` hold it. No other member now (no other camera value has an entity caller).
2. Home of the performance value. Recommended: `GameClock.performance`. Changed by the owner ("Agreed" to a small
   settings object on the holder): a new class `GameSettings` with `performance`; the game and the holder field
   `settings` hold it; `setPerformance` stays as an action; read when used.
3. Home of `weaponsAttained`: `LevelState.weaponsAttained`. Accepted.
4. The derived level reads: members of `LevelState` (`currentRoom`, `getRooms(out)`, `isInExtraLevel`); the simple
   getters and setters become direct field reads. Accepted.
5. The game camera on the holder: `fun getGameCamera()` over the holder's own private field (not a forward). Accepted.
6. Package of `MegaServices`: `com.megaman.maverick.game`. Accepted.
7. The game keeps its own fields; the holder is a local value in `postCreate`; no game member `services`. Accepted.
8. The host accessors of private fields (`getDrawables()`, `getShapes()`, `getSystems()`): in this branch. Accepted.
9. `.git-blame-ignore-revs`: list the `016c` squash SHA after the merge into the parent. Accepted.
10. A small temporary suite `Temp016cForwardsEquivalenceTest` with `Temp016cFrozenGameForwards`. Accepted.
11. The 3 defaults. Recommended: move them to `MegaGameEntity` (decision 6). Changed by the owner (verbatim): "I agree
    with you, remove them." Result: they are removed and not moved; the 48 callers read `game.audioMan` and
    `game.getGameCamera()` (decision 15).
12. Manual checks: real rooms, no test map; a short pass of 3 stages plus the targeted checks. Accepted.
