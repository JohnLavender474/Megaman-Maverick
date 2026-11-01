---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-031f-menu-input scan (2026-11-01)
related-items: [031, 013]
---

# A press read carries a held button to the next screen

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/other/CreditsScreen.kt:83`: `shouldSkipCredits()`, the skip on press
- `core/src/main/kotlin/com/megaman/maverick/game/screens/menus/MainMenuScreen.kt:134`: `gameState.reset()` on `START NEW GAME`

Inference from reading. This needs a check in the game.

## What happens

`CreditsScreen` skips on a press. `fullEndTimer` is 1.5 s (`:50`). If `START` stays held past that, its release on `MainMenuScreen` confirms `START NEW GAME`, because the menus confirm on release. That row calls `gameState.reset()`.

## What should happen

A release that belongs to a press on an earlier screen does not confirm a row on the next screen.

## How to reproduce

Not run. Reach `CreditsScreen`, press and hold `Enter` to skip it, and keep it held for more than 1.5 s, until `MainMenuScreen` shows. Let go, and look at which row runs.

## Notes

`031f` kept the same button and the same edge at this read, so this behavior is not new. Check K4 of the `031f` contract keeps the press short for this reason.
