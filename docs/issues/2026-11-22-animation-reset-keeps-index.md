---
status: open
verified: yes
area: engine
gameplay-change: no
found-by: mega-refactor-078g-timber-walker (2026-11-22)
related-items: [078, 180]
---

# `Animation.reset()` does not set the frame index back to 0

## Where

`engine/src/main/kotlin/com/maverick/game/engine/animations/Animation.kt`, `reset()`.

## What happens

`reset()` sets only `elapsedTime` to 0. `currentIndex` keeps the index of the last play. A caller that reads the
frame after `reset()` and before the next `update()` gets the old frame. The `TimberWalker` of `078g` showed this as a
one-frame flash of a wrong frame or the wrong facing at a state change, from the second time a state was entered.
`Animator` hides the fault, because it calls `update()` after a reset. The walker now calls `setIndex(0)`.

## What should happen

`reset()` also sets `currentIndex = 0`.

## How to reproduce

Code reading; or an `AnimationTest` case: play an animation to its last frame, call `reset()`, read `getIndex()`.

## Notes

Smoothness review finding 1 of `078g`.
