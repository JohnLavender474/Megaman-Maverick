---
status: open
verified: yes
area: engine
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [026]
---

# `InsertionOrderPriorityQueue.size` is always 0

## Where

`engine/src/main/kotlin/com/maverick/game/engine/common/objects/InsertionOrderPriorityQueue.kt:20`

## What happens

`override val size = queue.size` reads the size once, at construction, so it is always 0.

## What should happen

`override val size get() = queue.size`.

## How to reproduce

Read the code, or add items and read `size`.

## Notes

Item `026` deletes this class. Check the callers of `size` before a fix.
