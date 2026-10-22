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

### Support X-style camera bounds and decouple the camera manager

Today `CameraManagerForRooms` supports only the classic Mega Man way to set camera bounds: the
bounds are the current room, and the bounds change only through a room transition.

In X6, objects in the stage control the camera bounds. The bounds can change while X stays in
the same area, with no room transition. See the "Camera" section in
`~/IdeaProjects/mmx-demake-tools/docs/engine.md`: each layer camera has current limits and target
limits, and the current limits move toward the target limits at a set speed.

Problems now:

- `MegaLevelScreen` is tightly coupled to `CameraManagerForRooms`. It sets the transition
  callbacks, reads `currentGameRoom` and `priorGameRoom`, and sends the room events
  (`BEGIN_ROOM_TRANS`, `CONTINUE_ROOM_TRANS`, `END_ROOM_TRANS`, `SET_TO_ROOM_NO_TRANS`) from those
  callbacks.
- Room logic and camera logic are mixed in one class. Music, boss rooms, gravity direction and
  enemy cleanup are tied to room transitions in the level screen.
- No logic exists for X-style camera bounds.

Goal:

- An interface for camera control, so that the level screen does not depend on one camera
  manager.
- Room-based bounds (the current behavior) and X-style bounds (set by objects in the map) as two
  options. A level can use either one, and possibly both.

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
