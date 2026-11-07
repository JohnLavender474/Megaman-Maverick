# Contract: mega-refactor-047g-shared-camera

- Status: approved
- Approved: 2026-11-06

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with new tests:
  - the group focus gives the mean of the live players, slot 0 when only slot 0 is live, and slot 0's bounds;
  - a view edge counts only inside the room (none at a room edge), and the rules do nothing when off;
  - `BOUNCE` puts a player back at the edge with a speed into the view, and the damage once per cooldown;
  - `WARP` and `KILL` act only after `delay` seconds fully out, and the timer resets when the player is back;
  - the `--viewEdges` parser: defaults for missing keys, each rule name, `delay`, `damage`, and a launch error for a bad
    key, a bad value, or the flag with 1 player.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test (see `docs/mega-refactor/CLAUDE.md`) finds that every
  manual check compiles, launches and runs as written. No game launch.

## Manual playthrough

Run from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-047g-shared-camera`.

Experimental checks (2 players):

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_wide --players 2`: the camera stays between the 2
  players. Walk apart: at one view apart, each player stops at its view edge. A player cannot wall-slide or wall-jump
  on the view edge. Walk together: the view follows.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_clamp --players 2`: at the left room edge the view stops;
  the left room edge is the wall, and the right view edge is a wall only while it is inside the room.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_pit --players 2`: a player who falls in the pit dies as
  today. The walls do not stop a fall.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_tall --players 2`: default rules (`top=NONE`,
  `bottom=NONE`). One player climbs the ladder while the other stays down. The low player can leave the view.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_tall --players 2 --viewEdges top=WALL,bottom=WALL`: the
  climber stops at the top view edge; a player who drops stands on the bottom view edge (on air), and can walk and jump there.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_tall --players 2 --viewEdges top=BOUNCE,bottom=BOUNCE,damage=2`:
  a player who touches the top or bottom view edge is thrown back into the view, loses 2 health, and gets the damage
  reaction, the blink and the iframes, as from an enemy hit.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_tall --players 2 --viewEdges bottom=WARP,delay=1`: the
  climber stays in view on every frame; the player who goes fully below the view is held in place (no fall, even
  when its floor culls), a white line and circle show the warp spot, and after 1 s the player appears beside the partner.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_tall --players 2 --viewEdges bottom=KILL,delay=1`: slot 1
  fully below the view for 1 s dies. (Slot 0 the same: the level resets.)
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_wide --players 2 --viewEdges left=WARP,right=WARP`: the
  side edges are no walls; a player who walks out of the side appears beside the partner, and the camera eases (no
  snap). Both players running out at once: only one is held; the camera follows the other. Side WARP is experimental.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_pit --players 2`: let slot 1 die in the pit. The camera
  eases to slot 0, with no jump.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_wide --viewEdges bottom=WARP`: a launch error (exit
  code 2) that says the flag needs 2 players.

Control checks (behavior that must not change):

- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room group_wide`: 1 player. The camera follows as before. No walls.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room transition --players 2`: slot 0 walks into `transition_next`.
  The transition is as today (slot 1 stacks at the transition point).
- `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_block --players 2`: a room with a camera zone; no view edge
  rules; the `051` right edge block acts as before and blocks both players.
- `./run-game-alpha.sh --level DESERT_MAN --room room3`: 1 player; walk right, then up through `room4` and `room5` to
  `room6`. The camera and the transitions feel the same as before.

## Behaviors to check

- With 1 player, the camera code path is the one of today: no group focus and no group walls are built.
- Pause and respawn (slot 0 death resets the level) with 2 players: the camera starts on the group again.

## Intended changes

- With `--players 2`: the camera follows the mean of the live players, not slot 0.
- With `--players 2`: each view edge has a rule (default: left and right are players-only walls, top and bottom have none), inside the room, outside a turned room, and
  when no camera zone or scroll acts.
- New dev flag `--viewEdges` to try the rules `NONE`, `WALL`, `BOUNCE`, `WARP` and `KILL` per edge, with `delay` and `damage`.
