# TODO: mega-refactor-051c-camera-zones

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `CameraRoomTransition.cancel()` sets `state = null` but does not reset `delayTimer` or `transTimer`, so a reset during
  a pan can shorten the next pan. From reading the code; not seen in play.
- A spawn inside the bottom-left 16x14 tiles of a map replaces the start spawn. `MegaLevelScreen.start()` puts the camera
  there with `ConstFuncs.getGameCamInitPos()`, and `playerSpawnsMan.run()` runs before the first camera update, so
  `PlayerSpawnsManager` treats the spawn as passed. This also replaces a `--room` spawn. Seen with spawn `23` of
  `RefactorTest051.tmx`; the map change avoids it, but the code keeps the fault. A scan of the 23 maps that
  `LevelDefinition` names found no other map with a non-first spawn in that area.
