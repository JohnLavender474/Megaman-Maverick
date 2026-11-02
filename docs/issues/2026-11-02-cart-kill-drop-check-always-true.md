---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: 048 research (2026-11-02)
related-items: [048]
---

# The check that stops an item drop on a cart kill is always true

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractEnemy.kt:202`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/contracts/AbstractHealthEntity.kt:96`
- `core/src/main/kotlin/com/megaman/maverick/game/entities/special/CartV2.kt:150-157`

## What happens

`canSpawnItemOnHealthDepleted()` is `lastDamager != CartV2`. `CartV2` here is the companion object of the class, not a cart, so the comparison is always true.

Also, `CartV2` kills through `depleteHealth()`, which does not set `lastDamager`. So a fix to the comparison alone has no effect.

## What should happen

The code suggests that an enemy killed by a cart drops no item. The owner can also choose to keep the drop, and then the check goes away.

## How to reproduce

Ride a moving `CartV2` in `DESERT_MAN` `room9` into an enemy until it drops an item.

## Notes

`048` keeps this behavior unless this issue is fixed first. It moves the check with no change in meaning.
