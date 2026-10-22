---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [007]
---

# The save keeps which health tanks the player has, not how full they are

## Where

`GameState.kt:273`, `:85`

## What happens

Load puts the tanks back empty (`putHealthTank` default `MIN_HEALTH`).

## What should happen

Unknown: maybe on purpose. Decide in `007`.

## How to reproduce

Fill a health tank, save, load.
