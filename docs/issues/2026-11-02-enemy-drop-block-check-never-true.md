---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: fix/quicksand-two-players (2026-11-02)
related-items: []
---

# The `BODY_TOUCHING_BLOCK` check on an enemy's item drop is never true

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractEnemy.kt`: the `runnablesOnDestroy` entry `ConstKeys.ITEMS` reads `BodySense.BODY_TOUCHING_BLOCK`.
- `core/src/main/kotlin/com/megaman/maverick/game/world/body/BodyComponentCreator.kt:47`: `body.onReset` calls `resetBodySenses()`.

## What happens

On destroy, the engine calls `reset()` on every component before `onDestroy()`, so all body senses are false when the runnable reads them. So the check never stops a drop, and an enemy that dies against a block can drop an item inside the block. The collision handler can then move the item far from that position.

Found by reading the code. Not run. The owner saw this move in play for a `Met` that died in sand. `fix/quicksand-two-players` stops drops from sand, and it reads the sense when the destroy is queued.

## What should happen

An enemy that touches a block when it dies drops no item, as the check intends. Read the sense when the destroy is queued, in the same way as the sand sense.

## How to reproduce

Kill an enemy that is pressed against a wall until it drops an item. The drop is random, so try several times. Then look for an item that moves through the block.
