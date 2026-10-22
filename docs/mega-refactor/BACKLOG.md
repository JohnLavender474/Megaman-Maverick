# Backlog

Ideas that are not yet assigned to a branch. Change this file only on `master`. When a branch
takes an item, move the item to that branch `docs/TODO.md`.

## Player and multiplayer

### Remove the global `megaman`

`megaman` is reached globally through `game.megaman` (for example in `MegaLevelScreen`,
`AbstractEnemy` and `AbstractBoss`). This blocks other playable characters and multiplayer.

### Support multiple controllers and co-op

All controller and behavior logic assumes one player. Add support for more than one controller
and a co-op mode.

## Entity design

### Replace entity inheritance with components

Entities get health by extending `AbstractHealthEntity`. Enemies must extend `AbstractEnemy` and
bosses must extend `AbstractBoss`. Logic in the game does type checks on these classes. Replace
the inheritance layers with components (for example a health component) and remove the type
checks.

### Reduce repetitive object-oriented patterns

Some patterns repeat in many classes. Look at where functional code, plain tables or config
files are a better fit than classes.

### Define entities in data files

Let other developers and level creators build new entities from JSON or YAML, without deep work
in the code.

## Camera

### Support X-style camera bounds, auto-scroll and other camera behaviors

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

## Configuration

### Make values configurable

Many values are hard-coded. Move them to config files where it helps.

## Events

### Clean up event-driven logic

`MegaLevelScreen` listens to many events, and some events must follow others in a fixed order.
Remove the fragile ordering. Replace events that are better as flags in a table.

## Platforms

### Support web browsers and Android

Make the game playable in a web browser and on Android phones.

## Testing

### Record and replay for contract checks

Make it possible to check the contract with less manual work.

Approach under discussion:

- Drive a run with recorded inputs only.
- Record a full trace of each frame from the run on `master` and from the run on the branch, then
  compare the two traces frame by frame.
- Compare at levels. A strict level checks what the player sees and hears (positions, health,
  current room, sounds, drawn output). A detailed level shows events and internal state, to find
  the cause of a failure.

Requirements:

- The recorder can capture any relevant data, not only a fixed list.
- Recording must not lower the FPS. One option is an in-memory buffer that a background thread
  writes to a file in a compact binary format.

Required first (determinism):

- A seeded random source. Many files use `getRandom`, `shuffle` or `.random()`.
- A fixed timestep in replay mode. The frame delta comes from `Gdx.graphics.deltaTime` today.
- Synchronous pathfinding in replay mode. `AsyncPathfindingSystem` runs on other threads.
