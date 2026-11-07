# Contract: mega-refactor-047j-coop-bottom-warp

- Status: approved
- Approved: 2026-11-06

## Automated checks

- [ ] `./gradlew engine:test` passes.
- [ ] `./gradlew core:test` passes, with tests: `GameMode.COOP` has `bottom=WARP` and the `delay` of D2; `PVP` and
  `SINGLE` keep `ViewEdgeRules()`; `--players 2` with no `--viewEdges` gives the co-op default; `--viewEdges
  bottom=NONE` turns it off and keeps the other keys; a held player with no free partner dies and is released; a held
  player whose partner is live and in view still warps.
- [ ] `./gradlew core:smokeTest` passes.
- [ ] `./gradlew lwjgl3:jar` builds.
- [ ] The review before the play test finds that every manual check runs as written. No game launch.

## Manual playthrough

Run from `cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-047j-coop-bottom-warp`.

Experimental checks (2 players, no `--viewEdges`):

- `./run-game-test-coop.sh --room coop_shaft`: one player stays on the top ledge, the other drops. The top player stays
  in view; the dropper is held below the view, and after the delay appears beside the top player.
- `./run-game-test-coop.sh --room group_tall`: the same with the ladder: the climber stays in view.
- `./run-game-alpha.sh --level PRECIOUS_WOMAN --room room2 --players 2`: one player climbs a ladder, the other stays at
  the bottom: the climber stays in view; the bottom player is held below the view and warps beside the climber after
  1 s; the `LaserBeamer`s act as before.
- `./run-game-alpha.sh --level DESERT_MAN --room room9 --players 2`: a 42-tile shaft; one player stays at the top, the
  other drops: the same.

The "no free partner" death (slot 1 dies while slot 0 is held) has no room where slot 1 can die in time; the unit tests
in `SharedViewTest` cover it.

Control checks:

- `./run-game-test-coop.sh --room coop_shaft --viewEdges bottom=NONE`: the behavior before this branch (the camera can
  lose both players).
- `./run-game-test-coop.sh --room group_pit`: a pit in a room one view tall still kills.
- `./run-game-alpha.sh --level REFACTOR_TEST_047 --room coop_shaft`: 1 player; the camera follows as before.
- `./run-game-test-coop.sh --mode pvp --room coop_shaft`: PvP keeps `bottom=NONE` (D1).

## Intended changes

- With 2 players in co-op, a player fully below the view in a tall room is held and warps beside the partner.
- A held player with no free partner dies (the level resets if it is slot 0), in place of a soft lock. A player with no
  free partner is not held at all, so a lone player acts as with `bottom=NONE`.
