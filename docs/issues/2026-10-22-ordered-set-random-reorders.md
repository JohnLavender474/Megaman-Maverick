---
status: open
verified: yes
area: engine
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [011]
---

# `OrderedSet.random()` changes the order of the set

## Where

`ObjectSetExtensions.kt:68-79`

## What happens

It removes the chosen item and adds it again, so each read changes the iteration order.

## What should happen

Read a random element without a side effect.

## How to reproduce

Read the code.

## Notes

Item `011` replaces random calls.
