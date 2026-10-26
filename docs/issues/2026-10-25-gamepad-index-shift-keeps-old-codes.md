---
status: open
verified: no
area: core
gameplay-change: yes
found-by: mega-refactor 031 scan (2026-10-25)
related-items: [031]
---

# After gamepad 0 is unplugged while another gamepad stays, the game keeps the old gamepad's codes

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/controllers/MegaControllerPoller.kt:19-46`
- `engine/src/main/kotlin/com/maverick/game/engine/controller/ControllerUtils.kt:11, 23`
- `gdx-controllers-desktop` `2.2.3`: `JamepadControllerMonitor` (disconnect calls `removeValue` on the live
  controller list)

(`master` `9a74fb08a`)

## What happens

The game reads only the gamepad at index 0 of `Controllers.getControllers()`. That list is in connect order, and
the library removes a gamepad from it when it is unplugged. So if gamepads A (index 0) and B (index 1) are connected
and A is unplugged, B becomes index 0.

`MegaControllerPoller.run()` checks only "is a gamepad connected at index 0". That stays true, so it never runs its
disconnect or connect code. B keeps A's button codes (A's saved prefs if `loadPrefsOnConnect` is on, else the
default mapping, which is the same for every gamepad).

## What should happen

Each gamepad is tracked by its controller object (or by the library's connect and disconnect events), not by its
index. A new gamepad at index 0 gets its own codes.

## How to reproduce

Found by reading the code and the library source; not seen in the game. Connect two gamepads, rebind gamepad A,
select "LOAD SAVED SETTINGS", unplug A, and press buttons on B.

## Notes

Item `031` (the device adapters branch) plans one adapter per connected gamepad, keyed by the controller object.
