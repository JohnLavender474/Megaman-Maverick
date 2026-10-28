# Notes: mega-refactor-064b-map-object-id

## Decisions

- 2026-10-27 (owner, item `ITEM.md`): `id` becomes a `core` component present only on map-spawned entities;
  `RANDOM_ID_POOL` goes; runtime-key readers use entity references; the 4 casts go through `getComponent`. Copied in
  `SCOPE.md`.

- 2026-10-28 (owner, gate 1, "064b scope is approved."): the answers to the 8 open questions, copied in `SCOPE.md`
  "Open questions". Not the recommendation for `Laser`: the exact option.

- 2026-10-28 (owner, during the play-test wait: "yes, accept the reflector map id"): `Laser.setReflector` also adds
  the reflector's map id to the `Int` set (found in step 6: without it, a reflecting laser no longer ignored entities
  that share the reflector's map id, as it does on `master`). `SCOPE.md` and `CONTRACT.md` updated to match the code.

## Overlap with other branches

- `fix/spider-web-super-on-destroy` (`.worktrees/fix-spider-web-super-on-destroy`), read on 2026-10-28 at
  `48fc05578`; merged into `master` as `9d3f67fc5` (coordinator note, 2026-10-28). Changes: `SpiderWeb.onDestroy`
  calls `super.onDestroy()`; new `OnDestroySuperCallTest` (each `onDestroy()` override in `core/src/main` calls
  `super.onDestroy()`; `MegaGameEntity.kt` is exempt) and `SpiderWebDestroyTest` (spawns a web with
  `ConstKeys.ID pairTo 201` and asserts that `MegaGameEntities.getOfId(201)` and `getOfTag` drop it at destroy).
  - No shared file with the `064b` scope (`064b` does not change `SpiderWeb.kt`).
  - `SpiderWebDestroyTest` uses `getOfId`, which `064b` keeps. The web spawns with `ConstKeys.ID`, so under `064b` it
    gets a `MapObjectComponent` and is indexed as today. The test needs no change. Check it after `master` is merged
    into this branch.
  - `OnDestroySuperCallTest`: a changed `onDestroy()` in `064b` must keep its `super.onDestroy()` call.
  - On the base, a destroyed `SpiderWeb` stayed in `idToEntities` under a leaked random id. `SpiderWeb` is code-spawned
    only (and the coordinator says nothing fetches it), so under `064b` it is not in `idToEntities` at all.
- `fix/weapons-launch-option` (a `--weapons` dev flag), noted 2026-10-28 by the coordinator: it merges into `master`
  first; the coordinator then merges `master` into this branch, so that the `RODENT_MAN` `room1` check can run with
  `--weapons MOON_SCYTHES`. No shared code file expected.
