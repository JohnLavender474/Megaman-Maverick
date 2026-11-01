# Scope: mega-refactor-051e-camera-rotation

- Status: approved
- Approved: 2026-11-01

## Goal

Remove `RotatableCamera` and `GameCameraState`. The game camera and the background cameras become plain
`OrthographicCamera`s. Rotation becomes an optional part, so a camera with no rotation is a valid case. The game plays
the same.

## Item

From the `051` `ITEM.md`, "Decisions".

2026-10-30 (owner): `051e` removes `RotatableCamera`. The game camera and the background cameras become plain
`OrthographicCamera`s, and the rotation behavior stays the same. Design:

- A `CameraRotation` part holds `direction`, `rotating` and `angle`, and never touches the camera.
- The controller writes the orientation after the parts update.
- A thin `CameraView` holds the plain camera, implements `ICameraView`, and reads the direction from the rotation part
  (`UP` if there is none).
- A background with `rotatable = false` omits the part.
- Keep: the direction changes at the start of a turn, not at its end.
- `up` can differ from today by a small float amount, so the equivalence check needs a tolerance.
- Rejected: delta writes with a cleanup hook, absolute writes by the part with a cleanup hook, and a turn applied only
  at render (the frustum test and the map renderer read the camera).

2026-11-01 (owner):

- `051e` goes before `046`.
- The design must not assume that every camera can rotate. `ICameraView` gets `rotating`, and a camera with no part
  gives `false` and `UP`. This replaces the typed lookup for `rotating`.
- `ICameraPart` and `CameraController` lose their type parameter. A cast to `OrthographicCamera` is acceptable if a part
  ever needs it.

2026-11-01 (accepted with the write-up): the view sizes go on `CameraView`; `CameraRotation` is not `Resettable`; a
rotatable background owns a `CameraRotation`, with no controller; `CameraRoomTransition` keeps the raw viewport sizes;
`utils/CameraRotator.kt` is deleted; the merge target is `mega-refactor-051-master`.

## Current state

Line numbers are for commit `3c4fb0391`.

### `RotatableCamera` (`screens/levels/camera/RotatableCamera.kt`, 154 lines)

It holds 4 things: the turn (`update`, `startRotation`, `immediateRotation`, `:55-80`, `:106-130`), the orientation
write (`rotate`, `:87-90`), the view sizes (`:30-40`, `:132-133`) and `ICameraView` (`:92-104`).

Details to keep:

- `direction` changes at the start of a turn (`:120`).
- `startRotation` does not clear `accumulator`, so a new turn during a turn starts part of the way along its time
  (`:118`, `:135-139`).
- The start view size of a new turn during a turn is the current blended size (`:115-116`).
- The angle is cumulative and can leave the range 0 to 360 (`:137`, `:146-151`).

### Who uses it

- `RotatableCamera` is named in 12 main files and 7 test files.
- 7 `core` parts bind to it. `CameraRoomBounds`, `CameraRoomScan` and `CameraEdgeBlocks` read no rotation member.
  `CameraLimitStage` (6 reads), `CameraRoomClamp` (2) and `CameraScroll` (2) read the view sizes. `CameraRotationStep`
  ticks the turn.
- The turn starts in `MegaLevelScreen.kt:849`, in the `START_GAME_CAM_ROTATION` event. Each background also starts one
  (`:850-855`).
- `MegamanMaverickGame.kt:192-196` holds the end callback: it clears `rotating`, stops the smooth follow, and submits
  `END_GAME_CAM_ROTATION`.
- `gameCamera.immediateRotation(UP)` has one caller, `MegaLevelScreen.kt:1077`, in `reset()`. Backgrounds snap at `:534`,
  in `PLAYER_SPAWN_INIT`.

### `rotating`

- `game.camera.rotating` and `services.camera.rotating` are on 43 lines in 36 files: 3 writes
  (`MegamanMaverickGame.kt:193`, `MegaLevelScreen.kt:372`, `:856`) and 40 reads.
- The hot read is `IBody.defaultDoUpdate()` (`world/body/IBodyExtensions.kt:21`), for each non-player body in each frame.
- The flag and `rotFinished` can differ in 2 cases only, and neither can be seen in play:
  - `:1077` sets `rotFinished` and does not clear `rotating`. `rotating` stays `true` until the next `start()` (`:372`).
    No level runs in that time.
  - `:372` clears `rotating` with `rotFinished` as it is. `startLevel` calls `start()` and then
    `setCurrentScreen`, which calls `reset()` on a current level screen, so `:1077` follows in the same call.
    When the level screen is not current, `reset()` ran when the level was left.
- So `rotating` can be a value of the turn, and the write at `:372` can go. The `051` contract note on the value at level
  start is answered by this. The contract still checks it.

### Backgrounds (`drawables/backgrounds/Background.kt`)

- Each background has its own `RotatableCamera` (`:70`) and ticks it in `update` (`:125-129`), before the controller
  (`MegaLevelScreen.kt:920-921`). `AnimatedBackground` and `ScrollingBackground` call `super.update`.
- `rotatable` is set only by the constructor. No TMX file sets it to `false`, but the Tiled property stays.
- `startRotation` and `immediateRotation` return a `Boolean` that no caller reads.

### Orientation reads

- Nothing reads `camera.up` in `core` or `engine`. The matrices and the frustum change only at `Camera.update()`, in
  `viewport.apply()`. So the place of the write inside `CameraController.update` cannot be seen by a part.
