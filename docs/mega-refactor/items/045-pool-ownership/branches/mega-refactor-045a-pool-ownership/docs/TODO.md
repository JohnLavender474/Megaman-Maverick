# TODO: mega-refactor-045a-pool-ownership

## Tasks

## Backlog candidates

- `borrow {}` (an inline scoped pool fetch, freed at the end of the block): not added, because no caller needs an
  exact lifetime (`SCOPE.md` D2). Add it with its first caller if a hot loop must keep its pool peak low.
- Per-frame `new` left out of this branch (not hot, debug-only or an output API): `ShapeUtils.intersectRectangleAndLine`
  (a new `Vector2` per hit, kept by the caller; used each frame by `Laser` and `TubeBeamerV2`, with a `GamePair` per
  hit in the caller), the `--debugShapes` suppliers of `Saw`, `SwingingAxe` and `SwingingPlatform` (a `GameLine` per
  frame), `Darkness.handleLightEvent` (a `GameCircle` per light event). Inventory:
  `temp/mega-refactor-045a-pool-ownership/scan-new.tsv` (main checkout).
- Measure the scratch fetch count per frame (for example a `--diagnostics` entry for the peak bucket size) to put a
  real number on the cost of the lifetime value (`NOTES.md`).
- `PoolOwnershipGuardTest` sees one line only: a default split over 2 lines, or a default that calls a scratch helper
  (`= body.getCenter()`), passes. A type-aware check (item `161`, detekt) could cover it.
- At the merge: add `--poolChecks` to the launcher flag list in the root `CLAUDE.md` (a top-level file; not edited on
  the branch).
- `MegaUtilMethods.delayRun` makes a `UUID` string and a `Timer` per call; not a pool type, out of scope.

## Bug candidates

- The engine `Pool` checks a double free with a LibGDX `IdentityMap`, whose cost grows far faster than linear with
  the number of free entries (100,000 keys: 3.8 s to add, 14 s to remove; `NOTES.md`). Entity pools and the pools
  inside `GameRectangle` use it. Check whether the check is still needed, or replace the map.

- `TEST_1` has map objects `SwinginAxe` and `JetpackIceBlaster` that match no class; they log an error and skip the
  spawner (found by the room scan).
- `DarknessV4` (no live level): `while (!lightSourceQueueIter.hasNext())` looks inverted, so the light data list is
  never filled; it also makes a `Vector2`, a list and a `LightSourceData` per light per frame.
