# Process log: mega-refactor-045b-spawn-allocations

## Status

- State: active
- Goal: a pooled entity makes its objects once: no constructor call in `onSpawn`, `onDestroy` or an event handler
- Done:
- Next: scan, SCOPE.md and CONTRACT.md, brief; done end to end by a subagent
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-16

- No branch exists yet. The owner, in the `045a` Diffity review (2026-11-16), on `SpikeBall.kt`
  (`getCenter(Vector2())` in `onSpawn`): "Instantiating a new `Vector2()` defeats the whole purpose of the pool which
  is to prevent the GC from having to collect new instances. [...] I propose refactoring code like in this class."
  Owner choice: `045a` fixes the `CODE_STYLE.md` rule and refactors `SpikeBall` as the model; `045b` does the sweep, by
  a subagent.
- Measured on `045a` at `071f6f1ad` (text count, so it can include a few false hits): 146 lines in 75 files construct
  an object inside an `onSpawn` body (`Vector2`, `GameRectangle`, `GameCircle`, `GameLine`, `GameSprite`, `Pendulum`,
  `MotionDefinition`, `Timer`); 84 of them are `Vector2`, `GameRectangle`, `GameCircle` or `GameLine`. Event handlers
  and `onDestroy` bodies are not counted yet. The scan step counts them.
- `045a` merged (2026-11-16, squash `aaf51b514`), so this branch can start. Added to its scope from the `045a`
  `docs/TODO.md`: the per-frame `new` that `045a` left (`ShapeUtils.intersectRectangleAndLine`, a `Vector2` per hit,
  each frame for `Laser` and `TubeBeamerV2`; the `--debugShapes` suppliers of `Saw`, `SwingingAxe` and
  `SwingingPlatform`; `Darkness.handleLightEvent`) and `MegaUtilMethods.delayRun` (a `UUID` string and a `Timer` per
  call, often from `onSpawn` and events). Inventory: `temp/mega-refactor-045a-pool-ownership/scan-new.tsv` (main
  checkout).
- Branch start on `master` (2026-11-16). Merge target: directly into `master` (as `045a`). Done end to end by a subagent.
