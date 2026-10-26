---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor 031 scan (2026-10-25)
related-items: [031]
---

# A gamepad disconnect during a button capture leaves the capture state and the listener behind

## Where

`core/src/main/kotlin/com/megaman/maverick/game/screens/menus/ControllerSettingsScreen.kt:287-299` (capture start),
`:385-392` (`render`), `:431-437` (`reset`), `master` `9a74fb08a`

## What happens

When the player confirms a button row on the gamepad settings screen, the screen sets `selectedMegaButton` and adds
`buttonListener` to the gamepad. If the gamepad disconnects before a button is pressed, `render` plays the error
sound and calls `backAction` at once. The listener is not removed, and `selectedMegaButton` stays set. `show()` and
`reset()` do not clear them. So the next visit to the screen shows the capture hint text, and the old listener stays
on the old controller object.

## What should happen

Leaving the screen ends the capture: remove the listener and clear the capture state (in `reset()` or before
`backAction`).

## How to reproduce

Open the gamepad settings, select a button row, unplug the gamepad, plug it in again, and open the gamepad settings.
