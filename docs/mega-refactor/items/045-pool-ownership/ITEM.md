# 045: Clear ownership rules and debug checks for `GameObjectPools`, and no per-frame allocation of pooled types

- Status: planned
- Type: refactor
- Objectives: `O1`, `O5`
- Depends on: none
- Related: `009`, `010`, `019`, `022`
- Branches: none yet

## Goal

Make pooled scratch objects safe without `.free()` calls scattered everywhere, and do not depend on the 3-frame reclaim being long enough. Then move the existing per-frame allocations to the pool or to owned fields, and write the rule so that new code does not add more.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Write the contract: scratch objects live until a defined reclaim point and are never stored. Spawn props never pass ownership.
- Owned value fields and owned reusable lists for entities. Remove the `onSpawn` fetch / `onDestroy` free pairs.
- An inline `borrow {}` for local scratch use.
- Debug checks: poison on reclaim (NaN), track the generation, assert on `free()` of a reclaimable object.
- Make hidden pooled default parameters explicit (for example `Camera.toGameRectangle()`).
- Sweep (added 2026-11-15): find each per-frame `new` of a pooled type (`Vector2`, `GameRectangle` and the other
  types in `GameObjectPools`) in `engine/` and `core/`, and move it to a pool scratch or an owned field, under the
  contract above. A value made once (a field, `init()`) stays. A value that goes into spawn props is not a pool
  scratch (the contract: spawn props never pass ownership). On `master` (2026-11-15), `core/src/main` has 238
  `Vector2()` calls and 301 `GameObjectPools.fetch(Vector2` calls; the scan sorts them into per-frame and one-time.
- Docs (added 2026-11-15): a rule in `docs/CODE_STYLE.md` for when to use `new`, an owned field or the pool, with
  examples; the package README of `GameObjectPools`. Consider a source-scan test like `RandomSourceGuardTest`
  (`011a`) if the rule can be found by text.

## Owner's input

Answer to review item 44 (2026-10-22, verbatim):

> yes, I made the assumption that 3 frames is safe, but that might not always be a guarantee. this might be a case where freeing of objects should be done manually to ensure safety, but then we should think about how to consolidate this as much as possible. for instance, an entity being passed a possible rectangle in its spawn props shouldn't have to worry about freeing that rect, otherwise we'll have a gazillion `.free()` calls scattered everywhere

2026-11-15, in the `011a` Diffity review (verbatim):

> I see some places where we're instantiating new `Vector2()` instances instead of using `GameObjectPools`. Should we replace those former calls with the latter?

> Does an existing item/fix already have the goal of addressing lines like this? If not, then we should create a new one that attempts to (1) fix all such existing cases and (2) update docs to make it so that new code doesn't repeat the same mistake

The owner chose to add this to `045` and not to make a new item. The `011a` sites (`SabreBlock.kt:112`,
`D1000Defeat.kt:118`) existed before `011a` and stay as they are in that branch.

## Open questions

- Is a debug-only NaN poison acceptable?
- Must the arena reclaim on ticks or on frames once `010` lands?

## Sources

- `NOTES.md` in this folder.
