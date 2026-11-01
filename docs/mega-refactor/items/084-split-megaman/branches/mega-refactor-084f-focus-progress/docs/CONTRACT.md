# Contract: mega-refactor-084f-focus-progress

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-01

The branch moves the camera focus and the game state listener off the `Megaman` class. The camera, the `PRECIOUS_GUARD` cluster, the heart tanks, the weapon reward and the currency sound must act the same, in the same frames.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with these tests:
  - `FocusComponent`: it gives the values of its hooks, and each read calls the hook.
  - The focus position: for each of the 4 directions, the point is half a body height past the inverted direction point of the body.
  - `IGameEntity.focus`: it gives the component, and it throws if the component is missing.
  - `PlayerProgressComponent`: a level defeat, an added and a removed heart tank, and added currency each call the hook once. A removed level, a removed currency and the tank and enhancement members call no hook.
  - The Mega hooks: a level defeat sets `weaponsAttained`, and `MegamanHeartTankTest` passes through the component. The sound plays only if the currency is below the max.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The interfaces are gone: the class line of `Megaman.kt` has 8 interfaces, and `grep -nE 'IFocusable|IGameStateListener' core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/Megaman.kt` finds nothing.
- [ ] The dead key is gone: `grep -rn 'ConstKeys.FOCUS\b\|const val FOCUS\b' core/src` finds nothing.
- [ ] A short launch of `REFACTOR_TEST_029 --room heart` and of `REFACTOR_TEST_051 --room scroll_corridor` starts with no exception, at the end of the branch.

## Frame order

No line moves. The focus reads stay at `MegaLevelScreen.kt:336`, `MegamanWeaponsHandler.kt:383` and in the camera parts. `GameState` calls one listener at the same points, and `MegamanMaverickGame.kt:444` registers it at the same line. Review the final diff for any other change of order. Lines are at `6eb3884bd`.

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084f-focus-progress`

Experimental: the camera focus:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: switch to each gravity direction. The camera follows Mega at the same offset in each one.
- `./run-game-alpha.sh --level DESERT_MAN --room room13`: walk into the gate to `room14`. The camera pans, and it follows Mega after the pan.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement --weapons PRECIOUS_GUARD`: fire `PRECIOUS_GUARD`, then run and jump. The cluster follows Mega at the same rate.
- `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_corridor`: the scroll starts when Mega enters its section, and it stops at the far edge.
- `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_lock`: the view locks when Mega enters the zone, and it unlocks when Mega leaves.

Experimental: the game state listener:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room heart`: pick up the `HeartTank`. The max health goes up by the same amount.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room pickups`: pick up the `Screw`. The pickup sound plays, and the currency goes up.
- `./run-game-alpha.sh --level DESERT_MAN --room room16`: finish the stage. The get-weapon screen shows the reward weapon.

Control:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die. After the beam-in, the camera is on Mega.
- `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_gravity`: switch the gravity during the scroll. The scroll and the follow act as before.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: play through several rooms.

## Behaviors to check

Control:

- A save load: `./run-game-alpha.sh`: load a save that holds a heart tank, and start a stage. The health bar has the same length.
- The start of a new game after a loaded save: the health bar goes back to the base length.

## Intended changes

None.
