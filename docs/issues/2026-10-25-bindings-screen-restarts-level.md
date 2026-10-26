---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor 031 bindings probe (2026-10-25)
related-items: [031]
---

# Leaving the keyboard or gamepad settings screen during a level restarts the level

## Where

`core/src/main/kotlin/com/megaman/maverick/game/screens/utils/GameSettingsPanel.kt:313-325` (keyboard) and
`:350-359` (gamepad), `master` `88177c11b`

## What happens

When the settings panel is open in a level (`isInLevelScreen`), it opens the keyboard or gamepad settings screen
with a back action that calls `game.startLevel()`. So "BACK" starts the level again, and the player loses their
progress in the level. Whether the restart uses the last checkpoint is not checked.

## What should happen

The owner's decision (2026-10-25): "BACK" goes back to the pause menu in the same level state, with no restart. The
restart is there only because the better flow was not done yet.

## How to reproduce

In a level, open pause, open the settings, open the keyboard settings, select "BACK".

## Notes

The fix must keep the level state while another screen is shown, or show the bindings screen as an overlay on the
pause menu. Item `031` plans an input menu and a bindings screen that the mouse and the system keys can reach; the
fix can come with it or before it.