- `MegaLevelScreen.kt:885` and `:1036` call `getRotatedBounds()` on the concrete camera. They must read the view.

### Respawn

`Megaman.onSpawn` calls `services.gravity.set(...)` (`Megaman.kt:355`). `body.direction` keeps its old value through a
destroy, so the setter sees a change. If `gameCamera.direction` differs (`:168`), the setter starts a turn, locks
movement and turns gravity off until `END_GAME_CAM_ROTATION`. The rotation must not reset with `cameraController.reset()`
(`MegaLevelScreen.kt:535`).

### Dead code

`utils/CameraRotator.kt` has no callers.

## In scope

Engine, new in `engine/.../camera/`:

| New API | First caller |
|---|---|
| `CameraRotation`: an `ICameraPart`, with `direction`, `rotating`, `angle`, `sideways`, `start`, `snap`, `step` and an end callback | `MegamanMaverickGame`, `MegaLevelScreen`, `Background` |
| `CameraView`: implements `ICameraView`, with `viewWidth`, `viewHeight`, `blendedViewWidth`, `blendedViewHeight`; takes a nullable `CameraRotation` | `MegaServices.gameCamera`, `CameraLimitStage`, `CameraRoomClamp`, `CameraScroll` |
| `CameraOrientation`: the shared write of an angle to one camera | `CameraController`, `Background` |
| `ICameraView.rotating` | the 40 reads |
| `CameraController.applyOrientation()` | `MegaLevelScreen.reset()`, after the snap |

- `CameraRotation.sideways` is 0 for `UP` and `DOWN`, 1 for `LEFT` and `RIGHT`, and moves between them in a turn. It lets
  the part hold the blend with no viewport size, and `CameraView` gives the size. With no part, the sizes are the
  viewport sizes.
- `CameraOrientation` writes an absolute angle (`up.set(0, 1, 0).rotate(direction, angle)`), and only when the angle
  changed. So a camera that does not turn costs no vector math in a frame.
- `CameraController` writes after the parts update, if it has a `CameraRotation`. It finds the part once, in `addEffect`.
- `ICameraPart` and `CameraController` lose their type parameter: 20 and 6 lines.

Core:

- 7 parts: signature changes. `CameraLimitStage`, `CameraRoomClamp` and `CameraScroll` take a `CameraView`.
  `CameraRotationStep` goes.
- `MegamanMaverickGame`: `gameCamera` is an `OrthographicCamera`. New `cameraRotation` (holds the end callback) and
  `cameraView`. `camera` goes.
- `MegaServices`: `camera` goes. `gameCamera` is the `cameraView`, typed `ICameraView`. `TestServices` follows.
- `MegaLevelScreen`: the rotation calls, `:885`, `:1036`, and the removal of the write at `:372`.
- `Background`: a plain camera, and a nullable `CameraRotation` set from `rotatable`. Its 2 rotation functions return
  `Unit`.
- The 40 reads of `rotating`: a mechanical change to `services.gameCamera.rotating` (and `game.cameraView.rotating` in
  the screen).
- Delete `RotatableCamera.kt`, `CameraRotationStep.kt`, `GameCameraState.kt`, `utils/CameraRotator.kt`.

Tests:

- `RotatableCameraTest` becomes `CameraRotationTest` and `CameraViewTest` in `engine`, with a case for a camera with no
  rotation part.
- `GameCameraStateTest` is deleted. `GameCameraFieldTest` follows the new field.
- New tests: the orientation write; the controller with and without a rotation; the part does not reset with the
  controller.
- 5 test rigs change their camera setup.

At the merge, on `master`: update the root `CLAUDE.md` (the `camera`, `cameraController` and `MegaServices` text).

## Out of scope

- Who starts a turn, and the `Megaman.direction` setter: `058`.
- Background positions, drawing and `Background.update`'s debug string: `046`.
- `CameraRoomTransition`'s raw viewport sizes: the issue `2026-11-01-camera-room-transition-raw-viewport-size.md`.
- The `MOVEMENT` lock when a level ends during a turn: no `END_GAME_CAM_ROTATION` fires, and `Megaman.onSpawn` clears the
  lock at the next spawn (`Megaman.kt:363`). It is the current behavior.
- The typed part lookup for `startSmoothFollow()` and `stopSmoothFollow()`.

## Affected areas and risks

- **Turn timing.** The tick must stay first among the effects, and the end callback must run at the same point. It runs
  before `CameraShake` and the follow stop rule, and `stopSmoothFollow()` is part of it.
- **A new turn during a turn.** `accumulator` and the start sizes must behave as now. `CameraRotationTest` keeps these
  cases.
- **The `sideways` value.** It gives the same blend as the stored start sizes, up to float rounding.
- **`up` values.** The absolute write differs from the cumulative one by a small float amount. It removes the drift of
  many turns. This is the accepted item decision.
- **Backgrounds.** They turn in step with the game camera only because they start in the same event and take the same
  delta. Each one keeps its own snap at `:534`.
- **`core` to `engine`.** `MegaServices` has 40 readers of the new property, but the change does not change behavior.
- **Warnings.** The build has a baseline of 96 unique warnings. `CameraLimitStage` and `CameraScroll` stop reading their
  `camera` parameter in some functions, so the count must stay at the baseline.

## Overlap with other branches

See `docs/NOTES.md`. `031e` and `084f` both change `MegamanMaverickGame.kt`, and they are both near the merge.

## Open questions

- None. All design questions have decisions in the `051` `ITEM.md`.
