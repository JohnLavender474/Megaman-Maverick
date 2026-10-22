---
status: open
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
