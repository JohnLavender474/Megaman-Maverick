---
status: closed
verified: yes
area: engine
gameplay-change: no
found-by: mega-refactor review (2026-10-22)
related-items: [032]
---

# `Float.round` throws in comma-decimal locales

## Where

`FloatExtensions.kt:5`

## What happens

`"%.${decimals}f".format(this).toFloat()` gives `1,5` in some locales, and `toFloat()` throws `NumberFormatException`.

## What should happen

Round with math, not with string formatting, or delete it.

## How to reproduce

No callers today.

- Fix: `fix/engine-small-bugs`. The short contract is in `2026-10-22-insertion-order-priority-queue-size.md`.

## Resolution

Fixed in `fix/engine-small-bugs`, squash commit `4522cf9db` (2026-11-14). `round` uses `BigDecimal(toDouble()).setScale(decimals, RoundingMode.HALF_UP)`. It is kept, not deleted, because `PendulumTest` and `AnimationTest` call it. `FloatExtensionsTest` checks the rounding and a run in `Locale.GERMANY`.
