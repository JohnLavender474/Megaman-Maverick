---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor 031 bindings probe (2026-10-25)
related-items: [031, 034]
---

# "RESET TO DEFAULTS" in the key and gamepad settings is not saved

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/menus/ControllerSettingsScreen.kt:251-258`
- `core/src/main/kotlin/com/megaman/maverick/game/controllers/ControllerUtilsExtensions.kt:57-70`

(`master` `9a74fb08a`)

## What happens

"RESET TO DEFAULTS" calls `resetSettingsToDefaults`, which changes the button codes in memory only, and sets
`game.selectButtonAction = NEXT_WEAPON` in memory only. Nothing is written to the prefs. Each rebind saves at once,
so after a reset, "LOAD SAVED SETTINGS" brings back the old codes and the old select action.

## What should happen

A reset saves the defaults, like a rebind does.

## How to reproduce

Rebind a key, select "RESET TO DEFAULTS", then select "LOAD SAVED SETTINGS": the rebound key comes back.

## Notes

Item `031` plans a reset that saves at once as part of the escape hatch for bad bindings.
