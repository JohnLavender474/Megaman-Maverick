# Contract: mega-refactor-016c-entity-services

- Status: approved
- Approved: 2026-10-29 (owner, "approved")

The gameplay that this branch must not change, and how to check it. Scope: `docs/SCOPE.md` (approved 2026-10-29).
The branch changes the type that entities take (`MegaServices` in place of the game), where 4 values live (the camera
flags in `GameCameraState`, the performance in `GameSettings`, `weaponsAttained` and the derived level reads in
`LevelState`), the creation order in `postCreate` (`players` and `gravity` earlier), and how entities reach the sound
and the camera (no `IMegaGameEntity` defaults). It does not change when a value is written or read, and every holder
field is the same session object as the game's. So every check below looks for "the same as the parent".

Rooms come from the live TMX files that `LevelDefinition` names (a room scan on 2026-10-29). Each `--room` has a player
spawn.

## Automated checks

All from the worktree root.

- [x] `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar` passes.
      Engine tests: 575, 0 failures (no engine change). Core tests: 249 plus the new tests, 0 failures.
- [x] Warnings: no new unique `^w:` line against the baseline of 81 (`grep "^w:" <log> | sed -E
      's/:[0-9]+:[0-9]+ / /' | sort -u`).
- [x] `MegamanMaverickGame` is named in no code line (comments excluded) of
      `core/src/main/kotlin/com/megaman/maverick/game/entities/`, `utils/MegaUtilMethods.kt` and
      `utils/misc/LightSourceUtils.kt`. The 5 files with commented-out code keep their comments.
- [x] The game has none of the removed members (`getCurrentLevel`, `setCurrentLevel`, `isInExtraLevel`, `getRooms`,
      `setRoomsSupplier`, `getCurrentRoom`, `setCurrentRoomSupplier`, `getWorldContainer`, `setWorldContainer`,
      `getTiledMapLoadResult`, `setTiledMapLoadResult`, `setCameraRotating`, `isCameraRotating`,
      `setFocusSnappedAway`, `isFocusSnappedAway`, `getPerformance`, `getDrawables`, `getShapes`, `getSystems`,
      `weaponsAttained`, the camera flag fields). The build proves that no caller is left in `core`, `lwjgl3` and the
      tests.
- [x] `IMegaGameEntity` has no `game`, no `getGameCamera`, `playSoundNow` or `stopSoundNow`, and no extension with an
      `IMegaGameEntity` receiver. `grep -rnE "playSoundNow|stopSoundNow" core/src` finds 0 lines. `MegaGameEntity` has
      no `getGameCamera`, `playSoundNow` or `stopSoundNow`.
- [x] Check scripts (scratch, the output goes in the process log):
      - Step 1 (homes): each changed line in `core` and `lwjgl3` is one of the listed rewrites (for example
        `game.getCurrentLevel()` → `game.levelState.currentLevel!!`, `game.isCameraRotating()` →
        `game.camera.rotating`, `game.getPerformance()` → `game.settings.performance`), except the lines of the new
        classes, the game members that go, and the listed hand edits.
      - Step 3 (defaults): each changed call line is `playSoundNow(` → `game.audioMan.playSound(`,
        `stopSoundNow(` → `game.audioMan.stopSound(`, or `getGameCamera()` → `game.getGameCamera()`, except the lines
        of `IMegaGameEntity`, `MegaGameEntity`, `FreezableEntityHandler` and `LightSourceUtils`.
      - Step 4 (swap): each changed line is the same line with `MegamanMaverickGame` replaced by `MegaServices`,
        except `MegaServices.kt` (new), the listed `postCreate` lines and `GameEntityUtils.kt` (`game.groups`).
- [x] Temporary suite `Temp016cForwardsEquivalenceTest` with its frozen copy `Temp016cFrozenGameForwards` (core test
      sources) passes. The frozen copy has the old game functions (current level, extra level, rooms, current room,
      world container, map load result, the two camera flags, performance) over their old storage. Seeded sequences of
      writes run on both sides; after each write, every old read gives the same value as the new direct read
      (references compared by identity), and the old function that throws (`getCurrentLevel()`,
      `getTiledMapLoadResult()` before a write) throws the same exception as the new read. The suite and the frozen
      copy are deleted in their own commit after the play test.
- [x] Permanent tests pass:
      - `EntityConstructorsTest`: every concrete `MegaGameEntity` class in the main output of the `entities` package
        has a primary constructor with one required parameter of type `MegaServices`, and the call with a helper holder
        succeeds. The one named exception is `PushableBlock.InnerBlock`.
      - `MegaEntityFactoryTest`: after `init(services)`, `fetch(X::class)` gives an entity whose `game` is the same
        holder object.
      - `LevelStateTest`: the expectations of the old game-function tests, now on the `LevelState` members
        (`currentRoom`, `getRooms(out)`, `isInExtraLevel`, the throw of `currentLevel!!` before a write); the values
        stay when a new level is set; `weaponsAttained` starts `null`.
      - `GameCameraStateTest`: `rotating` and `focusSnappedAway` start `false`; a game's `camera` is one object for the
        session.
      - `GameSettingsTest`: a game's `settings.performance` starts as `params.performance`.
- [x] The 17 entity tests pass on the test helper (a real `MegaServices`), with no change in their asserts. The tests
      that keep the host (`GameClockTest`, `MegaLevelScreenEventsTest`, `ContactRuleSelectionTest`) pass unchanged.
