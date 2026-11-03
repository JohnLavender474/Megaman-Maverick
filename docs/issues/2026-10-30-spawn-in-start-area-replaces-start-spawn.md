---
status: open
verified: yes
area: core
gameplay-change: no
found-by: mega-refactor-051c-camera-zones (2026-10-30)
related-items: [051, 036]
---

# A player spawn in the map's bottom-left area replaces the start spawn

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt`: `start()` sets the camera position with
  `ConstFuncs.getGameCamInitPos()`, and `render` runs `playerCheckpointMan.run()` before the first camera update.
- `core/src/main/kotlin/com/megaman/maverick/game/screens/levels/spawns/PlayerCheckpointManager.kt`: `popIfInCamera()`.

## What happens

On the first frame the camera covers the bottom-left 16x14 tiles of the map. `PlayerCheckpointManager` treats any other spawn in that
area as passed and uses it in place of the start spawn. This also replaces a `--room` spawn.

## What should happen

The level starts at the start spawn, or at the `--room` spawn.

## How to reproduce

Put a spawn that is not the first one inside the bottom-left 16x14 tiles of a map, then launch with `--room` for another room. Seen
with spawn `23` of `RefactorTest051.tmx`, before that spawn moved.

## Notes

A scan of the 23 maps that `LevelDefinition` names found no map in this state after the spawn moved, so no level plays differently
today. Possible fix: do not call `playerCheckpointMan.run()` until the first camera update is done. This changes only frame 1, and it
needs a check that the camera is at the spawn after that frame.
