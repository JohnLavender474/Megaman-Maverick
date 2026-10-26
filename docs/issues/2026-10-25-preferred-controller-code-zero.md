---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor 031 bindings probe (2026-10-25)
related-items: [031, 034]
---

# A gamepad model with no saved bindings gets code `0` on all 8 buttons after "LOAD SAVED SETTINGS"

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/controllers/ControllerUtilsExtensions.kt:72-76`
  (`getPreferredControllerCode`)
- `core/src/main/kotlin/com/megaman/maverick/game/controllers/MegaControllerPoller.kt:30-31`
- `core/src/test/kotlin/com/megaman/maverick/game/controllers/MegaControllerPollerTest.kt:110`

(`master` `88177c11b`)

## What happens

`getPreferredControllerCode()` returns `controllerPreferences.getInteger(button.name)`. LibGDX
`Preferences.getInteger(key)` returns `0` when the key is missing, so the function never returns `null`. The
fallback in `MegaControllerPoller.run()` (`if (code == null) code = controller.mapping.getMapping(key)`) never runs.

After the player selects "LOAD SAVED SETTINGS" on the gamepad screen (`loadPrefsOnConnect = true`), a gamepad model
with no saved prefs file gets code `0` for all 8 buttons when it connects. One face button then presses every game
button. This can happen when the player connects a different gamepad model later in the same session.

The test mocks the function to return `null`, so it does not find the bug.

## What should happen

A missing key gives `null`, so the default mapping is used: check `controllerPreferences.contains(button.name)`
first.

## How to reproduce

Read the code. In the game: on the gamepad settings screen, select "LOAD SAVED SETTINGS", then connect a gamepad
model that has never been rebound. Every button acts as every game button.
