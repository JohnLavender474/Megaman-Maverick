---
status: open
verified: yes
area: core
gameplay-change: no
found-by: item 014 research (2026-10-22)
related-items: [014]
---

# Contact state and hooks that nothing uses: `TOUCHING_CART`, `ConstKeys.CART`, `setHitByLaserReceiver`

## Where

- `MegaContactListener.kt:748-749` (begin `FEET`-`CART`) and `MegaContactListener.kt:1566-1567`
  (end) write the body sense `BodySense.TOUCHING_CART` and the body property `ConstKeys.CART`.
- `BodySense.kt:21`: `TOUCHING_CART`.
- `IFixtureExtensions.kt`: `setHitByLaserReceiver`. The listener calls `getHitByLaser` at
  `MegaContactListener.kt:806` and `:1248`.

## What happens

- No code reads `TOUCHING_CART` or the `ConstKeys.CART` body property.
- No entity calls `setHitByLaserReceiver`, so the two `getHitByLaser` calls never run.

## What should happen

Remove the dead writes, the sense value and the receiver, or use them. No gameplay effect.

## How to reproduce

`grep -rn "TOUCHING_CART\|ConstKeys.CART\|setHitByLaserReceiver" core/src/main/kotlin`

## Notes

Item `014` can drop these when it moves the rules, as an intended change with no gameplay effect.
