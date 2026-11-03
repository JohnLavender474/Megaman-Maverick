---
status: closed
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

Do not clear `velocity.y` at rest. A puzzle depends on it. In `RodentMan_16x14.tmx` `room9`, the hole is exactly as wide as the block, and the player can stop pushing with a sliver of the block still on the ledge. The stored velocity makes the sink per step larger than the width of the sliver, so `StandardCollisionHandler` pushes the block sideways into the hole. `fix/quicksand-two-players` tried to clear the velocity, and the block then stayed above the hole (found in the owner's play test). `PushableBlockTest` now covers this case.

A fix must keep a sideways tolerance for that hole, at all performance tiers. The owner chose a fixed first fall (2026-11-02): `2026-11-02-pushable-block-ledge-sliver.md` adds the tolerance and then clears the velocity, and that fix closes this issue. The same dependence means that a block pushed soon after it spawns has less tolerance than a block that rested.

## Resolution

Fixed by `fix/pushable-block-ledge-sliver` (squash `6bdacead9`). `velocity.y` clears while the block rests on its feet block, and the 16 px ledge tolerance keeps the drop into a hole of the block's own width (`2026-11-02-pushable-block-ledge-sliver.md`).
