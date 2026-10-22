---
status: open
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
