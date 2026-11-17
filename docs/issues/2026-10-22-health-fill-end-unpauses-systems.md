---
status: closed
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [012]
---

# When a health fill ends during a pause, all systems turn on while the game is still paused

## Where

`MegaLevelScreen.kt:894`, `PlayerStatsHandler.kt:317`

## What happens

`playerStatsHandler.update(delta)` runs every frame, also while paused. A pickup health fill does not lock input. When the fill queue empties during a pause, `PlayerStatsHandler.kt:317` turns all systems on. `engine.update` also runs while paused, so the simulation and the controller system run under the pause menu.

## What should happen

The fill does not change system flags while paused.

## How to reproduce

Pick up a large health item at low health and pause at once. Wait for the fill to end while paused.

## Notes

Confirmed by reading the code on `master` (2026-11-16): `PlayerStatsHandler.update` runs at `MegaLevelScreen.kt:853`,
outside the `!game.clock.paused` block, and turns all systems on at `PlayerStatsHandler.kt:321`.

The fix is in `fix/pause-restores-systems`, with the short contract in
`2026-10-22-pause-during-gate-reenables-physics.md`.

## Resolution

Fixed in `fix/pause-restores-systems`, squash commit `165ee4510` (2026-11-17). The fill still runs while paused, as
before (owner decision, 2026-11-16). While paused, its system changes go into the pause snapshot of
`LevelStateHandler`, and `resume()` applies them. See `2026-10-22-pause-during-gate-reenables-physics.md`.
