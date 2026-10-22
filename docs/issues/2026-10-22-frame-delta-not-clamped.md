---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [010]
---

# The frame delta is not clamped

## Where

`MegamanMaverickGame.kt:414`

## What happens

After a hitch (for example a window drag), every timer gets the full large delta.

## What should happen

Clamp the delta (`010`).

## How to reproduce

Drag the window during play and release.
