---
status: open
verified: yes
area: core
gameplay-change: yes
found-by: mega-refactor review (2026-10-22)
related-items: [034]
---

# Saved keyboard bindings are not applied at boot

## Where

`MegamanMaverickGame.kt:261`, `ControllerUtilsExtensions.kt:17-35`, `ControllerSettingsScreen.kt:185`, `:224`, `MegaControllerPoller.kt:30`

## What happens

Boot loads only the default keys. Saved keys load only when the player selects "load saved settings". Controller prefs load on connect only after that screen sets `loadPrefsOnConnect = true`.

## What should happen

Unknown: maybe on purpose. If not, saved bindings apply at boot.

## How to reproduce

Change a key binding, save, restart the game. The default binding is active.

## Notes

Ask the owner first. Item `034` covers settings.
