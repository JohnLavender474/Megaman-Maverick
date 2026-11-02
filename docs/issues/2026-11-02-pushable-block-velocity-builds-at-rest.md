---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: fix/quicksand-two-players (2026-11-02)
related-items: []
---

# The y velocity of a `PushableBlock` grows with no limit while it rests

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/blocks/PushableBlock.kt`: `preProcess` of the body sets the ground gravity and never clears `velocity.y`. `applyFrictionY` is false and `velocityClamp.y` is not set.
- `engine/src/main/kotlin/com/maverick/game/engine/world/collisions/StandardCollisionHandler.kt`: a collision moves the body out and does not change its velocity.

## What happens

While the block rests, gravity adds to `velocity.y` on every step, and nothing removes it. A test at `MEDIUM` measured -1.7 tiles per second after 1 s of rest, -6.2 after 4 s and -12.2 after 8 s. So the block starts its next fall at that speed, and the first fall is faster after a long rest.

The world step moves the body by this velocity before the collision pushes it back, so the body sinks further into the floor in each step as the rest goes on. A very long rest can be enough for the block to pass through a thin floor. Calculated, not run.

The same rule applies to other dynamic bodies that do not clear their velocity at rest. This issue covers only `PushableBlock`.

## What should happen

The speed of the first fall does not depend on how long the block rested.

## How to reproduce

Leave a `PushableBlock` on a ledge for about 10 s, then push it off, and compare the start of the fall with a block that rested for about 5 s.

## Notes

Do not clear `velocity.y` without a check on a sinking sand floor: the block follows the floor down with that velocity and with the stick translate in `FeetBlockRule`.
