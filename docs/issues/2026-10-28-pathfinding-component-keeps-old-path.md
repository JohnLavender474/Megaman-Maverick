---
status: closed
verified: yes
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
`./run-mega-man-maverick-debug.sh --debugPathfinding` (after that flag exists) shows the path lines.

## Notes

Found by the read-only research for the pathfinding debug shapes (2026-10-28). A fix needs an engine test: a component
with a path, a destroy, a new spawn, and no consumer call with the old path.

## Short contract

Branch: `fix/small-bugs-3`, off `master`, in `.worktrees/fix/small-bugs-3`. The branch fixes 3 small issues together.
This file holds the short contract for all of them:

- `2026-10-28-pathfinding-component-keeps-old-path.md` (this file)
- `2026-11-02-locks-reset-skips-change-hook.md`
- `2026-10-22-literal-whitespace-regex.md`

Intended changes:

- `PathfindingComponent.reset()` sets `currentPath` to `null` and resets `intervalTimer`, so a new spawn starts as a
  first spawn does.
- `AsyncPathfindingSystem` drops the pending future of an entity that leaves the system (`onEntityLeft`). Without
  this, a search that ends after the destroy writes a path into the reset component, and the next spawn consumes it.
- `LocksComponent.reset()` calls `onChange(group, false)` for each group that was locked, as `clear` does.
- All 9 `replace("\\s+", "")` calls in `core` use `Regex("\\s+")`: `GameState.fromString`, `LightSourceUtils`,
  `ElecDevil`, `DesertMan`, `BulbBlaster`, `Block` (body labels, fixture labels, block filters) and `Laser`. The
  issue named only `GameState`; the other 8 are the same bug.

Not changed, with the caller check on `master` `a0cf393e7`:

- Gameplay, for the regex: no TMX file has whitespace in a `block_filters`, `body_labels`, `fixture_labels`, `keys`,
  `light_source_keys` or `light_keys` value, and `GameState.toString` writes no whitespace. With whitespace, the
  labels and the keys threw before the fix (`valueOf`, `toInt`), and a filter tag did not match.
- Gameplay, for the locks: the only `onChange` is `behaviorsLockHook` on `MegaPlayer`. At a destroy with a
  `BEHAVIORS` lock held, the player's behaviors are allowed again at once, not at the unlock in
  the respawn steps (`RespawnSteps.kt`, `SlotRespawn.kt`; `PlayerSpawnEventHandler` when this was written). They
  lock a player that is unspawned or already spawned, and no destroy comes between the lock and the unlock, so a
  respawn sees no change.
- Gameplay, for the pathfinding: a respawned `Bat`, `SubmarineJoe`, `DeathBat` or `FloatingCan` no longer follows its
  old path in its first frames, and gets its first path after the interval, as on a first spawn.
- `SimplePathfindingSystem`, the pathfinders, the other components and the lock API.

Checks:

- New unit tests fail before the fix and pass after it: a `PathfindingComponent` with a path, then `reset()`, gives
  no consumer call; an async search that ends after the entity left the system writes no path; `LocksComponent.reset()`
  calls the hook with `false` for each locked group and not for the free groups; `GameState.fromString` with
  whitespace in the string reads the same state as without it.
- `./gradlew engine:test core:test core:smokeTest lwjgl3:jar` passes.
- No play test: no gameplay change in live data. The respawn change for the 4 pathfinding enemies is the issue's
  intended fix and is not visible in normal play.

## Resolution

Fixed in `fix/small-bugs-3`, squash commit `c52ce18f6` (2026-11-16). `PathfindingComponent.reset()` sets
`currentPath` to `null` and resets `intervalTimer`, and `AsyncPathfindingSystem.onEntityLeft` cancels and drops the
pending search. `PathfindingRespawnTest` checks that a respawn consumes no old path, waits a full search interval, and
gets no path from a search that ended after the destroy. The 2 other issues of the branch are closed in the same
commit.
