# Contract: mega-refactor-084g-movement-state

The gameplay that this branch must not change, and how to check it. Write this file during
planning, before you change code. Do the checks before the branch merges, and write the result
in the branch process log.

- Status: approved
- Approved: 2026-11-01

The branch merges the 2 run functions and moves the run state, the crouch timers and 2 bag values. Run, crouch, air dash, the animations and the shot position must act the same, in the same frames.

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with these tests:
  - The run function: for each of the 4 directions and both sides, the velocity change, the facing and `running`. These tests are written on the old `runLeft` and `runRight`, and their expected values do not change.
  - The run ramp: the 3 `runTime` scalars on the ground, and no scalar in the air.
  - The run guard: each stop condition stops the run, and the speed limit stops the impulse.
  - The `RODENT_CLAWS` difference: with both buttons pressed during a shot, the facing goes right, and it does not go left.
  - `RunComponent.reset()` clears both values. `running` is false out of play and on an entity with no component.
  - The crouch delays: a crouch starts only after both delays end, and a release of `DOWN` or a ground slide restarts its delay.
  - `LevelRoomHandler`: the end of a room transition stops the run of each player, with no `Megaman` cast.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] 15 casts to `Megaman` remain: `grep -rnE '\bas\??\s+Megaman\b' core/src/main/kotlin` has 15 matches.
- [ ] The old names are gone: `grep -rnE 'MegamanKeys|ConstKeys\.ANIMATION_KEY|const val ANIMATION_KEY|runningFlag|fun Megaman\.run(Left|Right)|groundSlideToCrouchDelay|buttonToCrouchDelay' core/src/main/kotlin` finds nothing outside `CrouchBehavior.kt`.
- [ ] A short launch of `REFACTOR_TEST_029 --room movement` starts with no exception, at the end of the branch.

## Frame order

No line moves. Each actuator calls the run function at the same place, and the left actuator runs before the right one. The run function does the checks in the old order. `:530` reads `running` at the same place. The `reset()` of `RunComponent` replaces the 2 `onSpawn` lines, which act before the next frame of a player in either form. Review the final diff for any other change of order. Lines are at `c6b8cecf7`.

## Manual playthrough

Run the commands from the worktree:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-084g-movement-state`

Experimental: the run function:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: run left and right in each gravity direction. Mega moves the way the button says, at the same speed, and with the same start ramp.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: run both ways, press both buttons, and release one. Run into a wall. Run, then jump, and steer in the air.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: run on ice. The slide and the speed are the same.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement --weapons RODENT_CLAWS`: slash on the ground, and press left, right and both buttons during the slash. Mega stops, and the facing changes as before. With both buttons pressed, Mega faces right.

Experimental: the run state and the typed values:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: fire while you run and while you stand. The shot starts at the same place, and the run animation plays.
- `./run-game-alpha.sh --level DESERT_MAN --room room13`: hold a direction through the gate to `room14`. After the pan, Mega does not keep the run animation with no input.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: die during a run. After the beam-in, Mega stands, and a new run starts the ramp again.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room gravity_switch`: air dash in each gravity direction, and switch the gravity during a dash. The sprite keeps the direction of the start of the dash.
- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room trail`: the trail sprites show the same frames as Mega.

Experimental: the crouch timers:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: hold `DOWN` to crouch, with the same delay. Ground slide, then keep `DOWN` held: the crouch comes after the same delay.

Control:

- `./run-game-alpha.sh --level REFACTOR_TEST_029 --room stun`: a stun stops the run, and the run comes back after it.
- `./run-game-alpha.sh --level REFACTOR_TEST_084 --room knockback`: a hit during a run acts as before.
- `./run-game-alpha.sh --level GLACIER_MAN --room room1`: play through several rooms.

## Behaviors to check

Control:

- The pause menu: `./run-game-alpha.sh --level REFACTOR_TEST_029 --room movement`: pause during a run, and resume. Input works, and the run goes on if the button is held.

## Intended changes

None.
