---
status: open
verified: no
area: engine
gameplay-change: unknown
found-by: 120 research (2026-11-02)
related-items: [087, 120]
---

# `LocksComponent.reset()` does not call its change hook

## Where

`engine/src/main/kotlin/com/maverick/game/engine/locks/LocksComponent.kt:39`.

## What happens

`remove` and `clear` call `onChange`, but `reset()` only clears the bits. A `BEHAVIORS` lock held at destroy leaves the allowed behaviors off, and `Megaman.onSpawn` does not clear that group. Behaviors stay off until a later `unlock` brings the group to zero.

Today `PlayerSpawnEventHandler.kt:214` does that unlock. A direct `destroy()` and `spawn()` with no spawn handler, as `120` plans, does not. Found by reading the code. Not run.

## What should happen

Clearing a lock has one effect, on every path.

## How to reproduce

Hold `SpiderWeb` or another `BEHAVIORS` lock on a player, destroy the player, and spawn it with no spawn handler.

## Short contract

Fixed in branch `fix/small-bugs-3`. The short contract is in `2026-10-28-pathfinding-component-keeps-old-path.md`.
