# TODO: mega-refactor-084d-damage-reaction

## Tasks

Loose tasks that are not steps in `PLAN.md`.

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- The `SPAWN` damage lock appears to have no effect: `PlayerSpawnEventHandler.kt:127` sets it, and `Megaman.onSpawn` clears
  it (`clearLocks(MegaLockGroup.DAMAGE)`) before the beam-in, so only `!inPlay` protects Mega there. The unlock at
  `PlayerSpawnEventHandler.kt:209` is then a no-op. Traced, not run. A fix can change `SmallIceCube.kt:228`.
- `Saw` type `t` looks wrong: `setToTrajectory` (`Saw.kt:205-216`) passes the `Trajectory` value to `body.setCenter`, but that
  value is a velocity, so the saw would go to a point near the map origin. Read, not run. Check first if a live map uses type `t`.
