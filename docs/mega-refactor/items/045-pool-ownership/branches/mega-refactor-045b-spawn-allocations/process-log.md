# Process log: mega-refactor-045b-spawn-allocations

## Status

- State: blocked
- Goal: a pooled entity makes its objects once: no constructor call in `onSpawn`, `onDestroy` or an event handler
- Done:
- Next: start after `045a` merges (it needs `045a`'s `CODE_STYLE.md` rule and `OwnedArray`); done end to end by a subagent
- Blocked: waits for `mega-refactor-045a-pool-ownership`

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
