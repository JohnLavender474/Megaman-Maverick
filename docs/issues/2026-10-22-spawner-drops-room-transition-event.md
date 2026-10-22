---
status: open
verified: no
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