- [x] Short smoke launches (about 15 to 20 seconds each; a clean exit or the timeout with no exception in the log is a
      pass): `./run-game-alpha.sh --level INTRO_STAGE --room room1`, `./run-game-alpha.sh --level MOON_MAN --room
      room6` and `./run-game-alpha.sh --performance low --level WILY_STAGE_1 --room room2`.
- [-] At the merge (not on this branch): after the squash into the parent, `.git-blame-ignore-revs` gets the full SHA
      of the `016c` squash commit, in a separate commit in the parent's worktree (owner answer to open question 9).

Results (2026-10-29, after step 7 at `2de63cb0d`): clean build passes; engine 575, core 260 tests, 0 failures; 81
unique warnings, no new one. 0 code lines name `MegamanMaverickGame` in `entities/`, `MegaUtilMethods.kt` and
`LightSourceUtils.kt` (only the comments of the 5 commented-out files). 0 `playSoundNow`/`stopSoundNow`. The game has
none of the removed members; `IMegaGameEntity` has no `game` and no default. Check scripts: step 2, 142 line pairs, 0
problems; step 4, 47 line pairs, 0 problems; step 5, 950 line pairs, 0 problems (the listed hand edits are in the
process log). The temporary suite and the permanent tests pass. The 17 entity tests pass on `testServices()` with no
assert change, except the one `DrWilyTest` verify that the type now makes impossible (process log). 3 smoke launches:
each ran 20 seconds and loaded its level, no exception. The `.git-blame-ignore-revs` item is for the merge.

## Manual playthrough

The owner plays these once, after the implementation. Each line is the command from the worktree root and what to
look at. The commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-016c-entity-services`

Short pass of real stages (every entity changed its constructor type; enemies, items, blocks and hazards must look,
sound and act as before):

1. `./run-game-alpha.sh --level INTRO_STAGE --room room1`: play to the boss gate. Enemies spawn, fire, drop items;
   sounds play; Megaman dies in a pit and respawns.
2. `./run-game-alpha.sh --level REACTOR_MAN --room room4`: the toxic water waves move; enemies and blocks act as
   before; room transitions.
3. `./run-game-alpha.sh --level WILY_STAGE_2 --room fork_room`: take one path; enemies, hazards and room transitions
   as before.

Targeted checks:

4. `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: the gravity switch turns the camera; during the turn,
   projectiles and Megaman's controls stop and the trail sprite hides; after the turn, the camera eases back to
   Megaman; the gravity blocks of each group start to move only when their group comes near the camera.
5. `./run-game-alpha.sh --level MOON_MAN --room room6`: the gravity switches in rooms 8 to 10 turn the camera as
   before; the gravity blocks and their groups in room 10 act as before; the heart tank in room 10.
6. `./run-game-alpha.sh --level MOON_MAN --room room14`: beat the boss. The victory, the beam-out, the
   get-weapon screen with the new weapon, then the save screen, as before.
7. `./run-game-alpha.sh --level TIMBER_WOMAN --room room1`: the falling leaves fall at the same speed and only in the
   leaf rooms. Pause, open the settings panel, change the performance down and up: the notification and the FPS
   change; the leaves keep the same speed. Then in `room5` the `CannoHoney` sends its bees one by one with a delay.
8. `./run-game-alpha.sh --performance low --level WILY_STAGE_1 --room room2`: the `DragonFly` enemies move as before at
   the low setting.
9. `./run-game-alpha.sh --weapons all --level INTRO_STAGE --room room2`: the `BikerKibbo` does not spawn again at once
   after it is destroyed (the spawn delay). Freeze an enemy with `FRIGID_SHOT`: the ice shield shows, the enemy stops,
   and it breaks free after the same time. Throw the axe (`AXE_SWINGER`): it leaves after the same short delay.
10. `./run-game-alpha.sh --level DESERT_MAN --room room2`: the `Jetto` spawn delay as before.
11. `./run-game-alpha.sh --level DESERT_MAN --room room14`: get the heart tank; the health bar grows.
12. `./run-game-alpha.sh --level WILY_STAGE_1 --room room8`: the dark room; lights and shots light up the dark as
    before; leave the room and come back.
13. `./run-game-alpha.sh --level RODENT_MAN --room room8`: in rooms 10 and 11, push the blocks and use the switch gates;
    they act only in their own rooms.
14. `./run-game-alpha.sh --level GLACIER_MAN --room room2`: the snowfall in `room3` shows only in that room.
15. `./run-game-alpha.sh --level GAUNTLET_1 --room room1`: an extra level. Pause and exit: the exit works (it works only
    in an extra level or a beaten level).
16. Any level with a ladder (for example check 1): climb to the top of a ladder; the camera eases to Megaman as before.

Play test result (2026-10-29): the owner (verbatim): "All tests pass". All manual checks pass. Check 8 ran with the
run script after `29bbb5e04` (the scripts no longer pass `--performance`; before that, the second option failed with
"Can only specify option --performance once").

After the temporary suite was deleted (`22338b9e5`): clean build passes; engine 575, core 259, 0 failures; 81 unique
warnings, no new one.

## Behaviors to check

- Death and respawn, a game over and a continue (check 1).
- The pause menu and the settings panel (checks 7 and 15).
- A second level in one session (the pools and the level values carry over between levels): after check 15 exits to
  the menus, start any robot master level from the level select; it starts, plays and draws as before.

## Intended changes

None.
