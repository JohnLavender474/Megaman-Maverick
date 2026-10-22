# Notes: 051 X-style camera bounds, auto-scroll and other camera behaviors

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## From the old `BACKLOG.md` (2026-10-22)

Today `CameraManagerForRooms` supports only the classic Mega Man way to set camera bounds: the
bounds are the current room, and the bounds change only through a room transition.

Camera behaviors to support:

- **Room bounds** (current behavior): the camera follows the player inside the current room, and
  a room transition moves it to the next room.
- **X-style bounds:** objects in the stage change the camera bounds while the player stays in the
  same area, with no room transition.
- **Auto-scroll:** in some sections, the screen scrolls by itself and the player must keep up.
- Other behaviors that may come later, for example a locked camera for a boss fight, a camera
  that moves along a path, or a camera that follows a target other than the player.

What X6 does (from `~/IdeaProjects/mmx-demake-tools/docs/engine.md`, section "Camera", and
`docs/stages/shark-2.md`):

- Each layer camera has current limits (min/max x and y) and target limits. The current limits
  move toward the target limits at a set speed.
- Camera zones (`CameraZone`) are rectangles in the stage. While the player is in a zone, the zone
  writes new target limits. Boss gates and some doors also set target limits.
- Auto-scroll (`AutoScroll` in Commander Yammark's stage, area 1) is an object that sets
  min x = max x = camera x and moves it to the right at 0.5 px per update. So in X6, auto-scroll
  is only bounds that move, and the normal follow logic stays the same. A stage flag ends it.

Problems now:

- `MegaLevelScreen` is tightly coupled to `CameraManagerForRooms`. It sets the transition
  callbacks, reads `currentGameRoom` and `priorGameRoom`, and sends the room events
  (`BEGIN_ROOM_TRANS`, `CONTINUE_ROOM_TRANS`, `END_ROOM_TRANS`, `SET_TO_ROOM_NO_TRANS`) from those
  callbacks.
- Room logic and camera logic are mixed in one class. Music, boss rooms, gravity direction and
  enemy cleanup are tied to room transitions in the level screen.
- No logic exists for X-style camera bounds or auto-scroll.

Goal:

- An interface for camera control, so that the level screen does not depend on one camera
  manager.
- Room-based bounds, X-style bounds and auto-scroll as options that a level can use. A level can
  use more than one, for example room bounds with one auto-scroll section.

Research first (write the result in the branch `docs/NOTES.md` before any code changes):

1. **Reuse the focus logic, or make a first-class design?**
   - Option A: keep the current `IFocusable` follow logic. For auto-scroll, the camera follows an
     invisible focusable object that moves at a set speed.
   - Option B: make each behavior first-class, as its own class, function or branch of logic.
   - Option C (the X6 way): keep one follow logic, and express each behavior as changes to the
     camera bounds (for example, auto-scroll is bounds that move).
   - For each option, find out: how does the player interact with the scroll (pushed by the screen
     edge, crushed against a wall, killed off screen)? How does it work with room transitions,
     checkpoints and respawn? How do level designers set it up in Tiled?
2. **One class or several?**
   - One interface with one implementation class that handles all camera behaviors.
   - Or one interface with several classes (for example one per behavior, or small parts that
     combine: a target source, a bounds source, a mover).
   - Compare: how easy it is to add a new behavior, to test a behavior alone, to switch between
     behaviors in one level, and to read the code.
3. **Who owns the room logic?** Find out which of the room-transition side effects (music, boss
   rooms, gravity direction, enemy cleanup, controller on/off) belong to the camera and which
   belong to a separate room or level system.
4. **Examples from other games and engines** for camera bounds, camera zones and auto-scroll.

Contract notes: the current room-based camera behavior must not change in any existing level.
