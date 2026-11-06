---
status: closed
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [038]
---

# The room-music block on respawn reads the room before Mega moves, and can throw

## Where

`MegaLevelScreen.kt:570-579`

## What happens

In `PLAYER_SPAWN_INIT`, the block calls `nextGameRoom()` before the queued `megaman.spawn` runs (`GameEngine.spawn` only queues). It tests the death position, then reads `currentGameRoom!!`, which can be stale or null.

## What should happen

Read the room after Mega is placed, or remove the block (the respawn routine in `038` replaces it).

## How to reproduce

Dead today: no live room has a `music` property.

## Resolution

Closed by `mega-refactor-038a-respawn-routine` (squash commit `a97e75e4d`, 2026-11-06), which removes the block.

Correction: the "How to reproduce" text above is wrong. Among the live maps, every `game_rooms` object of `SMB3_1.tmx` has a `music` property. The block played the death room's track for the frames before the next camera update; then `LevelRoomHandler.onSetToRoomNoTrans` played the respawn room's track. So the block changed nothing that a player can hear, and its removal changes nothing either. The owner's play test of an underground death in `SMB3_1` passed. Details: the "room-music block" section of the branch `docs/SCOPE.md`.
