---
status: open
verified: no
area: core
gameplay-change: yes
found-by: camera split review for item 130 (2026-11-06)
related-items: [130, 051]
---

# A camera shake that runs at level exit goes on in the next level

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt`: `reset()` (level exit) does not
  stop `cameraShake`. The respawn path does (`if (!cameraShake.isFinished) cameraShake.stop()`, about line 558).
- `engine/src/main/kotlin/com/maverick/game/engine/camera/CameraShake.kt`: not `Resettable`, so
  `cameraController.reset()` does not stop it. Its timers keep their state.

## What happens

`SHAKE_CAM` starts a shake with a duration. If the level ends (for example after a boss) while the shake runs, the
shake keeps its timers, and the next level's camera shakes until the rest of the duration ends.

## What should happen

A level exit stops the shake, as a respawn does.

## How to reproduce

Found by reading the code; not seen in the game yet. Make a shake run at the moment a level ends (a long `SHAKE_CAM`
duration just before `END_LEVEL`), then start the next level and watch the first frames.

## Notes

- Fix: stop the shake in `reset()`, or make `CameraShake` `Resettable`. The item `130` refactor keeps the current
  behavior on purpose (owner decision, 2026-11-06), so the fix is its own change.
