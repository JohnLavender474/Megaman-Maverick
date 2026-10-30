# Contract: mega-refactor-016a-typed-level-state

- Status: approved
- Approved: 2026-10-28

The gameplay that this branch must not change, and how to check it. Scope: `docs/SCOPE.md` (approved 2026-10-28).
The branch changes where 23 values are stored (the property bag of the game becomes typed fields), not when they are
written or read. So every check below looks for "the same as `master`".

Rooms come from the live TMX files that `LevelDefinition` names (`SCAN.md` section 10 and a room scan on
2026-10-28). Each `--room` has a player spawn. For a boss, the room is the room that holds the boss `Gate`.

## Automated checks

All from the worktree root.

- [x] `JAVA_HOME=/home/johnlavender/.jdks/corretto-17.0.13 ./gradlew clean engine:test core:test lwjgl3:jar` passes.
      Engine tests: 571, 0 failures (no engine change). Core tests: 219 plus the new tests, 0 failures.
- [x] Warnings: no new unique `^w:` line against the baseline of 87 (`grep "^w:" <log> | sed -E
      's/:[0-9]+:[0-9]+ / /' | sort -u`). Lines that go away with the removed bag code are allowed.
- [x] `grep -rnE "game\.(is|put|get|getOrDefault|remove|has)Property\(" core/src lwjgl3/src` finds 0 lines outside
      comments.
- [x] `MegamanMaverickGame` does not implement `IPropertizable` and has no `properties` member (the build proves
      it: no code can call the bag).
- [x] `grep -rnwE "ConstKeys\.(COINS|LEVEL_POINTS|TIME_SINCE_LAST_FLOATING_POINTS|LAST_FLOATING_POINTS_TYPE|WEAPONS_ATTAINED|ROOM_TRANSITION|TILED_MAP_LOAD_RESULT|WORLD_CONTAINER|DRAWABLES|SHAPES|SYSTEMS)" core/src lwjgl3/src`
      finds 0 lines, and the 11 constants are gone from `Constants.kt`.
- [x] Temporary suite `Temp016aBagEquivalenceTest` with its frozen copy `Temp016aFrozenGameBag` (core test sources)
      passes. It runs seeded random sequences of the write moments of `SCAN.md` section 4 on the frozen bag
      expressions and on a real `MegamanMaverickGame`. After each step, each read gives the same value on both sides:
      - `ROOM_TRANSITION`, beam flag, camera rotating, focus snapped away: `Boolean`, `false` before the first write;
      - beam center, rooms supplier, current-room supplier, world container, map load result: the same object
        (identity), `null` (or "no supplier") before the first write;
      - coins and points as the gameplay code reads them (`0` before the first write) and as the HUD text reads
        them (compared only after a level start; see "Unreachable differences");
      - floating-points time and type, with the `AbstractEnemy` steps (aggregate window, next type, points added)
        and the per-frame `+ delta`;
      - weapons attained: "set" (`hasProperty` against `!= null`) and the set object, after each put, remove,
        get-weapon read, save load and `resetPlayerProgress`;
      - current level: `getCurrentLevel()` (both throw before the first write), `isInExtraLevel()`;
      - values that no moment resets (floating-points type, beam flag and center, current level) keep their value
        across a new level start on both sides.
      The suite and the frozen copy are deleted in the last commit before the merge.
- [x] Permanent tests pass: `LevelStateTest` (each field starts with the value in `SCOPE.md` item 1; the beam center
      keeps the reference) and the checks on a real `MegamanMaverickGame` (`SCOPE.md` item 6: `getCurrentLevel()` and
      `getTiledMapLoadResult()` throw before the first write, `isInExtraLevel()` is `false`, `getRooms(out)` returns
      `out` unchanged, `getCurrentRoom()` is `null`, both camera flags are `false`, `weaponsAttained` is `null`).
- [x] The 8 tests that stub `game.getCurrentLevel()` pass with no change.
- [x] Short smoke launches (about 15 to 20 seconds each, the window closed by the owner or by a timeout; a clean
      exit or the timeout with no exception in the log is a pass):
      `./run-game-alpha.sh --level SMB3_1 --room room2` and `./run-game-alpha.sh --level WILY_STAGE_1 --room room8`.

## Manual playthrough

The owner plays these once, after the implementation. Each line is the command from the worktree root and what to
look at.

1. `./run-game-alpha.sh --level WILY_STAGE_1 --room room8`: this room has `DarknessV2`. While Megaman beams in, a
   light follows the beam down; after the landing the beam light goes and the normal player light stays. Die once
   (for example in a pit or to an enemy): at the respawn the beam light shows again in the same way.
