---
status: open
verified: yes
area: engine
gameplay-change: unknown
found-by: mega-refactor 020 scan (2026-10-25)
related-items: [020]
---

# `GameLine.rotate` sets `originY` to the `originX` value

## Where

`engine/src/main/kotlin/com/maverick/game/engine/common/shapes/GameLine.kt:185-190` (`master` `9cd13b5a5`)

## What happens

```kotlin
override fun rotate(rotation: Float, originX: Float, originY: Float) {
    setLocalPoints(localPoint1, localPoint2)
    setPosition(position)
    this.rotation = rotation
    this.originX = originX
    this.originY = originX
}
```

The last line uses `originX`. So the line rotates about the point (`originX`, `originX`), not about
(`originX`, `originY`). `calculateWorldPoints` (`:232-248`) uses both origin values.

`Fixture.getShape()` (`engine/.../world/body/Fixture.kt:51-65`) calls `rotate` with the body's direction for each
attached rotatable shape. So a `GameLine` fixture on a body whose direction is not `UP` can get the wrong world
points.

## What should happen

`rotate` turns the line about the world point (`originX`, `originY`), as `GameRectangle.rotate` and its callers
expect. `calculateWorldPoints` pivots on `position + origin`, so `rotate` must store the origin relative to
`position`: `this.originX = originX - position.x` and `this.originY = originY - position.y`. Only
`this.originY = originY` is not enough: `Fixture.getShape` calls `setCenter` first, which moves `position`, so the
pivot would still be off by `position`.

Example: local points (0, 0) and (1, 0), `position` (10, 0), so the world points are (10, 0) and (11, 0).
`rotate(90f, 10f, 0f)` must give (10, 0) and (10, 1). With only the `originY` fix it gives (20, -10) and (20, -9).

## How to reproduce

Read the code. A unit test can show it: rotate a `GameLine` about a point where `originX != originY`, then compare
its world points with the expected rotation.

## Notes

The gameplay effect is not checked. Look for `GameLine` fixtures (for example `Laser`, `LaserRules`) on bodies that
can rotate, or on direct `rotate` calls with `originX != originY`. If no live case has `originX == originY` by
chance, the fix changes how those lines behave.

- Live effect today: none. The only `GameLine` fixtures (`Laser`) set `attachedToBody = false`, and no other code
  calls `rotate` on a `GameLine` (checked on `master` `035e5dbec`).
- `GamePolygon.rotate` passes the world origin to the LibGDX `Polygon`, which also pivots on `position + origin`, so
  a polygon with a non-zero `position` turns about the wrong point too. No fixture uses a `GamePolygon` today, so it
  is not live and not in this fix.
- Fix: `fix/engine-small-bugs`. The short contract is in `2026-10-22-insertion-order-priority-queue-size.md`.
