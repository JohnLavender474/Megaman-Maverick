---
status: open
verified: no
area: core
gameplay-change: unknown
found-by: mega-refactor-047b-player-rules enemy review (2026-11-01)
related-items: []
---

# `DarknessV4` loops while its iterator has no next entry

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/special/DarknessV4.kt:572`

## What happens

The loop is `while (!lightSourceQueueIter.hasNext())`, and its body calls `next()`. With entries in the queue, the
body never runs, so the light source list stays empty. With no entries, the body calls `next()` on an empty iterator.

## What should happen

The loop visits each entry of `lightSourceQueue`: `while (lightSourceQueueIter.hasNext())`. First check if this code
path runs at all, and what depends on today's result.

## How to reproduce

Found by reading the code. Not run.
