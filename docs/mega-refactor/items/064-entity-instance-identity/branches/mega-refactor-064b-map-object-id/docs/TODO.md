# TODO: mega-refactor-064b-map-object-id

## Tasks

Loose tasks that are not steps in `PLAN.md`.

- Done: merge `master` (with `9d3f67fc5`) into the branch before the contract checks, and run `SpiderWebDestroyTest`
  (step 1).

## Backlog candidates

Ideas found during this branch that are out of scope. When the branch merges, make each one a
new item on `master` (a row in `ITEMS.md` and a folder in `items/`).

- `IFixture.getEntity()` and `IBody.getEntity()` cast the stored entity to `MegaGameEntity`
  (`C/world/body/IFixtureExtensions.kt:17`, `IBodyExtensions.kt:65`).

## Bug candidates

Bugs found during this branch. File each one in the bug tracker (see `CLAUDE.md`).

- `MegaGameEntity.onSpawn` (base `3f7329489`, `:53`) fetches a `RANDOM_ID_POOL` value also for a map spawn (eager
  default argument of `getOrDefault`) and never frees it. Each map spawn leaks one value. No gameplay effect seen.
  Goes away with `064b` (the pool is removed), so no separate fix is needed; recorded for the history.
- `FireFlower` with no `block` property (`SMB3_1` object `117`) gives its fireballs the ignore id `-1`, which is in the
  random id space; the fireball ignores any entity that holds `-1`. `064b` makes it match nothing (intended change in
  `SCOPE.md`, open question 6).
