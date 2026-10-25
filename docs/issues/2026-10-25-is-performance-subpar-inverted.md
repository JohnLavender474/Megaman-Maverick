---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor 010 trade-offs scan (2026-10-25)
related-items: [010]
---

# `isPerformanceSubpar()` is true for the high tiers and has no caller

## Where

`core/src/main/kotlin/com/megaman/maverick/game/MegamanMaverickGame.kt:1073` (`master` `9cd13b5a5`)

## What happens

```kotlin
fun isPerformanceSubpar() = getPerformance().ordinal > Performance.LOW.ordinal
```

`Performance` is declared in the order `VERY_LOW`, `LOW`, `MEDIUM`, `HIGH` (`:149-154`). So the function returns
`true` for `MEDIUM` and `HIGH`, and `false` for `VERY_LOW` and `LOW`. The name says the opposite.

No code calls it, so it changes nothing today.

## What should happen

Either remove it, because it has no caller, or invert the comparison
(`getPerformance().ordinal <= Performance.LOW.ordinal`) when a caller needs it.

## How to reproduce

Read the code. `grep -rn isPerformanceSubpar` finds only the declaration.
