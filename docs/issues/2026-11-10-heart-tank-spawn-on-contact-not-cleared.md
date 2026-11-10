---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-146a-player-marker (2026-11-10)
related-items: []
---

# A pooled `HeartTank` keeps its spawn-on-contact list from the last life

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/items/HeartTank.kt` (`onSpawn`, `spawnOnContact`)

## What happens

`onSpawn` adds each spawn-on-contact map object to `spawnOnContact`, and nothing clears the array (`onDestroy` clears
only `lightSourceKeys`). A pooled `HeartTank` that spawns again runs its contact spawns twice.

## What should happen

Each life runs only the contact spawns of its own spawn props: clear `spawnOnContact` in `onSpawn` or `onDestroy`.

## How to reproduce

Read the code. In play: a heart tank with spawn-on-contact objects that spawns a second time (for example after a
death and a respawn in its room).
