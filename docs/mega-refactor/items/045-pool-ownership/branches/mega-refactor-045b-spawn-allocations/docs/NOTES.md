# Notes: mega-refactor-045b-spawn-allocations

## Decisions

The decisions D1 to D5 are in `SCOPE.md` ("Decisions"). They are Claude's choices, pending owner confirmation.

## Recipes used in the sweep

- A timer that a spawn made: make it in a field or `init()`, then `resetDuration(value)` in `onSpawn` (it also calls
  `reset()`, which puts the timer's runnables back in its queue). A `Timer(d).setToEnd()` becomes
  `resetDuration(d).setToEnd()`.
- A cullable that a spawn made: make it once, then `reset()` and `putCullable(...)` in `onSpawn`. A cullable that is
  not in the component at destroy is not reset by the component, so the spawn resets it.
- A motion: make it once with an owned vector, then `set(...)` and `reset()` in `onSpawn`. The `MotionDefinition`
  and its lambda are made once and put again.
- A sprite list that depends on the spawn size: grow-only; add a sprite only when the spawn needs one more, and hide
  the extra ones (as `SpikeBall`).
- A value that only goes into a child's spawn props: a pool scratch. The child copies it (`045a` rule 3).

## Overlap with other branches

Read on 2026-11-16:

- `fix/projectiles-ignore-death`: adds `explodesOnDeathFixture` to `IProjectileEntity` (3 lines, near the top of the
  interface). This branch changes the cull helpers lower in the same file. Separate hunks; a merge should be simple.
- `compactor-level`: changes `CeilingCrusher` (4 lines). This branch leaves `CeilingCrusher` out of scope (`045c`).
- `mega-refactor-161a-random-call-lint`: moves `RandomSourceGuardTest` to detekt. The new `SpawnAllocationGuardTest`
  is a text scan of the same kind; a `TODO` at it names item `161`. `161a` changes 2 lines of `MegamanMaverickGame`;
  this branch does not change that file.
- `mega-refactor-060k-protoman-frame-edits`, `fix/pause-restores-systems`: no overlap.
