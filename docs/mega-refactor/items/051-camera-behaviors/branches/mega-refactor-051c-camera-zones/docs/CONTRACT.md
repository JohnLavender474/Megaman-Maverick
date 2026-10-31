# Contract: mega-refactor-051c-camera-zones

- Status: approved
- Approved: 2026-10-30

The gameplay that this branch must not change, and how to check it. Scope: `docs/SCOPE.md` (approved 2026-10-30).

A level with no `camera_sections` layer must play the same as `master`: the same camera position, room choice, timing
and event order. Zones, edge blocks and crush by an edge block are new. They act only where a level asks for them.
`GravityBlock` moves to `CrusherComponent` and kills in the same cases as before.

Rooms come from the live TMX files that `LevelDefinition` names (a room scan on 2026-10-30). Each `--room` has a player
spawn. The `zone_*` and `edge_*` rooms in `RefactorTest051.tmx` are new in this branch.

## Automated checks

- [x] `./gradlew engine:test core:test lwjgl3:jar` passes (the CI job).
- [x] No unique compiler warning is new compared with the baseline.
- [x] `CameraRoomClamp` no longer calls `scanForNextRoom`, and `grep -n "roomBounds.reset()" MegaLevelScreen.kt` finds
      nothing: all resets go through `CameraController.reset()`.
- [x] No limit value goes through `coerceIn`, because it throws when min > max.
- [x] Temporary suite `Temp051cCameraEquivalenceTest`, with its frozen copy `Temp051cFrozenCameraManager` (core test
      sources), passes. The base is the `051b` suite (commit `b7fb77bb8`). Old and new run side by side on seeded room
      layouts and seeded focus paths, with no zones. After each frame the suite compares the camera position, the
      current and prior room, `transitioning`, `transitionInterpolation`, and the ordered list of callbacks. The paths
      cover all `051b` cases:
      - a walk across rooms in each of the 4 directions;
      - a gap between rooms (the focus in no room, the x-only follow in the prior room);
      - a requested transition, with and without a direction;
      - a reset, also in the same frame as a transition and during a pan;
      - start and stop of the smooth move, also near the 0.25-tile stop rule;
      - a shake during a pan and during the smooth move;
      - each of the 4 camera directions (the swapped viewport size in the clamp);
      - no focus, and no rooms.
      And these new cases:
      - the end of a pan where the cross axis must snap into the next room, seeded from live pairs (`DesertMan` `room9`
        to `room10`, `MoonMan` `room12` to `room13`);
      - the reset frame, which has no clamp;
      - a room smaller than the view, where min wins;
      - a rotated pan that ends 1 tile off the clamp.
      The suite and the frozen copy are deleted in the last commit before the merge. A mutation check proves that the
      suite catches a defect in the new stage.
- [x] Temporary suite `Temp051cGravityBlockCrushTest`: the frozen old `GravityBlock` check and `CrusherComponent` give the
      same kill result on seeded states. Deleted in the last commit before the merge.
- [x] Permanent tests:
      - the fold: Tiled order, per edge, a later object wins, and the same position gives the same limits on any path;
      - ranges: a limit object sets the low or the high edge, one object can set both edges and lock the view, a later
        zone beats a range, and min wins in an empty range;
      - the snap and ease rule: a room change, a reset, and the focus out of view snap; other changes ease; a shaken
        position does not move a limit;
      - the room cut: in a room, and in a gap with the prior room;
      - `CameraController.reset()` resets each `Resettable` part;
      - a controller with no zone part gives the same result as a controller with no zones;
      - `CrusherComponent`: the side, the active predicate, the opposite solid, the rotated player, and `instant`;
      - `GravityBlock`: not on the ground and overlapping the head kills; on the ground does not; invincibility does not
        protect;
      - `CameraEdgeBlock`: a player collides with it, and a dynamic enemy does not. `EntityConstructorsTest` covers the
        new class.

## Manual playthrough

The owner plays these once, after the implementation. The commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-051c-camera-zones`

Levels with no zones play the same:

1. `./run-game-alpha.sh --level INTRO_STAGE --room room1`: play to the boss gate. The camera follows and stops at the
   room edges; each transition has the same pause and pan. Die in a pit: the camera resets at the respawn.
2. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic`: walk right through the transition and back, then climb
   into the room above.
3. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room gap_left`: walk into the gap. The camera follows in x only, and
   it picks up the next room with no transition.
4. `./run-game-alpha.sh --level DESERT_MAN --room room9`: go right into `room10`. The camera y moves into the new room as
   before.
5. `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: the gravity switch turns the camera, and the camera eases back
   to Megaman.
6. `./run-game-alpha.sh --level INFERNO_MAN --room room5`: go right into `room6`. The camera shakes as before.

`GravityBlock` kills in the same cases:

7. `./run-game-alpha.sh --level MOON_MAN --room room6`: go to rooms 8 to 10. A falling `GravityBlock` kills Megaman when
   it hits his head; a block at rest does not. `room6` is the nearest room with a spawn.
8. `./run-game-alpha.sh --level REFACTOR_TEST_029 --room player_death`: the `GravityBlock` there acts as in check 7.

Basics, one feature at a time:

9. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_lock`: inside the zone the camera does not move on the
   locked axis. On the other axis it follows. Leaving the zone eases the camera, with no jump. Leave the zone, then turn
   back while the camera eases: the camera does not jump.
10. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_hold`: walk right. The camera follows, then stops at the
    limit while Megaman walks on in view. Past the end of the zone the camera eases back to him and follows. Walk back
    left: the camera stops at the same place.
11. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_block`: the zone limit is in open space and the block holds
    Megaman in view. Megaman cannot wall-slide on it. An enemy and a shot go through it.
12. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_room`: the zone limit is at the room edge. Megaman reaches
    the next room, and the transition starts.

Zones with other camera features:

13. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_pipe`: take the pipe to a far zone in the same room. The
    camera cuts to Megaman, and he is in view at once.
14. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_gap`: walk out of a room with an active zone into a gap and
    back. The view does not slide with no cause.
15. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_pan`: go to the next room from inside a zone. The pan is
    the normal pan, and the view is correct after it.
16. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_shake_entry`: walk right into `zone_shake` and do not stop
    until the zone limit starts to ease. A shake during an ease does not jolt the limit.

Edge cases:

17. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room zone_overlap`: 2 zones overlap, and the left stop comes from
    `zone_overlap_a` and the right stop from `zone_overlap_b`. Walk left: the camera stops with its center near tile 20,
    and it releases past tile 13. Walk back right: it eases slowly to the stop (`speed=2`). Walk right: it stops with its
    center near tile 34, and it releases past tile 39. Stop on a tile, wait up to 3 s for the view to settle, then reach
    the same tile from the other side and wait again: the view is the same. Jump into the `Death` sensor above the
    overlap: the respawn shows the same view at once, with no ease.
18. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_crush`: a narrowing zone pins Megaman against a wall. He
    dies at once, also while invincible after a hit. He does not pass through the wall.
19. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room edge_rotated_entry`: walk right into `edge_rotated`, with no crash
    at the end of the transition, then repeat the pin with gravity rotated. The crush acts on the correct side.

## Behaviors to check

- Death and respawn inside a zone: the limits take the target at once, with no ease.
- A second level in one session: after check 17, exit to the menus and start another level. No zone, limit or edge block
  stays.
- Pause during an ease: the ease goes on from the same place.

## Intended changes

None for a level with no `camera_sections` layer. `GravityBlock` keeps its behavior.
