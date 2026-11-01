# Contract: mega-refactor-051d-auto-scroll

- Status: approved
- Approved: 2026-10-31

Levels with no scroll section play the same. `051c` zones and edge blocks act the same when no section is active. New
behavior acts only in a level with `type=scroll` objects.

## Automated checks

- [x] `./gradlew engine:test core:test lwjgl3:jar` passes (the CI job).
- [x] No unique compiler warning is new compared with the baseline.
- [x] Permanent tests. The hold test covers one predicate; the list of holds is in `MegaLevelScreen`, so manual checks 5, 6 and
      8 prove each hold in play:
      - the start: the section starts when the focus enters it, from the current camera position, with no jump;
      - the scroll: each of the 4 directions, the speed, and the other axis keeps its follow and its limits;
      - a shake does not add to the scroll position;
      - the stop: mode `exit`, mode `arrive`, and the trailing block and limit stay after the stop;
      - the edges: the block covers the section rectangle, the leading block turns off at a room edge, the trailing block
        stays on, a per-section `edge_blocks`
        overrides the defaults, and a trailing edge in the direction of gravity kills;
      - the room rules: the section waits at a room edge, and it continues from the camera after a pan;
      - the view size: it blends over a turn, a pinned axis stays on the scroll position, and a range axis obeys the hold;
      - the holds: the scroll waits for each hold, and it runs while a player spawns;
      - the respawn: the limits clear, and a section counts as done if the camera at the spawn is past its stop;
      - the crush: only with a surface of an active section, and past the trailing edge of a running section;
      - the fall edge: no block, a kill past it, not at a room edge when it leads, each case of the 4 by 4 table, and `edge_blocks`;
      - the chain: `next`, and an overlapping section with no `next` starts in Tiled order;
      - a controller with no section gives the same result as a controller with no scroll part.

## Manual playthrough

The owner plays these once, after the implementation. New behavior comes first, and the control checks come last. The
commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-051d-auto-scroll`

Basics, one feature at a time:

1. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_corridor`: walk right to the marked spot. The scroll starts
   with no jump and runs right at a constant speed to the limit, and then the camera stays there. Low floor blocks can be
   jumped. A block with a red crush surface that pins Megaman against the trailing edge kills him at once. Enemies and hazards in the corridor
   act as normal, and what the scroll leaves behind goes away. The leading edge holds Megaman in view, and he cannot go
   back past the trailing edge after the stop.
2. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_turn`: the scroll runs right, and at the corner it hands
   over to a scroll up with no pause and no jump. In the scroll up, a fall below the view kills Megaman.

Scrolls with other features:

3. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_wide`: a section inside a wider room. Before the section the
   camera follows as normal. After the stop the follow takes control with an ease.
4. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_wall`: a wall in the path. Megaman dies against the
   trailing edge if he stays at the red surface, and the opening lets him through.
5. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_gate`: the scroll waits during a gate and then continues.
6. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_gravity`: a gravity switch in a section. The scroll waits for
   the camera turn and then continues in the same world direction. Megaman falls toward the leading edge,
   which has no block, and a fall past it kills. The trailing edge stays a block.
7. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_rooms`: a section across 2 rooms. The scroll waits at the
   room edge, the normal pan runs, and the scroll continues from the camera.
8. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_spawn`: spawn inside a section. The scroll waits until the
   player is ready, with no jump, and no edge kills before that. Die, and the camera resets to the spawn.
9. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_spawn_past`: spawn past a stop. The section stays done, and
   its trailing limit is in place.
10. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_pit`: a section over a pit. A fall kills as normal, and the
    bottom edge does not hold Megaman up.

Other directions, and scrolls after a `GravitySwitcharoo`:

17. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_left`: walk left. The scroll runs left, with the block on the right edge
    behind Megaman, and it stops at the far edge.
18. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_up`: climb. The scroll runs up, and a fall below the view kills.
19. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_down`: drop down the platforms. The scroll runs down. The top edge is
    a block, and a red surface on a platform crushes him under it. The bottom edge has no block, and a fall past it kills.
20. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_flip_right`: jump into the `down` switch, then walk right along the
    ceiling. The camera turns, and the scroll runs right after the turn. Left and right on the screen stay correct.
21. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_flip_up`: touch the `down` switch. He falls up. The scroll runs up. The top
    edge has no block, and a fall past it kills.
22. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_flip_down`: touch the `down` switch at the start. The scroll runs down.
    A fall past the top of the view kills.
23. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_side_up`: touch the `right` switch, then walk up the left wall. The
    scroll runs up, in the same world direction, and the side blocks hold him.
24. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_side_down`: touch the `left` switch, then walk down the right wall. The
    scroll runs down.
25. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_switch_mid`: during the scroll, touch the `down` switch, then the `up`
    switch. The scroll waits for each camera turn, then continues from the same place with no jump.

26. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_switch_sides`: during the right scroll, use the `left` switch and fall right
    onto a pillar, use `up`, then use `right` and fall left onto a pillar, then `up`. The scroll waits for each camera turn, the edge rule
    follows gravity, and a fall past the leading edge kills.
27. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room scroll_switch_vertical`: during the up scroll, use the `down` switch and move
    along the undersides, then use the `up` switch. The bottom and top edges swap their block and kill roles at each switch.

Control checks:

11. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_lock`: the zone acts as in `051c`.
12. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_block`: the block holds Megaman in view, as in `051c`.
13. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_crush`: a narrowing zone kills as in `051c`.
14. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic`: walk right through the transition and back. The camera
    acts as normal.
15. `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: the gravity switch turns the camera, and the camera eases back
    to Megaman.
16. `./run-game-alpha.sh --level INTRO_STAGE --room room1`: one short pass of a real stage. Die in a pit: the camera resets
    at the respawn.

## Behaviors to check

- Pause during a scroll: the scroll goes on from the same place.
- A second level in one session: after check 1, exit to the menus and start another level. No scroll, limit or edge block
  stays.

## Intended changes

None for levels with no scroll section.
