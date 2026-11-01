# Notes: mega-refactor-051e-camera-rotation

## Decisions

## Overlap with other branches

Scan on 2026-11-01, at `3c4fb0391`.

- `mega-refactor-084f-focus-progress` (play test passed, waits for merge approval): changes `MegamanMaverickGame.kt`
  (6 lines), `Constants.kt` and `Megaman.kt`. It does not plan a rotation change. `Megaman.kt` has reads of
  `gameCamera`, and they do not change here. `051e` changes other lines in `MegamanMaverickGame.kt`.
- `mega-refactor-031e-bindings` (at the play test): changes `MegamanMaverickGame.kt` (55 lines), `MegaLevelScreen.kt`
  (4 lines) and `Constants.kt`. No shared camera code.
- If either merges first, the parent takes in `master`, and then this branch takes in the parent.
- Both reached `master` during this branch: `084f` before step 2, and `031e` after step 7. Each came through the parent with
  no conflict, and the checks ran again after each merge.

## Root `CLAUDE.md` text for `master`

To apply when `051e` reaches `master`, in "Game object":

- Replace the `camera: GameCameraState` line with: `gameCamera`: a plain `OrthographicCamera`, made with the game.
  `cameraRotation: CameraRotation` (`engine/.../camera/`): the turn of the game camera, made with the game; it holds
  `direction`, `rotating` and `angle`, it never touches the camera, and it is not `Resettable`, so a respawn in a turned
  room keeps its smooth turn. `cameraView: CameraView`: the reads of the game camera; `viewWidth` and `viewHeight` are final
  at once, and `blendedViewWidth` and `blendedViewHeight` move over a turn. With no rotation part, a view gives
  `rotating = false`, direction `UP` and the viewport sizes.
- In the `cameraController` line, replace `CameraRotationStep` with `CameraRotation`, and add: the controller writes the
  orientation after the parts update (`applyOrientation()`).
- In the `MegaServices` line, remove `camera` from the list, and add that `gameCamera` also gives `rotating`.
