---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-031f-menu-input scan (2026-11-01)
related-items: [031, 034]
---

# A rebind of `START` or `A` with the key held past `0.25` s confirms the row again

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/menus/ControllerSettingsScreen.kt`: the capture of a key and the delay after it

Candidate 4 of the item `031` `NOTES.md`. Found by reading. Not run in the game.

## What happens

The player rebinds `START` or `A` and holds the new key past the `0.25` s delay after the capture. The menus confirm on a release of `START` or `A`, so the release of that key confirms the current row again, and the capture starts again.

## What should happen

The key press that ends a capture does not start a new action.

## How to reproduce

Not run. Open the keyboard settings screen, select the `START` row, press a new key, and hold it for more than `0.25` s. Let go, and look at the row.

## Notes

`031f` did not change this path.
