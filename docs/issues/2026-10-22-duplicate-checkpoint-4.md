---
status: open
verified: no
area: content
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [036]
---

# The same-name "4" spawns in ReactorMan and DesertMan likely activate the far-side spawn

## Where

`ReactorMan_v3.tmx` (ids 200 and 201, `room7`), `DesertMan_16x14_v4.tmx`, `PlayerSpawnsManager.kt:45-59`, `MegaLevelScreen.kt:237`

## What happens

No live object sets `duplicate`. Both spawns pop one after the other as the camera slides in, and the last one popped wins: probably the spawn on the far side from where the player came in.

## What should happen

The checkpoint on the path the player took is active (`036`).

## How to reproduce

In ReactorMan, reach `room7` from the main path and die. Repeat from the alt path. Found by reading; needs a check in the game.
