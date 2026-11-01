---
status: closed
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

## Resolution

Fixed by `mega-refactor-031e-bindings`, squash commit `b22b232c9`. The saved action applies at boot, from the slot file. The owner checked it in the `031e` play test (`B4`).
