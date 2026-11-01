---
status: open
verified: question
area: engine
gameplay-change: unknown
found-by: `051e` design discussion (2026-11-01)
related-items: [051, 058]
---

# Question: `CameraRoomTransition` uses the raw viewport size on a turned camera

## Where

- `engine/.../camera/CameraRoomTransition.kt:59-71`: `start` computes the pan target from `camera.viewportWidth` and
  `camera.viewportHeight`.
- `core/.../screens/levels/camera/RotatableCamera.kt:30-34`: `viewWidth` and `viewHeight` swap the two sizes when the
  camera direction is horizontal.

## What happens

When the camera direction is `LEFT` or `RIGHT`, the view is 14 tiles wide and 16 tiles high in world terms. The pan
target still uses half of `viewportWidth` (8 tiles) for x and half of `viewportHeight` (7 tiles) for y. So the target
of a pan can differ from the room edge by 1 tile on each axis.

This comes from reading the code. It is not checked in the game, and the parts that run after the pan were not traced.
They can correct the position.

## What should happen

Unknown. The owner decides. The options are to keep it, or to use the view size of the turned camera. `051e` keeps the
raw viewport sizes, so that its contract stays "plays the same".

## How to reproduce

Make a room transition with the camera direction `LEFT` or `RIGHT`, for example in a room with `megaman_direction`, and
look at the camera position at the end of the pan.
