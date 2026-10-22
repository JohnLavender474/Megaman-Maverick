# 045: Clear ownership rules and debug checks for `GameObjectPools`

- Status: planned
- Type: refactor
- Depends on: none
- Related: `009`, `010`, `019`, `022`
- Branches: none yet

## Goal

Make pooled scratch objects safe without `.free()` calls scattered everywhere, and do not depend on the 3-frame reclaim being long enough.

## Proposed scope

From the review. Not approved. The branch `SCOPE.md` decides the final scope.

- Write the contract: scratch objects live until a defined reclaim point and are never stored. Spawn props never pass ownership.
- Owned value fields and owned reusable lists for entities. Remove the `onSpawn` fetch / `onDestroy` free pairs.
- An inline `borrow {}` for local scratch use.
- Debug checks: poison on reclaim (NaN), track the generation, assert on `free()` of a reclaimable object.
- Make hidden pooled default parameters explicit (for example `Camera.toGameRectangle()`).

## Owner's input

Answer to review item 44 (2026-10-22, verbatim):

> yes, I made the assumption that 3 frames is safe, but that might not always be a guarantee. this might be a case where freeing of objects should be done manually to ensure safety, but then we should think about how to consolidate this as much as possible. for instance, an entity being passed a possible rectangle in its spawn props shouldn't have to worry about freeing that rect, otherwise we'll have a gazillion `.free()` calls scattered everywhere

## Open questions

- Is a debug-only NaN poison acceptable?
- Must the arena reclaim on ticks or on frames once `010` lands?

## Sources

- `NOTES.md` in this folder.
