# 130: `LevelCamera` and `SharedView`: the camera stack out of `MegaLevelScreen`

- Status: planned
- Type: refactor
- Objectives: `O1`, `O2`
- Depends on: `047` (after `047h` merges)
- Related: `051`, `094`, `013`, `010`, `046`, `058`
- Branches: none yet

## Goal

`MegaLevelScreen.kt` (1027 lines) holds about 165 lines of camera code: the build of the camera parts in their fixed
order, the 2-player parts with their on/off conditions, the camera steps of a level start, a level build, the resets,
the update and the debug draw. Move them into one class that owns the camera stack, so the screen gets lean and the
order stays in one place (`O1-R3`). The 2-player parts go into an add-on that exists only with 2 players, so later
co-op phases change one file, and split screen (`094`) can build one camera per view.

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. Owner's choice: option C of the review (`NOTES.md`).

- `LevelCamera` (`screens/levels/camera/LevelCamera.kt`): builds the 1-player stack and adds it to
  `game.cameraController` in the fixed order. Methods for the 3 level start points (before `super.start`, in
  `buildLevel`, after it), the 3 reset paths (level exit, respawn, `init`), the update and the debug draw. The
  conditions (slot 0 in play and no transition; an upright view; the hold) are written once.
- `SharedView` (`screens/levels/camera/SharedView.kt`): `PlayersFocus`, `CameraViewEdges`, `CameraRoomExits`, the gate
  rectangles and the warp guides. `LevelCamera` adds its effects after `CameraEdgeBlocks`, at the same point as
  today. One nullable `shared` in place of 3 nullable fields and a `!!`.
- The 3 camera branches of `onEvent` (`NEXT_ROOM_REQ`, `SHAKE_CAM`, `START_GAME_CAM_ROTATION`) stay in the screen as
  one-line calls. No new listener.
- Stays in the screen: `LevelRoomHandler` (it reads 2 properties of `LevelCamera`), the background delta (`046`), the
  viewport rounding, `uiCamera`, `spawnCameraBounds`, the background turn of the rotation event.
- Removes the dead field `camerasSetToGameCamera`. Keeps all other behavior, also the shake that does not stop at
  level exit (`docs/issues/2026-11-06-camera-shake-survives-level-exit.md`).
- A package README `screens/levels/camera/README.md` (the order lives in `LevelCamera`; it does not list the parts).
- Checks: the camera tests as they are (the test rigs keep building single parts); one new test that `getPart` finds
  the 2-player parts with 2 players and none with 1; a play test of 1 player (a scroll stage, a turned room, a respawn
  during a shake) and the `coop_*` route with 2 players.

- Changed by `134a` (merged 2026-11-09): `CameraRoomBounds`, `CameraLimitStage`, `CameraDrawOffset` and the
  `CameraSubject` mechanism are now in `engine` (`engine/.../camera/`). `LevelCamera` takes over their build, and the
  build of `PlayersCameraSubject`, from `MegaLevelScreen.init`.

## Owner's input

2026-11-06, after the review: "The new view/camera logic is piled into `MegaLevelScreen.kt`, and it's starting to get
very very messy and difficult to keep track of." Chose option C, a new item after `047h` and before `047` phase 3,
and: remove the dead field, file the shake as an issue.

## Open questions

- What `LevelCamera` takes in. Recommendation: `MegaServices`, `CameraView`, `CameraRotation` and 2 lambdas (`lead`,
  `worldOn`), not the game, so it can be built with `testServices()`.

## Sources

- `NOTES.md` in this folder: the review of 2026-11-06 (inventory, options A to D).
