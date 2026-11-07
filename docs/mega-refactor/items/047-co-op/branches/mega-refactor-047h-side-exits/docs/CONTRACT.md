# Contract: mega-refactor-047h-side-exits

- Status: approved
- Approved: 2026-11-06

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with new tests:
  - the side exits of a room: a touching room on each side, no exit for a gap, a partial vertical span, two rooms on
    one side;
  - the exit walls: placed with 2 live players, none with 1 live player, none when off, removed at a transition;
  - the gather: the transition starts only when every live player is at the same exit; a held or dead player does not
    count; a player who walks away stops it.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

Run from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-047h-side-exits`.

Experimental checks (2 players):

- `./run-game-test-coop.sh --room coop_start`: slot 1 runs right and stops at the room edge; slot 0 also stops there
  alone. When both touch the edge, the transition to `coop_wide` starts, and both arrive.
- `./run-game-test-coop.sh --room coop_wide`: walk back left to `coop_start`: the same rule at a left exit. Then go
  right into `coop_shaft`.
- `./run-game-test-coop.sh --room coop_shaft`: drop down; at the bottom right exit, the same rule into `coop_rest_1`.
- `./run-game-test-coop.sh --room coop_hscroll`: during the scroll, the scroll's right edge block holds the players; after
  the scroll stops, the right exit holds both players until both are at it.
- `./run-game-test-coop.sh --room coop_vscroll`: at the top right exit into `coop_rest_2`, the same rule. Slot 0 waits on
  the top platform: the up scroll kills slot 0 below its bottom edge, as before this branch.
- `./run-game-alpha.sh --level GLACIER_MAN --room room11 --players 2`: a real exit with no gate: walk left into `room10`;
  both players must be at the exit.
- `./run-game-test-coop.sh --room coop_start`: slot 0 stands at the exit, then walks away; then slot 1 walks to the exit
  alone: no transition.
- `./run-game-test-coop.sh --room coop_start --viewEdges right=WARP`: no effect at the room edge; the walls hold.

Control checks (behavior that must not change):

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room coop_start`: 1 player. The transition starts when Megaman
  walks out, as before. No walls.
- `./run-game-test-coop.sh --room group_wide`: the `047g` view edge walls act as before.
- `./run-game-alpha.sh --level DESERT_MAN --room room14 --players 2`: gates on both exits; no exit walls; the gate
  opens and the transition runs as today.

## Behaviors to check

- `./run-game-test-coop.sh --room coop_wide --viewEdges left=KILL`: slot 0 waits at the right exit; slot 1 walks out of
  the view to the left and dies; then slot 0 walks into `coop_shaft` alone.

## Intended changes

- With 2 players, a side room exit holds every player until all live players are at it. The `047a` check
  `--room transition --players 2` no longer lets slot 0 leave alone.
