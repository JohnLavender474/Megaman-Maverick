# Contract: mega-refactor-016b-entity-host-reads

- Status: approved (revision 2: the `DrWily` ending as an event, `SCOPE.md` "Revision 2")
- Approved: revision 1 on 2026-10-28 (owner, "yes to both"); revision 2 on 2026-10-28 (owner, "approved to all")

The gameplay that this branch must not change, and how to check it. Scope: `docs/SCOPE.md` (approved 2026-10-28).
The branch changes where 2 values live, how the `DrWily` screen switch is reached, and two declarations: the pause
flag (`game.clock`), `selectButtonAction` (`game.players`), the ending-screen switch (the event `GAME_COMPLETE`, which
the level screen handles), the declared type of the drawables map, and the dead `endGame` read on `VICTORY_EVENT`. It
does not change when a value is written or read. The one timing change is in "Intended changes". So every check below
looks for "the same as the parent".

Rooms come from the live TMX files that `LevelDefinition` names (`SCAN.md` section 7 and a room scan on 2026-10-28).
Each `--room` has a player spawn.

## Automated checks

All from the worktree root.

- [x] `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar` passes.
      Engine tests: 575 (after the merge of the parent, `7291362c8`), 0 failures (no engine change). Core tests: 225 plus the new tests, 0 failures.
- [x] Warnings: no new unique `^w:` line against the baseline of 81 (`grep "^w:" <log> | sed -E
      's/:[0-9]+:[0-9]+ / /' | sort -u`).
- [x] `grep -rnE "game\.(paused|selectButtonAction)\b" core/src lwjgl3/src` finds 0 lines, and
      `MegamanMaverickGame` has no member `paused` or `selectButtonAction` (the build proves that no caller is left).
- [x] `grep -rn "setCurrentScreen" core/src/main/kotlin/com/megaman/maverick/game/entities` finds 0 lines, and
      `grep -n "endGame" core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt` finds 0
      lines.
- [x] `grep -rn "java.util.Queue" core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt` finds 0
      lines; the drawables field, the local map in `createGameEngine`, the return type of `getDrawables()` and the
      `MegaLevelScreen` field name `InsertionOrderPriorityQueue`, with no cast.
- [x] Temporary suite `Temp016bDrWilyEquivalenceTest` with its frozen copy `Temp016bFrozenDrWily` (core test
      sources) passes. It runs the old `DrWily` (it calls `game.setCurrentScreen`) and the new `DrWily` (it submits
      `GAME_COMPLETE`) on the same seeded spawn values and delta sequences, with the target player alive and dead.
      After each update both sides have the same state, position and facing, and the same number of requests at the
      same update; each old request names `ENDING_SCREEN`, each new request is a `GAME_COMPLETE` event. The suite and the frozen copy are deleted
      in their own commit after the play test.
- [x] Permanent tests pass:
      - `DrWily` submits `GAME_COMPLETE` once, when the still timer finishes in `LAND` and the target lives; never
        when the target is dead.
      - `MegaLevelScreen` (built with a relaxed game mock): its event mask contains `GAME_COMPLETE`;
        `onEvent(GAME_COMPLETE)` calls `game.setCurrentScreen(ENDING_SCREEN.name, ...)` once; if `MLS.init` can run
        in a unit test, `VICTORY_EVENT` with and without `ConstKeys.END = true` starts the end-level handler in the
        same way (else manual check 7 covers it).
      - A real `MegamanMaverickGame`: `clock.paused` starts `false`; `pause()` sets it, a second `pause()` changes
        nothing, `resume()` clears it.
      - `Starkner`: the sprite `preProcess` keeps the rotation while `clock.paused` is `true` and turns the sprite
        when it is `false`.
      - `MegaPlayers.selectButtonAction` starts `NEXT_WEAPON` and keeps a value that is set.
- [x] The existing tests pass with no change in their asserts (for example `StarknerAliveTest`, `MegaPlayersTest`,
      `LevelStateTest`).
- [x] Short smoke launches (about 15 to 20 seconds each; a clean exit or the timeout with no exception in the log is
      a pass): `./run-game-alpha.sh --level MOON_MAN --room room12` and
      `./run-game-alpha.sh --level INTRO_STAGE --room room1`.

## Manual playthrough

The owner plays these once, after the implementation. Each line is the command from the worktree root and what to
look at.

1. `./run-game-alpha.sh --level WILY_STAGE_3 --room room8 --weapons all`: `room8` is the nearest room with a player
   spawn before the final boss (the room just before the boss has no spawn). Go on and defeat the final boss. Dr.
   Wily flies up, falls, lands, begs and stays still; then the ending screen opens with its music, as on the parent.
   No freeze, no crash, and no visible extra frame at the switch. Let the ending run: the credits and then the main
   menu follow.
2. `./run-game-alpha.sh --level MOON_MAN --room room12`: this room has two `Starkner`. While a `Starkner` moves, its
   sprite turns in steps of 90 degrees. Pause the game (`START`): the sprite stops turning while the pause menu is
   open, and turns again after the resume.