2. `./run-game-alpha.sh --level SMB3_1 --room room2`: a `MARIO_LEVEL`. The HUD shows `POINTS: 0` and `COINS: 0` at
   the start. Hit the question blocks: `COINS` goes up by 1 for each coin. Collect coins: `COINS` goes up by 1 each.
   Defeat several enemies quickly one after another: the floating points go up in steps (100, 200, ... up to a
   one-up) and `POINTS` adds them; after a pause the next enemy gives 100 again. Go through a pipe: the camera moves
   smoothly to Megaman after the pipe (focus snapped away), not in a jump. Die once: the HUD keeps the values it had
   (only a new level start sets them to 0).
3. `./run-game-alpha.sh --level TIMBER_WOMAN --room room9`: walk right into `room10`. During the room transition the
   saws are hidden, and they show again when the transition ends.
4. `./run-game-alpha.sh --level RODENT_MAN --room room8`: walk on into `room9` and `room10`. Push the pushable
   blocks; at a room transition a moving block stops and does not slide. The switch gates in `room9` to `room11` open
   and close as on `master`.
5. `./run-game-alpha.sh --level WILY_STAGE_2 --room fork_room`: the pipe puffs and saws in this room. Go through a
   room transition near puffs: the puff timers wait during the transition (no puff fires while the camera moves).
6. `./run-game-alpha.sh --level MOON_MAN --room room6`: `room6` is the nearest room with a player spawn before the
   `GravitySwitcharoo` rooms (`room8`, `room9`, `room10`). Go on to them and switch gravity. During the camera
   rotation Megaman cannot shoot (the shot is not fired on release), and after the rotation the camera follows
   Megaman again with no jump. The asteroids stop spawning during a room transition.
7. `./run-game-alpha.sh --level DESERT_MAN --room room16`: go through the gate and defeat the boss. During the room
   transition into the boss fight Megaman keeps his animation and cannot shoot; the boss appears and fights as on
   `master`. After the beam-out the get-weapon screen shows `NEEDLE_SPIN`; then the save screen opens.
8. `./run-game-alpha.sh --level INTRO_STAGE --room room1`: a short sanity pass through `room1` to `room3`: room
   transitions (also one on a ladder, if the path has one), enemies spawn after each transition, pause and resume.
9. `./run-game-alpha.sh --level GAUNTLET_1 --room room1`: an `EXTRA_LEVEL`. Open the pause menu: the options are the same as on
   `master` (the exit option depends on `isInExtraLevel()`).
10. `./run-game-alpha.sh --screen MAIN_MENU_SCREEN`: not a level, so no `--room`. Load the saved game (if a save exists): the game goes to the level
    select screen (or starts the intro stage), not to the get-weapon screen.

## Behaviors to check

These are in the checks above:

- Room transitions: entities that hide, stop or wait during a transition (bosses, saws, pushable blocks, puffs,
  asteroids, meteor showers, lava rivers, spawners that are not blocks, hazards, specials or decorations), and
  Megaman (no shot on release, the prior animation kept, ladder climb).
- Player spawn and death: the beam flag and center; the room transition flag and the focus flag go back to `false`.
- Level start: coins, points and the floating-points timer go back to `0`; the camera flags go back to `false`.
- Level end: the beam-out; the get-weapon screen after a new robot master win; the save screen after it.
- Save load from the main menu: no stale get-weapon screen.
- Camera rotation and the focus interpolation.

## Intended changes

None.

## Unreachable differences

Two reads differ only before the first level start of the session. The game cannot reach them (owner decision
2026-10-28, `SCOPE.md` open question 7):

- The HUD text of a `MARIO_LEVEL`: `"POINTS: null"` and `"COINS: null"` on `master`, `"POINTS: 0"` and `"COINS: 0"`
  now. The HUD draws only in `MegaLevelScreen.render`, and `MegaLevelScreen.start` sets both values to `0` first.
- The floating-points timer: a `NullPointerException` on `master`, `0f` now. Both readers run only after
  `MegaLevelScreen.start` sets it to `0f`.

## Results

- Automated checks, step 6 (2026-10-28, code at `6fd9ae190`): all pass. Engine 571, core 226 (with the temporary
  suite), 0 failures; 81 unique warnings, no new one; the `grep` checks give 0 lines; the 8 `getCurrentLevel()` stub
  tests pass; both smoke launches ran with no exception.
- Baseline run of the 10 manual checks on the old code (owner, 2026-10-28, "Manual checks pass. Contract approved"):
  pass. Not the play test.
- Play test of the 10 manual checks on the implemented code (owner, 2026-10-28, verbatim: "All tests pass"): pass.
- After the play test, step 7 (2026-10-28): the temporary suite and its frozen copy are deleted (`8036b34df`). The
  automated checks again: `clean engine:test core:test lwjgl3:jar` passes; engine 571, core 225 (the 219 of the
  baseline and the 6 tests of `LevelStateTest`), 0 failures; 81 unique warnings, no new one; the `grep` checks give
  0 lines; no `Temp016a` name is left in the sources. No game launch (coordinator).
