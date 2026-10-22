---
status: open
verified: question
area: core
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [030, 005]
---

# Question: explosions from Mega's own shots do not hurt `AbstractEnemy`

## Where

`IProjectileEntity.kt:74`, `AbstractEnemy.kt:254-255`

## What happens

`explodeAndDie()` makes the explosion's owner the projectile, not Mega. `AbstractEnemy.canBeDamagedBy` accepts only `owner == megaman` or no owner, so the explosion does no damage. About 19 projectiles pass the root owner instead.

## What should happen

Unknown. The owner decides (decision E, open part).

## How to reproduce

Read the code.

## Notes

The golden damage test (`005`) records today's result.