3. `./run-game-alpha.sh --level MOON_MAN --room room12 --pauseOnFocusLost --resumeOnFocusGained`: click on another
   window: the game pauses (pause menu, pause sound). Click on the game window again: the game resumes.
4. `./run-game-alpha.sh --level INTRO_STAGE --room room1 --weapons all`: the select action and the pause.
   - With the default setting (`NEXT_WEAPON`): `SELECT` goes to the next weapon; the air dash (if Megaman has it)
     uses `A`; `START` pauses.
   - Pause, open the settings, then the keyboard settings, and set the select action to `AIR_DASH`. Go back: the
     level starts again. `SELECT` now does the air dash and no longer changes the weapon.
   - Set it to `START`: `SELECT` pauses the game, as `START` does.
   - Leave it on `START`, exit the game and start it again with the same command: at boot the select action is
     `NEXT_WEAPON` again, as on the parent (open issue `docs/issues/2026-10-22-select-button-action-resets.md`; this
     branch does not change it). Set it back to the value that you want to keep.
5. `./run-game-alpha.sh --level GAUNTLET_1 --room room1`: an `EXTRA_LEVEL`, so the exit option is always allowed (in
   other levels the exit works only when the level is already defeated, `LevelPauseScreen`). Pause, then choose the
   exit option. The next screen opens and takes input (the game is not left paused).
6. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`: not a level, so no `--room`. The main menu, the level select and
   the robot master intro take input and animate as on the parent (they check the pause flag).
7. `./run-game-alpha.sh --level DESERT_MAN --room room16`: go through the gate and defeat the boss (a
   `VICTORY_EVENT`). The beam-out runs, then the get-weapon screen (if the level is new) or the save screen opens, as
   on the parent.
8. `./run-game-alpha.sh --level TIMBER_WOMAN --room room1`: a short sanity pass of the draw order: the background is
   behind the map, Megaman, the `Bat` enemies and their projectiles are in front of the map, the foreground is in
   front of all. Walk into `room2`.

## Behaviors to check

These are in the checks above:

- The ending screen after the final boss (now switched by the level screen on `GAME_COMPLETE`).
- The end of a robot master level (`VICTORY_EVENT`, the beam-out, the next screen).
- Pause and resume: from the level (`START` and `SELECT` with the `START` action), from the window focus, the exit
  from the pause menu, and the pause checks in the menus.
- The `Starkner` sprite during a pause.
- The select action: next weapon, air dash, pause; changed in the settings screen and loaded from the prefs.
- The draw order of the three drawing sections.

## Intended changes

- Timing only, with no visible effect (`016` decision 12; `SCOPE.md` "Revision 2"): the switch to the ending screen
  moves from inside `engine.update` to `eventsMan.run()` later in the same frame N, the point where `END_LEVEL` and
  game over switch today. In frame N the systems and the rest of the level screen's `render` now run on the live
  level (not a half-reset one), the engine resets at once at the switch (not at the end of its update), the level
  screen and the destroyed entities still get the frame-N events after the switch, and `TURN_CONTROLLER_ON` from the
  level screen reset arrives in frame N+1. The level still draws in frame N, the ending screen still renders first in
  frame N+1, and the music order is the same.

## Results

- Automated checks, plan step 7 (2026-10-28, code at `f0c2d7a1d`; the last build at `7cbc89c3b`, step 6 changes only
  the root `CLAUDE.md`): all pass. Clean build; engine 575, core 236 (225 of the baseline, 5 of step 1, 1 of step 2,
  1 temporary suite, 4 of step 4), 0 failures; 81 unique warnings, no new one. The `grep` checks give 0 lines
  (`game.paused`, `game.selectButtonAction`, `setCurrentScreen` in entities, `endGame`, `java.util.Queue` in the game
  class, casts to `InsertionOrderPriorityQueue`). The temporary suite passed on the old code (step 3) and unchanged on
  the new code (steps 4 and 5). The `VICTORY_EVENT` unit case was dropped: `MegaLevelScreen.init()` needs LibGDX
  (`Gdx.files` is `null` in a unit test), so manual check 7 covers it.
- Smoke launches (2026-10-28, `timeout 20` each, exit by the timeout): `--level MOON_MAN --room room12` and
  `--level INTRO_STAGE --room room1 --logTags all`. No exception and no `ERROR` line in the console or in
  `assets/logs.txt`; the second run logs `startAtLaunchRoom(): room=room1` and the switch to `LEVEL_SCREEN`, then
  about 14 seconds of play frames.
- Play test of the 8 manual checks on the implemented code (owner, 2026-10-29, verbatim: "All tests for 016b pass"):
  pass. The final boss run used `--weapons all --heartTanks all --healthTanks all`; check 5 ran in `GAUNTLET_1`.
- After the play test, plan step 8 (2026-10-29): the temporary suite and its frozen copy are deleted (`1b82fe890`).
  The automated checks again: `clean engine:test core:test lwjgl3:jar` passes; engine 575, core 249 (250 after the
  parent merge `2ee55ef9d`, minus the temporary suite), 0 failures; 81 unique warnings, no new one; the `grep` checks
  give 0 lines; no `Temp016b` name is left in the sources. No game launch (coordinator).
