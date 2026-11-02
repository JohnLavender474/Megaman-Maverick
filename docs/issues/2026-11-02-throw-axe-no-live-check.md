---
status: open
verified: no
area: core
gameplay-change: yes
found-by: 047 pooling research (2026-11-02)
related-items: [047]
---

# `throwAxe` runs after a `0.1` s delay with no live check

## Where

`core/src/main/kotlin/com/megaman/maverick/game/entities/megaman/handlers/MegamanWeaponsHandler.kt:660` (the delay) and `:1006` (`throwAxe`).

## What happens

`AXE_SWINGER` queues `throwAxe()` through `MegaUtilMethods.delayRun` for `0.1` s. Nothing cancels this work when the player dies, and `throwAxe` does not test that the player is live. So a player who dies in that window can still throw an axe. Found by reading the code. Not run.

## What should happen

A dead player spawns no shot.

## How to reproduce

Use `AXE_SWINGER`, and take a lethal hit within `0.1` s of the throw.

## Notes

`047c` owns this.
