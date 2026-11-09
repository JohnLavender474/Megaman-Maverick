---
status: open
verified: yes
area: core
gameplay-change: unknown
found-by: mega-refactor-134a-draw-interpolation (2026-11-09)
related-items: [067]
---

# The camera "rounding" in `MegaLevelScreen.render` does nothing

## Where

`core/src/main/kotlin/com/megaman/maverick/game/screens/levels/MegaLevelScreen.kt`, `render`, the block with the
comment "perform this on all cameras to reduce floating point rounding errors".

## What happens

For each viewport camera, the code sets `position.x = (position.x * PPM) / PPM` (and the same for `y`). A multiply and
a divide by the same number give the same float (or a value 1 ulp away), so the block does not round anything.

## What should happen

Either remove the block, or make it round to whole world pixels (`MathUtils.round(x * PPM) / PPM.toFloat()`). A real
rounding changes the drawn camera position, so decide it with item `067` (whole-pixel rounding).

## How to reproduce

Read the code.
