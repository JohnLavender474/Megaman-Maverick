# Process log: mega-refactor-045c-spawn-allocations-rest

## Status

- State: planned
- Goal: the rest of the spawn sweep: bosses, level objects, child entities made with a constructor, `Block` extras, shared label helpers
- Done:
- Next:
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-11-17

- No branch exists yet. From the `045b` `docs/TODO.md` ("045c"): the files in
  `core/src/test/resources/spawn-allocation-baseline.txt` (38 files, 85 constructor calls in direct `onSpawn`,
  `onDestroy` and `onEvent` bodies at the `045b` merge), by group: 15 bosses, about 38 level objects that build a sprite
  grid or parse map data per spawn, 4 classes that make a child entity with a constructor (`GutsTank`, `SigmaRat`,
  `Electrocutie`, `PushableBlock`), the extra fixtures and filters of `Block`, the `Animation` of an animated `Spike`,
  the label helpers (`addBodyLabels`, `addFixtureLabels`, `Properties.getAllMatching`), the per-event code of
  `PreciousTron`, `JetpackBlaster` and `HoverGunner`, and boxed `getOrDefault` defaults. The full scan script is
  `temp/mega-refactor-045b-spawn-allocations/scan.py` (main checkout); it also follows same-file calls.
