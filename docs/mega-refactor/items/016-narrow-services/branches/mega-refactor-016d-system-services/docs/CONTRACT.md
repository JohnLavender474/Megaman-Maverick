# Contract: mega-refactor-016d-system-services

- Status: approved
- Approved: 2026-10-29 (owner, verbatim: "contract approved")

The gameplay that this branch must not change, and how to check it. Scope: `docs/SCOPE.md` (approved 2026-10-29).
The branch changes what 3 pieces of code take: the contact rules take `players` and `gravity`, the pathfinder factory
takes `levelState`, and `LevelDefinition.screenOnCompletion` takes `LevelState`. It also builds `players` and
`gravity` with the game object, not in `postCreate()`. It does not change when a value is read or written, and each
piece reads the same session object as before. So every check below looks for "the same as the parent".

Rooms come from the live TMX files that `LevelDefinition` names (room scan, `SCAN.md` section 7, 2026-10-29). Each
`--room` has a player spawn. The room `REFACTOR_TEST_016` `gravity_change` is new (a plan step builds it before the code
changes).

## Automated checks

All from the worktree root.

- [x] `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar` passes.
      Engine tests: 575, 0 failures (no engine change). Core tests: 259 plus the new tests, 0 failures.
- [x] Warnings: no new unique `^w:` line against the baseline of 81 (`grep "^w:" <log> | sed -E
      's/:[0-9]+:[0-9]+ / /' | sort -u`).
- [x] `MegamanMaverickGame` is named in no code line (comments excluded) of `world/`, `pathfinding/` and `levels/` in
      `core/src/main/kotlin/com/megaman/maverick/game/`. Outside `screens/`, the only main files that name it in code
      are `MegamanMaverickGame.kt`, the 13 files in `drawables/backgrounds/`, `controllers/ScreenController.kt` and
      `lwjgl3/.../DesktopLauncher.kt`. The 5 entity files with commented-out code keep their comments.
- [x] `MegamanMaverickGame.kt`: `players` and `gravity` are `val` fields built with the game object; `postCreate()` has
      no `players =` or `gravity =` line; `MegaServices`, the `Megaman` build, `state.addListener(megaman)` and
      `players.register(PlayerSlot(0), megaman)` stay in `postCreate()` in the same order.
- [x] No test builds the contact rules with a mocked `MegamanMaverickGame` (`ContactRuleSelectionTest`,
      `CartV2ItemTest` use a real `MegaPlayers` and `WorldGravity`).
- [x] `ContactRuleSelectionTest` passes with no change to `core/src/test/resources/contact-rule-selection.txt` (`git
      diff` of the file is empty). This proves that the rule table selects the same rule for every pair and phase.
- [x] `CartV2ItemTest` passes with no change to its asserts.
- [x] New permanent tests pass:
      - `MegaContactRulesTest` (the 2 rules that read `players` and `gravity`, taken from the built table):
        - Feet on sand, `BEGIN`: with a player registered and in play, one sand splash spawns at the bottom center of
          the feet fixture; with no player in play, no splash spawns. In both cases the body senses `FEET_ON_SAND`.
        - Body in a gravity-change area with a direction: for a player entity, the world gravity takes the direction,
          and every registered player gets it; for a non-player `IDirectional` entity, only that entity gets the
          direction, and the world gravity and the players keep theirs.
      - `MegaPathfinderFactoryTest`: the factory is built with a `LevelState`; a world container and a map load result
        that are set after the build are used by the next `getPathfinder` call (world width and height from the current
        map load result); after the level values change, the next call uses the new values.
      - A game field test: a new `MegamanMaverickGame()` (no `create()`) has `players` and `gravity`; each is one object
        for the session; `gravity.direction` starts `UP`; `gravity.set(direction)` sets the direction of an entity
        registered in `game.players`.
      - `LevelCompletionScreensTest`: each of the 19 `LevelDefinition` entries gives the same screen as today for a
        `LevelState()` with `weaponsAttained` `null` and with a non-empty set: the 9 robot master levels give
        `SAVE_GAME_SCREEN` and `GET_WEAPONS_SCREEN`; `WILY_STAGE_1` to `WILY_STAGE_3`, `TEST_1`, `TEST_2`,
        `REFACTOR_TEST_029`, `REFACTOR_TEST_031` give `SAVE_GAME_SCREEN`; `SMB3_1` gives `LEVEL_SCREEN`; `GAUNTLET_1`
        and `GAUNTLET_2` give `EXTRAS_SCREEN`. The new `REFACTOR_TEST_016` is in the table too (`SAVE_GAME_SCREEN`, as
        the other test levels). An entry that is not in the table fails the test.
- [x] The tests that keep the host (`MegaLevelScreenEventsTest`, `LevelStateTest`, `GameClockTest`,
      `GameSettingsTest`, `GameCameraStateTest`) pass unchanged.
- [x] Short smoke launches (about 15 to 20 seconds each; a clean exit or the timeout with no exception in the log is a
      pass):
      - `./run-game-alpha.sh --level REFACTOR_TEST_016 --room gravity_change` (on the baseline code after the map step,
        and again after the code steps).
      - `./run-game-alpha.sh --level DESERT_MAN --room room10` (pathfinding, `--pathfinding async` is the default).
      - `./run-game-alpha.sh --pathfinding sync --level DESERT_MAN --room room10` (the other pathfinding system).
      - `./run-game-alpha.sh --level DESERT_MAN --room room1` (sand).

No temporary old-versus-new suite (scope, open question 5): no runtime path changes.

