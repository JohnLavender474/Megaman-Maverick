# Scope: mega-refactor-051a-camera-read-interface

- Status: approved
- Approved: 2026-10-30

## Goal

Entities read the game camera through a read-only interface in `engine`, with live values. The function
`getGameCamera()` becomes a property on the services holder and on the host. The game plays the same.

## Item

`051` (X-style camera bounds, auto-scroll and other camera behaviors). From the 051 `ITEM.md`, "Decisions":

- 2026-10-30 (owner, verbatim): "Fold the removal of `getGameCamera` into 051". This item removes the function
  `MegaServices.getGameCamera()`. Callers read the camera object directly, through a property. The host function
  `MegamanMaverickGame.getGameCamera()` is in the same scope. The camera instance does not change in a session.
- 2026-10-30 (owner): entities read the camera through a read-only interface with live values, not through a
  snapshot. The interface goes in `engine`. `RotatableCamera` implements it; the holder and host property and the
  `getGameCameraCullingLogic` parameters take the interface type.
- 2026-10-30 (owner): keep the frustum test of `overlaps`.
- 2026-10-30 (owner): the branch split, item 1: `051a-camera-read-interface` (refactor). The flags
  (`GameCameraState`) stay; `051b` removes them.

## Current state

Scan on 2026-10-30, at `1dc4d898a`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

### The camera object

- `RotatableCamera` (`C/screens/levels/camera/RotatableCamera.kt:14`) extends the libGDX `OrthographicCamera` and
  implements `Updatable` and `IDirectional` (`var direction`). It has `getRotatedBounds()` (`:71`): it takes a pooled
  `GameRectangle` from `toGameRectangle()` and rotates it by the direction.
- `MegamanMaverickGame.create()` makes the one game camera (`MMG:314`) as a local `val gameCamera` and gives it to the
  game viewport. `MMG.getGameCamera()` (`MMG:1018`) reads `viewports.get(ConstKeys.GAME).camera as RotatableCamera` on
  each call. The pixel-perfect toggle (`MMG:691-712`) makes new viewports with the same camera.
- `MegaServices` (`C/MegaServices.kt:35-38`) holds the camera as a private `RotatableCamera` and returns it from
  `getGameCamera()`. `postCreate()` passes `getGameCamera()` (`MMG:417`).
- `engine` has no camera type. It has `Camera.overlaps(bounds, out: BoundingBox)` (frustum test) and
  `Camera.toGameRectangle(out)` in `engine/.../common/extensions/CameraExtensions.kt`.
- `core` adds pooled forms: `Camera.overlaps(bounds)` (`C/utils/extensions/CameraExtensions.kt:9`) and
  `Camera.toGameRectangle(out = pooled)` (`C/utils/extensions/Extensions.kt:112`).

### Callers

`services.getGameCamera()`: 76 uses. No caller writes to the camera.

| Use | Count | Notes |
|---|---|---|
| `.getRotatedBounds()` | 37 | Plus 1 in `IBodyExtensions.kt` (`overlapsGameCam`) inside the 37. |
| `.overlaps(...)` | 9 | The `core` frustum extension. |
| `.toGameRectangle()` | 2 | `Disintegration.kt:72`, `RisingLavaRiver.kt:343`. Not rotated. |
| `.direction` | 1 | `Megaman.kt:214`: reads the direction, then submits `START_GAME_CAM_ROTATION`. |
| bare | 27 | 23 pass it to `getGameCameraCullingLogic`; 4 store it in a local (`DisappearingBlocks.kt:136` uses `toGameRectangle()`, `FlipperPlatform.kt:212` passes it to the culling function, `GameEntityUtils.kt:63,79` use `getRotatedBounds()`). |

Other uses:

- `GameEntityUtils.kt:56-60`: `getGameCameraCullingLogic(camera: RotatableCamera, ...)` and the overload that takes an
  entity.
- Host: `MMG:417` (holder build), `MMG:691` (pixel-perfect toggle), `MegaLevelScreen.kt:236`
  (`gameCamera = game.getGameCamera()`; the screen field is `RotatableCamera` because it calls `startRotation`,
  `update` and the position setters).
- `CameraManagerForRooms` (`:24`) and `Background` (`:70`, its own camera) use `RotatableCamera` directly. They are not
  entity readers.
- Tests: `TestServices.kt:38` has the parameter `gameCamera: RotatableCamera = RotatableCamera()`. No test reads the
  camera.
- 1 commented-out line: `GravityCapsule.kt:166`.

### The rectangles that callers change

`DarknessV2.kt:331-332` and `DarknessV4.kt:593-594` call `translate` and `translateSize` on the rectangle from
`getRotatedBounds()`. Today each call returns a new pooled rectangle, so this is safe. With live values (no shared
snapshot) it stays safe. The 051 split said that these 2 sites stop changing the returned rectangle; the scan shows
that this is not necessary, so this branch does not change them.

