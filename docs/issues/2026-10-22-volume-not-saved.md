---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [034]
---

# Volume changes in game are not saved

## Where

`GameSettingsPanel.kt:177-215`; defaults in `DesktopLauncher.kt:38-39` (0.8) and `MegamanMaverickGame.kt:126-127` (0.5)

## What happens

The player can change the volume in game, but the value is lost on quit. The two defaults also differ.

## What should happen

Save the volume, and use one default.

## How to reproduce

Change the volume, restart the game.
