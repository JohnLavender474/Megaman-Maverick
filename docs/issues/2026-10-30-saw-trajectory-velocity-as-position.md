---
status: open
verified: no
area: core
gameplay-change: no
found-by: mega-refactor-084d-damage-reaction test map (2026-10-30)
related-items: []
---

# `Saw` type `t` passes a velocity to `body.setCenter`

## Where

- `core/src/main/kotlin/com/megaman/maverick/game/entities/hazards/Saw.kt:205` (`setToTrajectory`)

(`master` `4b65b1fdb`)

## What happens

`setToTrajectory` makes a `MotionDefinition` with `function = { value, _ -> body.setCenter(value) }`. The value from `Trajectory` is a
velocity, so a type `t` saw would go to a point near the map origin, not along its path.

## What should happen

The saw follows its trajectory from its spawn.

## How to reproduce

Read, not run. Spawn a `Saw` with `type=t` and a `trajectory` property.

## Notes

No live map uses `Saw` type `t` (checked in every `tmxMapSource` in `LevelDefinition`), so the game does not show this today.
