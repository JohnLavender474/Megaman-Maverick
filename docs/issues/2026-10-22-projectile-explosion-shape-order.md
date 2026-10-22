---
status: open
verified: yes
area: core
gameplay-change: no
found-by: item 014 research (2026-10-22)
related-items: [014]
---

# `hitExplosion` gets its two shapes in the reverse order

## Where

- `MegaContactListener.kt:721`: `projectile1.hitExplosion(otherFixture, otherShape, thisShape)`.
- `IProjectileEntity.kt:94`: `fun hitExplosion(explosionFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D)`.

## What happens

All other `PROJECTILE` sub-rules pass `(thisShape, otherShape)`. The `EXPLOSION` sub-rule passes the
explosion's shape as `thisShape` and the projectile's shape as `otherShape`.

Today no override reads the shapes (`IceBomb`, `FallingIcicle`, `BunbyRedRocket` and `Snowball`
only call `explodeAndDie()`), so nothing goes wrong. A future override that reads the shapes gets
them swapped.

## What should happen

Pass `(otherFixture, thisShape, otherShape)`. No gameplay effect today.

## How to reproduce

Read the lines above.