Results (2026-10-29, after plan step 6 at `3b0512c8c`): clean build passes; engine 575, core 272 tests (259 + 13 new:
`GamePlayersFieldsTest` 2, `MegaContactRulesTest` 5, `MegaPathfinderFactoryTest` 3, `LevelCompletionScreensTest` 3), 0
failures; 81 unique warnings, no new one. `MegamanMaverickGame` in code outside `screens/`: only `MMG`, the 13
backgrounds, `ScreenController` and `DesktopLauncher`; the 5 entity files name it only in comments; 0 in `world/`,
`pathfinding/`, `levels/`. `screens/`: 32 files. Test files that name the host: `MegaLevelScreenEventsTest` (a screen) and
the 5 tests that build a real `MegamanMaverickGame()` for a session field (`GameCameraStateTest`, `LevelStateTest`,
`GameClockTest`, `GameSettingsTest`, `GamePlayersFieldsTest`); no mock for the contact rules.
`contact-rule-selection.txt`: no diff against the base. `LevelCompletionScreensTest` also passed on the old code (plan
step 5 adapter, not committed). 4 smoke launches (20 seconds each, the jar run from `assets/` with the alpha script
options): `REFACTOR_TEST_016 gravity_change`, `DESERT_MAN room10` (async and sync), `DESERT_MAN room1`; no exception.
The `DESERT_MAN` log has the known `SpawnersLayerBuilder` errors for `Cactus` in `blocks` and `Bat` in `specials`
(`docs/issues/2026-10-27-desert-man-enemies-on-wrong-layers.md`); they are on the base too and not from this branch.

## Manual playthrough

The owner plays these once, after the implementation. Each line is the command from the worktree root and what to
look at. The commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-016d-system-services`

Sand (the feet-sand rule reads `players`):

1. `./run-game-alpha.sh --level DESERT_MAN --room room1`: jump onto the quick sand. Each landing makes a sand splash
   with its sound; Megaman moves slowly and sinks as before; he can jump out. If an enemy lands on the sand, it makes a
   splash too (the splash spawns for any feet while Megaman is in play).

Gravity change (the gravity-change rule reads `gravity`):

2. Replaced (owner, 2026-10-29, verbatim: "For 016d, we'll test via GravitySwitcharoo instead"): checks 3
   (`REFACTOR_TEST_029 gravity_switch`) and 4 (`WILY_STAGE_3 room5`) replace this check. `MegaContactRulesTest` still
   covers the gravity-change rule for a player and a non-player. The test map and the room stay. In the play test,
   Megaman was at the top of the screen with the camera oriented down in this room; a separate read-only agent checks
   whether that is a bug (an issue on `master` if so). The old check text, not run:
   `./run-game-alpha.sh --debugShapes --level REFACTOR_TEST_016 --room gravity_change`: each gravity-change area shows
   its outline; the color is the area's gravity direction: red = `DOWN`, green = `UP`, yellow = `LEFT`, cyan = `RIGHT`,
   white = no direction (a gravity scalar only). This room has, from the left: a red area, a green area, and at the
   right end a red area with the `Met` in it. Walk right into the red area: the world gravity turns down, Megaman turns
   and falls to the ceiling, and the camera rotates as before. Go on into the green area: Megaman and the camera turn
   back. The `Met` in its own red area turns to `DOWN` (it shows upside down); while only the `Met` is in an area,
   Megaman and the camera do not turn.
3. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: hit the `GravitySwitcharoo`. The gravity and
   the camera turn as before (this uses `gravity` from an entity, not from the rules; `gravity` is now built earlier).
4. `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: the gravity switches turn the camera; during the turn,
   Megaman's controls stop; after the turn, the camera eases back to Megaman.

Pathfinding (the factory reads `levelState`):

5. `./run-game-alpha.sh --debugPathfinding --level DESERT_MAN --room room10`: the `Bat` enemies leave their perches,
   chase Megaman around blocks and fly back as before; the path lines follow them. Die and respawn in the room: the
   `Bat` enemies chase again.
6. `./run-game-alpha.sh --pathfinding sync --level DESERT_MAN --room room10`: the same with the other pathfinding
   system.
7. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room enemy_chase`: the `Bat` chases Megaman as before.

Level completion (`screenOnCompletion` takes `LevelState`):

8. `./run-game-alpha.sh --level DESERT_MAN --room room16`: go on and beat the boss. The victory, the beam-out, the
   get-weapon screen with `NEEDLE_SPIN`, then the save screen, as before. Then start `TIMBER_WOMAN` from the level
   select: a second level in the same session; in `room1` the `Bat` enemies chase Megaman (the pathfinder
   factory reads the new level's values).
9. `./run-game-alpha.sh --level WILY_STAGE_1 --room room10`: go on and beat the boss. After the victory, the save screen
   shows (no get-weapon screen), as before.

Play test result (2026-10-29): the owner (verbatim): "All tests pass". All manual checks pass; check 2 is replaced by
checks 3 and 4 (the `GravitySwitcharoo` checks). After the play test (at `fb5c9f7b2`): clean build passes; engine 575,
core 272, 0 failures; 81 unique warnings, no new one. No game launch.

## Behaviors to check

- Death and respawn (check 5).
- A second level in one session (check 8): the level values, the pools, `players` and `gravity` carry over; the new
  level plays as before.
- The start of the game with no `--level` (`./run-game-alpha.sh`): the logo and the menus show, and a level starts from
  the menus as before (the creation order changed).

## Intended changes

None.
