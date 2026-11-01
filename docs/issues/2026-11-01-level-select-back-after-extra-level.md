---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor-031f-menu-input contract (2026-11-01)
related-items: [031, 007]
---

# `Esc` on the level select screen after an extra level opens the extras rows

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/menus/level/LevelSelectScreenV2.kt`: `onBack`, with a `TODO` that names this file
- `SAVE_GAME_SCREEN`: `show()` and the `MAIN MENU` row read `isInExtraLevel`
- `loadSavedState()`: does not clear `currentLevel`

Found by reading. The `031f` contract accepted this case, in its question 4. Not run in the game.

## What happens

`currentLevel` stays on the extra level after the player leaves it, and `loadSavedState()` does not clear it. So this route is possible: an extra level, `MAIN MENU`, `LOAD SAVE FILE`, level select, `Esc`.

The save screen then has the extras rows. `CONTINUE` restarts the extra level. `MAIN MENU` calls `resetPlayerProgress()`, which removes the loaded progress from memory. The save file does not change.

## What should happen

After a save load, the save screen shows the normal rows.

## How to reproduce

Do the route above, then look at the rows of the save screen. Check M8 of the `031f` contract shows this state.

## Notes

The fix that the contract named is to clear `currentLevel` in `loadSavedState()`. `LevelPauseScreen` and `SaveGameScreen` read that value, so a fix must check them.
