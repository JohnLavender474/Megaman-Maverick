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

`this.originY = originY`.

## How to reproduce

Read the code. A unit test can show it: rotate a `GameLine` about a point where `originX != originY`, then compare
its world points with the expected rotation.

## Notes

The gameplay effect is not checked. Look for `GameLine` fixtures (for example `Laser`, `LaserRules`) on bodies that
can rotate, or on direct `rotate` calls with `originX != originY`. If no live case has `originX == originY` by
chance, the fix changes how those lines behave.
