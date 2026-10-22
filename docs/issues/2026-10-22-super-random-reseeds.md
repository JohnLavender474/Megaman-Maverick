---
status: open
verified: yes
area: engine
gameplay-change: unknown
found-by: mega-refactor review (2026-10-22)
related-items: [011]
---

# `superRandom()` makes a new clock-seeded `Random` on each call

## Where

`ArrayExtensions.kt:109-113`, `ObjectSetExtensions.kt:81-89`

## What happens

Two calls in the same millisecond give the same index.

## What should happen

Use one random source (`011`).

## How to reproduce

Read the code.
