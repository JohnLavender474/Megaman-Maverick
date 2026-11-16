---
status: open
verified: yes
area: engine
gameplay-change: no
found-by: `mega-refactor-045a-pool-ownership` (2026-11-16)
related-items: [045]
---

# The engine `Pool` double-free check slows down far faster than linear with many free entries

## Where

`engine/src/main/kotlin/com/maverick/game/engine/common/objects/Pool.kt`: `freeSet`, a LibGDX `IdentityMap`, checked
in `free()` and updated in `fetch()`.

## What happens

The cost of the `IdentityMap` grows far faster than linear with the number of free entries. Measured in `045a`:
100,000 keys take 3.8 s to add and 14 s to remove. The first `core:test` run of `045a` hung on it, when
`GameObjectPools` still used this `Pool`. `GameObjectPools` no longer does, but the entity pools
(`GameEntityPoolCreator`) and the pools inside `GameRectangle` still use it.

## What should happen

A double-free check with a cost that stays flat with the pool size, or no check where the owner contract makes it
unnecessary.

## How to reproduce

Read the code, and the measure in
`docs/mega-refactor/items/045-pool-ownership/branches/mega-refactor-045a-pool-ownership/docs/NOTES.md`.

## Notes

Normal play keeps entity pools small, so the effect in play is not measured.
