# Contract: mega-refactor-047i-transition-path

- Status: approved
- Approved: 2026-11-17

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew engine:test` includes `CameraRoomTransitionTest`: `start` sets `axisOnly`; the end of the pan and
  `cancel()` clear it; a start with no value gives `false`.
- [ ] `./gradlew core:test` passes, with the new tests: a gathered exit starts an axis-only pan; the transition
  events carry the value; a player with the value keeps its own y; a player without it takes the shared point.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] `./gradlew detektMain core:verifyRandomRule` passes.
- [ ] The review before the play test finds that every manual check compiles, launches and runs as written. No game
  launch.

## Manual playthrough

Run from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-047i-transition-path`.

Experimental checks (2 players):

- `./run-game-test-coop.sh --room coop_ledge`: slot 0 stands on the ledge at the right exit, slot 1 stands on the floor at the exit. In the transition to `coop_ledge_next`, each player keeps its own height; the high player falls to the floor at the end.
- `./run-game-test-coop.sh --room coop_start`: slot 0 stands at the right exit; slot 1 jumps into the exit wall. The
  transition starts while slot 1 is in the air. Slot 1 pans at its own height and falls at the end; it does not jump
  to slot 0.
- `./run-game-test-coop.sh --room coop_wide`: walk back left to `coop_start` with the same jump: the same rule at a
  left exit.

Control checks (behavior that must not change):

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room coop_start`: 1 player. Jump out of the right exit: the
  transition is as before.
- `./run-game-alpha.sh --level DESERT_MAN --room room14 --players 2`: a gate with 2 players: both players move to one
  point, as before.

## Intended changes

- At a gathered side room exit with 2 players, each player keeps its own height through the transition. Before, every
  player moved to one point at the height of the camera subject.
