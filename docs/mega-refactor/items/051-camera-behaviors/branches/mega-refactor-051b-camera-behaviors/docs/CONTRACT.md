# Contract: mega-refactor-051b-camera-behaviors

The gameplay that this branch must not change, and how to check it. Scope: `docs/SCOPE.md` (approved 2026-10-30).
The branch moves the camera logic of `CameraManagerForRooms` and `MegaLevelScreen` into a controller with parts, and
the room side effects into a room handler. It does not change a camera position, a room choice, a timing, or the
order of an event. So every check below looks for "the same as `master`".

Rooms come from the live TMX files that `LevelDefinition` names (a room scan on 2026-10-30). Each `--room` has a player
spawn.

## Automated checks

- [x] `./gradlew engine:test core:test lwjgl3:jar` passes (the CI job).
- [x] No unique compiler warning is new compared with the baseline.
- [x] `grep -rn "CameraManagerForRooms\|focusSnappedAway\|coerceIntoBounds" core engine lwjgl3 --include=*.kt` finds
      nothing, and the room handler and the controller are the only callers of the room and pan parts.
- [x] Temporary suite `Temp051bCameraEquivalenceTest`, with its frozen copy `Temp051bFrozenCameraManager` (core test
      sources), passes. The frozen copy has the old `CameraManagerForRooms`, the old `coerceIntoBounds`, the shaker
      update and the flag clear, in the old frame order. Old and new run side by side on seeded room layouts and
      seeded focus paths, and after each frame the suite compares the camera position, the current and prior room,
      `transitioning`, `transitionInterpolation`, and the ordered list of callbacks. The paths cover:
      - a walk across rooms in each of the 4 directions;
      - a gap between rooms (the focus in no room, the x-only follow in the prior room);
      - a requested transition, with and without a direction;
      - a reset, also in the same frame as a transition and during a pan;
      - start and stop of the smooth move, also near the 0.25-tile stop rule;
      - a shake during a pan and during the smooth move;
      - each of the 4 camera directions (the swapped viewport size in the clamp);
      - no focus, and no rooms.
      The suite and the frozen copy are deleted in the last commit before the merge.
- [x] Check script (scratch; the output goes in the process log): the 5 callback bodies in `LevelRoomHandler` are the
      same lines as in `MegaLevelScreen` on `master`, except the names of the receivers.
- [x] Permanent tests: `CameraFollow` (the x clamp, the smooth-move lerp and stop rule), `CameraRoomBounds` (the clamp,
      the 4 directions, the prior-room case), `CameraRoomTransition` (the delay, the duration, the target, the
      callback order), `CameraController` (the order of the parts; a controller with only a follow part works).
- [x] `LevelRoomHandlerTest`: with fake rooms, the end of a transition submits `ENTER_BOSS_ROOM` for `event=boss`,
      `VICTORY_EVENT` for `event=success`, `TURN_CONTROLLER_ON` with no event, and a `megaman_direction` room sets the
      gravity.

## Manual playthrough

The owner plays these once, after the implementation. The commands run from:

`cd /home/johnlavender/IdeaProjects/Megaman-Maverick/.worktrees/mega-refactor-051b-camera-behaviors`

Short pass of a real stage:

1. `./run-game-alpha.sh --level INTRO_STAGE --room room1`: play to the boss gate. The camera follows and stops at the
   room edges; each room transition has the same pause and pan, Megaman moves into the next room, enemies are removed,
   music as before. The gate opens and the transition through it works. Die in a pit: the camera resets at the respawn.

Targeted checks:

2. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room basic`: walk right through the transition and back; up and
   down through the vertical rooms. The camera clamps to each room.
3. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room gap_left`: walk into the gap between the rooms. The camera
   follows in x only and stays clamped to the prior room; it picks up the next room when Megaman reaches it.
4. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room to_gravity`: enter the next room. At the end of the transition
   the gravity changes, the aura shows and the sound plays.
5. `./run-game-alpha.sh --level REFACTOR_TEST_051 --room to_success`: enter the next room. The victory sequence starts.
6. `./run-game-alpha.sh --level WILY_STAGE_3 --room room5`: the gravity switch turns the camera, and the camera eases
   back to Megaman after the turn.
7. `./run-game-alpha.sh --level MOON_MAN --room room14`: go to the boss. The room transition, the boss intro with no
   controller, the fade of the music as before.
8. `./run-game-alpha.sh --level MOON_MAN --room room6`: the gravity switches in rooms 8 to 10 turn the camera as
   before, and room transitions work with a turned camera.
9. `./run-game-alpha.sh --level SMB3_1 --room room2`: go to a pipe and enter it. The camera eases to Megaman at the
   pipe exit and does not jump.
10. `./run-game-alpha.sh --level INFERNO_MAN --room room5`: go right into `room6`. The camera shakes with the lava
    river as before, and it stops at the same place.
11. `./run-game-alpha.sh --level WILY_STAGE_2 --room fork_room`: take one path; room transitions as before.
12. Any level with a ladder (for example check 1): climb a ladder to the top. The camera eases to Megaman as before.
13. Pause in a room transition (check 1), then resume. The pan goes on from the same place.

## Behaviors to check

- Death and respawn, also during a shake (check 10): the shake stops, and the camera resets to the spawn.
- A second level in one session: after check 1, exit to the menus and start another level. The camera, the rooms and
  the music start as before.

## Intended changes

None.
