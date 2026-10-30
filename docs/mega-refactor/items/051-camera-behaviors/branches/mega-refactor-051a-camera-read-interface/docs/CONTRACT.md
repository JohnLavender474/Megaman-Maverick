# Contract: mega-refactor-051a-camera-read-interface

The gameplay that this branch must not change, and how to check it. Scope: `docs/SCOPE.md` (approved 2026-10-30).
The branch changes the type through which entities read the game camera (the engine interface), the name of the read
(`getGameCamera()` → `gameCamera`), and where the pooled forms of 3 camera reads live (`core` extensions on the
interface). It does not change the camera object, when the camera moves, or any value that a read returns. So every
check below looks for "the same as `master`".

Rooms come from the live TMX files that `LevelDefinition` names (a room scan on 2026-10-30). Each `--room` has a player
spawn.

## Automated checks

- [x] `./gradlew engine:test core:test lwjgl3:jar` passes (the CI job).
- [x] No `getGameCamera` is left in `core`, `lwjgl3` and the tests, except the commented-out line in
      `GravityCapsule.kt`: `grep -rn "getGameCamera" core lwjgl3 --include=*.kt` finds only that line.
- [x] Check script (scratch; the output goes in the process log): each changed line of the mechanical commit is the
      same line with `getGameCamera()` replaced by `gameCamera`, and nothing else.
- [x] By name, each of the 4 reads resolves to the intended function (the risk in `SCOPE.md`). Every call goes through
      `services.gameCamera`, whose static type is `ICameraView`, so a call can resolve only to an `ICameraView` member
      or extension, not to a `Camera` extension. The 2-argument `overlaps` calls (`DragonFly`, `IceCubeMaker`) resolve
      to the member. The temporary suite checks the values of all 4.
- [x] Temporary suite `Temp051aCameraReadEquivalenceTest`, with its frozen copy `Temp051aFrozenCameraReads` (core test
      sources), passes. The frozen copy has the old `RotatableCamera.getRotatedBounds()`, the old
      `Camera.toGameRectangle(out)` and the old `Camera.overlaps(bounds)` (frustum). On a `RotatableCamera` with seeded
      positions, each of the 4 directions (after `immediateRotation`) and seeded test rectangles (inside, outside, on
      the edge), each new read through the interface gives the same value as the frozen read: the same rectangle
      (`x`, `y`, `width`, `height`) and the same `overlaps` result. Also `direction` through the interface is the
      camera's `direction`. The suite and the frozen copy are deleted in the last commit before the merge.
- [x] Permanent test: the host's `gameCamera` exists when the game is made, is the same instance on each read, and
      starts with direction `UP`. The holder build and the pixel-perfect toggle need a running game: `postCreate()`
      passes the field (the build checks the type), and manual check 8 covers the toggle.

## Manual playthrough

The owner plays these once, after the implementation. The commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-051a-camera-read-interface`

Short pass of a real stage (the culling of about 30 entity kinds reads the camera):

1. `./run-game-alpha.sh --level INTRO_STAGE --room room1`: play to the boss gate. Enemies spawn when they come on
   screen and leave when they go off screen as before; enemies that go off screen and come back respawn as before;
   room transitions.

Targeted checks:

2. `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: the gravity switch turns the camera (`Megaman` reads the
   camera direction to ask for the turn); after the turn, the camera eases back to Megaman; the gravity blocks of each
   group start to move only when their group comes near the camera.
3. `./run-game-alpha.sh --level WILY_STAGE_1 --room room8`: the dark room; lights and shots light up the dark only
   near the screen, as before; leave the room and come back.
4. `./run-game-alpha.sh --level RODENT_MAN --room room13`: the disappearing blocks play their sound only when they are
   on screen.
5. `./run-game-alpha.sh --level INFERNO_MAN --room room5`: go right into `room6`. The lava river rises and stays
   below the camera as before.
6. `./run-game-alpha.sh --level INFERNO_MAN --room room9`: the death sensors in this room kill Megaman only when they
   are on screen; go into `room10`, where the lava river rises as before.
7. `./run-game-alpha.sh --level TIMBER_WOMAN --room room8`: the flipper platforms flip and are culled as before.
8. Pause, open the settings panel and turn pixel-perfect off and on (any level, for example check 1). The view stays
   the same, and enemies on screen are not culled.

Play test result (2026-10-30): the owner (verbatim): "All tests pass". All manual checks pass.

## Behaviors to check

- Death and respawn (check 1, or a pit in check 5): after the respawn, enemies near the camera spawn as before.
- A second level in one session: after check 1, exit to the menus and start another level; it plays and culls as
  before.

## Intended changes

None.
