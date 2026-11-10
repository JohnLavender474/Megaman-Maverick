---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor-146a-player-marker (2026-11-10)
related-items: []
---

# `DarknessV4` turns its light queue into data with an inverted loop condition

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/special/DarknessV4.kt:572`

## What happens

`while (!lightSourceQueueIter.hasNext())` runs the body only when the iterator is empty. With entries in the queue,
the loop never runs, so no light source reaches the darkness. With an empty queue, `next()` throws.

## What should happen

`while (lightSourceQueueIter.hasNext())`: each queued light source becomes data for the darkness.

## How to reproduce

Read the code. In play: a level with `DarknessV4` (find it in the live TMX files) should show light around Megaman and
the other light sources. Not checked in play.
