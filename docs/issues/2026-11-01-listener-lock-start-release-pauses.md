---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-031f-menu-input plan (2026-11-01)
related-items: [012, 013, 031]
---

# A `START` release at the start of a lock from a listener can still pause, with a dead menu

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/LevelPauseRules.kt`: `queuePause`, with a `TODO` that names this case
- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt:753`: the boss-defeat lock, submitted from `onEvent`

Found by reading the code at `031f`. Not run in the game.

## What happens

`031f` fixed the 2 triggers of `2026-11-01-start-release-at-lock-start-pauses.md`: the pause test and the queued pause each test the lock. That covers a lock that `render` submits, because `events.run()` sets `locked` before the queued pause runs.

A lock that a listener submits starts 1 frame later, because it is submitted from inside `events.run()`. So a real `START` release at that moment passes both tests. The game pauses, and the lock then starts. The pause menu reads `RELEASED` for all later frames.

`Esc` leaves that pause, because the system frame is not locked. A pad-only player has no way out.

## What should happen

A pause does not start at the start of a lock, or the player has a way out of it on each device.

## How to reproduce

Not reproduced. It needs a `START` release in one exact frame, at a boss defeat.

## Notes

Item `012` replaces `TURN_CONTROLLER_*` and the lock, so the fix belongs there. A pad back button (item `070`) would also give a pad-only player a way out.
