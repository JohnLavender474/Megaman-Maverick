---
status: open
verified: no
area: engine
gameplay-change: unknown
found-by: pathfinding debug-shapes research (2026-10-28)
related-items: [016]
---

# `PathfindingComponent` keeps its old path through a destroy and a new spawn

## Where

`engine/src/main/kotlin/com/maverick/game/engine/pathfinding/PathfindingComponent.kt:17` (`internal var currentPath`),
on `master` at `f6e9feb7c`. The class has no `reset()` override, so the empty default runs. `AbstractPathfindingSystem`
(`engine/.../pathfinding/AbstractPathfindingSystem.kt:34-35`) calls the component's consumer on every frame while
`currentPath != null`.

## What happens

The engine calls `reset()` on every component when an entity is destroyed. `currentPath` is not cleared. A pooled
entity with pathfinding (`Bat`, `SubmarineJoe`, `DeathBat`, `FloatingCan`) that spawns again can consume its old path on
its first frames, until the system gives it a new one [inferred, not played].

## What should happen

A new spawn starts with no path: `PathfindingComponent.reset()` sets `currentPath = null` (and resets any other
per-spawn state of the component).

## How to reproduce

Not played yet. Kill a `Bat` that is following a path, then let the same pooled `Bat` spawn again (leave the area and
come back, or a level with many bats). Watch whether its first movement follows the old path toward the old target.
`./run-game-debug.sh --debugPathfinding` (after that flag exists) shows the path lines.

## Notes

Found by the read-only research for the pathfinding debug shapes (2026-10-28). A fix needs an engine test: a component
with a path, a destroy, a new spawn, and no consumer call with the old path.
