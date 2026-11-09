# TODO: mega-refactor-134a-draw-interpolation

## Tasks

Loose tasks that are not steps in `PLAN.md`.

- At the merge, on `master`: add to the `130` `ITEM.md` that `134a` moved `CameraRoomBounds`, `CameraLimitStage`,
  `CameraDrawOffset` and the `CameraSubject` mechanism to `engine`, and that `LevelCamera` takes over their build and
  `PlayersCameraSubject` from `MegaLevelScreen.init`.
- At the merge, on `master`: add `--interpolation` (and the `I` key) to the launcher flags in the root `CLAUDE.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- For `134b` (not a new item): the cases seen or found so far: the `GutsTank` fist (seen by the owner) and tank
  block, the `BigAssMaverickRobot` hand and orbs, the `PRECIOUS_GUARD` gems (`PreciousGemCluster`); sprite entities with no body that place a sprite on another body; the shape renderer
  draw offset (the `WhiteArrowPool` outline); the `I` key during a pause changes the sprite offsets at once, but the
  camera offset only at resume (dev key only).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `Background.update` (`core/.../drawables/backgrounds/Background.kt:127`) builds a debug string with
  `boundingRectangle` each frame for each background, also when the tag is filtered out (`O5-R1`).
- `MegaLevelScreen.render` "reduce floating point rounding errors" (`MegaLevelScreen.kt:847-851`) multiplies and
  divides by `PPM`, so it does nothing. Either remove it or make it round (with `067`).
