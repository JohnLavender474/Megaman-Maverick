# Plan: mega-refactor-047h-side-exits

Each step has its own check. Mark a step `[x]` when its check passes.

1. [x] **Side exits.** `SideExit` and `findSideExits(room, rooms, out)` in `screens/levels/camera/CameraRoomExits.kt`:
   for the left and right edge, each room whose opposite edge touches it (0.5 px) with an overlapping vertical span.
   Reuses the entries of `out`. Check: tests for a touch on each side, a gap, a partial span, two rooms on one side.
2. [x] **The part.** `CameraRoomExits(players, roomBounds, enabled, held)`:
   - exits found again only when the current room changes;
   - with 2 or more live players (not held), one `CameraEdgeBlock` 1 tile outside each exit over its span; none on an
     exit that a gate rectangle covers (`gates`, filled at level start), with a `TODO` for phase 4;
   - a player is at an exit when its body reaches the exit edge (within 0.1 tile, or past it) and its center is in the
     span; when every live player is at one exit, the blocks go and `transitionToRoom(camera, next, side)` runs;
   - `reset()` removes the blocks.
   Check: tests for the walls (2 live, 1 live, off, gate), the gather (both, one away, held, dead, past the edge).
3. [x] **Setup.** `MegaLevelScreen.init()`: built with `CameraViewEdges` (2 players), `addEffect` after it; on =
   slot 0 in play, no transition, no turn, direction `UP` (`TODO` for turned rooms). `buildLevel`: the `Gate` rectangles
   of the `sensors` layer. A `TODO` at the part for vertical exits (phase 3). Check: `lwjgl3:jar` builds.
4. [x] **Automated checks.** `engine:test`, `core:test`, `core:smokeTest`, `lwjgl3:jar`; mutation checks on the gather
   and the wall rules.
5. [x] **Review before the play test.** Subagents: the diff, and every manual check (rooms, spawns, a real gate room
   with 2 players). No game launch.
6. [x] **Play checks** to the owner.
