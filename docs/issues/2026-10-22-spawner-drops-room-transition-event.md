---
status: closed
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [019]
---

# An event spawner may drop `BEGIN_ROOM_TRANS` for enemies that the room transition destroyed

## Where

`MegaLevelScreen.kt:296`, `SpawnerForEvent.onEvent`, `SpawnersLayerBuilder.kt:113-121`

## What happens

`beginTransition` destroys all enemies inside `cameraManagerForRooms.update` and submits `BEGIN_ROOM_TRANS`. Their `SpawnerForEvent` still holds a spawn when the event is delivered, so it drops the event. A `spawn_room` enemy whose room list has both the old and the new room may not respawn.

## What should happen

The spawner learns about the destroy at once (`019`).

## How to reproduce

Find a `spawn_room` enemy whose list names two adjacent rooms. Go from one room to the other. Found by reading; needs a check in the game.

## Notes

The decision K research found no live case that changes with the current room predicates.

## Resolution

Fixed by `mega-refactor-019b-destroy-notification` (squash `7a30c8982`, 2026-10-27): a spawner lets go of its spawn at
the `destroy()` call, so it is free when `BEGIN_ROOM_TRANS` is delivered. `019b` intended change 4 names this case,
and the `SpawnerReleaseTest` case "accepts the events of the frame in which its entity is destroyed after the spawn
pass" proves it. No live map has a `spawn_room` entity that lists two rooms, so no game check can show it. Closed at
the `mega-refactor-019c-merge-spawn-queues` merge (2026-11-03).
