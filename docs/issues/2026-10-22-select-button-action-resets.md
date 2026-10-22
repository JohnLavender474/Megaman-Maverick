---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [034]
---

# `selectButtonAction` resets at every boot although it is saved

## Where

`MegamanMaverickGame.kt:263`

## What happens

The value is saved in prefs, but boot sets it to `NEXT_WEAPON`.

## What should happen

Boot applies the saved value.

## How to reproduce

Change the select button action, restart the game.