## In scope

1. A read-only camera interface in `engine`, for example `engine/.../camera/ICameraView.kt` (name: open question 1).
   Members, each with a caller now, all with `out` parameters because `engine` has no object pools (the same form as
   the engine camera extensions):
   - `val direction: Direction` (1 caller; open question 2).
   - `fun getRotatedBounds(out: GameRectangle): GameRectangle` (about 42 callers, with the culling function).
   - `fun toGameRectangle(out: GameRectangle): GameRectangle` (3 callers).
   - `fun overlaps(bounds: IRectangle, out: BoundingBox): Boolean` (9 callers). The frustum test, as now.
2. `RotatableCamera` implements the interface. Its `getRotatedBounds()` takes the `out` parameter.
3. `core` extensions on the interface give the pooled forms that the callers use now: `getRotatedBounds()`,
   `toGameRectangle()`, `overlaps(bounds)`. So the caller lines change only in the receiver.
4. `MegaServices`: `val gameCamera: ICameraView` in place of the private field and `getGameCamera()`.
5. The 76 caller lines: `services.getGameCamera()` → `services.gameCamera`. A mechanical commit, listed in
   `.git-blame-ignore-revs` (replaced by the squash SHA at merge).
6. `getGameCameraCullingLogic(camera: ICameraView, ...)` and the entity overload.
7. Host: `MMG.getGameCamera()` → `val gameCamera: RotatableCamera` (the full type: screens rotate and move the
   camera). The 3 host callers change. The local `val gameCamera` at `MMG:314` becomes the field.
8. `TestServices`: the parameter type becomes `ICameraView` (default stays `RotatableCamera()`).
9. One permanent test: the holder's `gameCamera` is the same instance as the host's `gameCamera` (as
   `GameCameraStateTest` does for the flags).
10. Docs: the root `CLAUDE.md` (the `MegaServices` text names `getGameCamera()`) and this branch folder.

## Out of scope

- `GameCameraState` and its 2 flags, the rotation and focus behaviors, the typed component lookup: `051b`.
- `CameraManagerForRooms`, `MegaLevelScreen` camera logic, `CameraShaker`, `Background` cameras: `051b` or later.
- A per-frame bounds cache: only after a `--diagnostics` measurement (051 decision).
- A rectangle test in place of the frustum `overlaps`: a behavior change (051 decision).
- `MMG.getUiCamera()`: not a game camera read; it stays.
- The 2 Darkness sites (see "Current state").
- The commented-out line in `GravityCapsule.kt`.

## Affected areas and risks

- About 63 files: 57 entity files, `GameEntityUtils`, `IBodyExtensions`, `MegaServices`, `MMG`, `MegaLevelScreen`,
  `RotatableCamera`, the new interface and extension files, `TestServices`.
- Risk: a call resolves to a different function after the type change. For example, `toGameRectangle()` on the
  interface must give the same rectangle as the `Camera` extension now (center and viewport size, not rotated), and
  `overlaps` must stay the frustum test, not `IRectangle.overlaps`. The build catches a missing member; a wrong overload
  is not a build error, so the plan checks each of the 4 members by name.
- Risk: the host field is set in `create()`, but the holder is built in `postCreate()`. The field must be set before
  `postCreate()` reads it.
- No gameplay change is planned. The values stay live and each call returns a new pooled rectangle, as now.

## Overlap with other branches

- `001b-host-contract` (blocked): its scope lists "cameras and viewports" in the host contract
  (`items/001-maverick-engine/branches/mega-refactor-001b-host-contract/docs/SCOPE.md:20`). The host property
  `gameCamera` and the engine interface are inputs for it. No file conflict now (no branch exists).
- `031d`, `031f` (planned, blocked): input only. No overlap.
- No other active worktree.

## Open questions

1. **Interface name and package.** Recommended: `ICameraView` in a new package `engine/.../camera/`. The engine names
   interfaces with `I` (`IDirectional`, `IRectangle`), and "view" says read-only. Alternative: `IGameCamera` in
   `common/interfaces/`.
2. **`direction` on the interface.** It belongs to rotation (051 decision), and only `Megaman.kt:214` reads it.
   - (a) Keep `val direction` on the interface now (recommended). `051b` moves it to the rotation behavior. A
     non-rotating camera can give `UP`.
   - (b) Leave it off. `Megaman` then needs another way to read the camera direction in this branch.
3. **Property name on the holder.** `camera` is taken by `GameCameraState` until `051b`.
   - (a) `gameCamera`, and keep it after `051b` (recommended): the same name as on the host and in `MegaLevelScreen`,
     no second rename of 76 lines, and no mix-up with the UI camera.
   - (b) `gameCamera` now, and `camera` after `051b` removes `GameCameraState`.
