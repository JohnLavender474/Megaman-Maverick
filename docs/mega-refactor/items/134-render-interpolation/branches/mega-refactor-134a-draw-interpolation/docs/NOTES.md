# Notes: mega-refactor-134a-draw-interpolation

## Decisions

Owner, 2026-11-09, at the brief:

- D1: distance guard of 1 tile per step.
- D2: co-op shared view: average draw offset of the grouped players; 0 while the ease or a view-edge keeper acts.
- D3: `--interpolation` launch flag plus a live dev key.

Owner, 2026-11-09, at the play test: the default of `--interpolation` is `true` for now (in place of D5 at `134b`).
The owner saw no clear difference in `open` at 640 x 480, and wants to keep the change. The known `134b` items now
show in normal play, so each has a `TODO` at its code.

## Overlap with other branches

Read on 2026-11-07, at the branch start.

- `mega-refactor-127a-warp-spot-solids`: it changes the `MegaLevelScreen` wiring of `CameraViewEdges` and adds
  `REFACTOR_TEST_127` to `LevelDefinition` and `LevelCompletionScreensTest`. Text overlap only. Its co-op warp is a
  teleport outside a world cycle; the explicit snap for it belongs to `134b`.
- `mega-refactor-081e-x-sabre`: no scope yet; work in `entities/megamanx/`. No overlap expected.
- `compactor-level`: not a refactor branch; level content only.

Overlap update, 2026-11-09 (scope change 3): `CameraRoomBounds`, `CameraLimitStage` and `CameraDrawOffset` moved from
`core` `screens/levels/camera/` to `engine` `camera/`. A branch that adds code in that `core` package and uses these
classes with no import (for example `127a` in `CameraViewEdges` or its new `WarpSpotTest`) needs the new imports after a
merge. Item `130` (`LevelCamera`) now starts with these parts already in `engine`.

Owner, 2026-11-09, at the play test (`WILY_STAGE_2 --room room9`): the `GutsTank` fist hitches while the tank moves.
Decision: keep interpolation on by default and accept the known hitches for this merge; `134b` fixes them with a link
that gives a sprite the draw offset of another body. `TODO`s at `GutsTankFist` (attached fist) and `GutsTank` (tank
block).
