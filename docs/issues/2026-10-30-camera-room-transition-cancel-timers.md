---
status: open
verified: no
area: core
gameplay-change: unknown
found-by: mega-refactor-051c-camera-zones (2026-10-30)
related-items: [051]
---

# `CameraRoomTransition.cancel()` does not reset its timers

## Where

- `CameraRoomTransition.cancel()` in `engine/src/main/kotlin/com/maverick/game/engine/camera/`

## What happens

`cancel()` sets `state = null` but does not reset `delayTimer` or `transTimer`. A reset during a pan can leave the timers part
way through, which can shorten the next pan.

## What should happen

Each pan has the full delay and the full move, also after a cancelled pan.

## How to reproduce

Read, not run. Reset the level during a room transition, then start a new transition.
