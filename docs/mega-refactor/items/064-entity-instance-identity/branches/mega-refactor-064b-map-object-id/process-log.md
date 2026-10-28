# Process log: mega-refactor-064b-map-object-id

## Status

- State: active
- Goal: the map object id is a `core` `MapObjectComponent` on map-spawned entities only; `RANDOM_ID_POOL` goes;
  runtime-key readers use entity references
- Done: planning approved; plan steps 1 to 12; play test passed; temporary suites deleted
- Next: owner merge approval
- Blocked:

## Entries

Add one dated entry for each work session. Put the newest entry at the bottom.

## 2026-10-28

- Branch start on `master` (`3f7329489`). Worktree `.worktrees/mega-refactor-064b-map-object-id`. The branch docs on
  `master` were empty files; filled from `_templates/branch/`.
- Scan (base `3f7329489`) in `docs/SCAN.md`. Method: a local rename of the field (reverted) and `core:compileKotlin`
  list every read and write: 57 in 25 files. Findings: every live registry query asks for a map id; `id > 0` exactly
  when the spawn props hold `ConstKeys.ID`; `Megaman` gets its player spawn object id; `Starkner`, `SteamRollerMan`
  and `DecoratedGravityForce` pass their id on; `RANDOM_ID_POOL` leaks one value per map spawn (eager default
  argument); `DarknessV3` is not used by any map and its event path throws; the `FireFlower` sentinel `-1` is in the
  random id space (`SMB3_1`); `BikerKibbo`, `Jetto` and `Starkner` read the id in `onDestroy`, after the component
  `reset()`.
- Read `fix/spider-web-super-on-destroy` (at `48fc05578`; merged into `master` as `9d3f67fc5` per the coordinator):
  no shared file; overlap in `docs/NOTES.md`.
- Draft `docs/SCOPE.md` with 8 open questions. Bug and backlog candidates in `docs/TODO.md`.
- Scope approved (owner, 2026-10-28: "064b scope is approved."), through the coordinator, with the answers: the
  extension `mapObjectId`; `MegaGameEntities` keeps `idToEntities` and reads the component at `add`/`remove`; delete
  `DarknessV3.kt`; own-id readers use `mapObjectId!!`; `Laser` takes the exact option (owner and reflector by
  reference, and the owner's map id in the `Int` set); the `FireFlower` `-1` change is an intended change with no code
  change; names `MapObjectComponent` (field `id`) and `mapObjectId`; real rooms. Also confirmed: `onSpawn` removes the
  whole component on a spawn with no `ConstKeys.ID`; `id` is a non-null `Int`; `reset()` keeps the id.
  `docs/SCOPE.md` updated; the `Laser` text names the one window that cannot be kept (after the owner or the reflector
  is destroyed, before the laser refreshes or dies).
- Draft `docs/CONTRACT.md`: automated checks (clean build, tests, warnings, the temporary
  `Temp064bMapIdEquivalenceTest` and `Temp064bLaserIgnoreEquivalenceTest`, permanent core tests, existing
  `StarknerAliveTest`/`SpiderWebDestroyTest`/`OnDestroySuperCallTest`, greps); manual checks in real rooms (rooms and
  player spawns read from the live TMX files): registry lookups (`DESERT_MAN` `room3`, `RODENT_MAN` `room1`,
  `TIMBER_WOMAN` `room13`, `INFERNO_MAN` `room11` before the boss), own-id keys (`MOON_MAN` `room12` and `room5`,
  `INTRO_STAGE` `room6` and `room2`, `DESERT_MAN` `room2`, `SMB3_1` `room2`), ignore lists (`INTRO_STAGE` `room3`,
  `WILY_STAGE_2` `room1`, `GLACIER_MAN` `room1` and `room6`, `INFERNO_MAN` `room1` and `room9`, `REACTOR_MAN` `room7`
  and `room10`), `Laser` (`PRECIOUS_WOMAN` `room2`, `WILY_STAGE_1` `room3`), and a `MOON_MAN` sanity pass. Intended
  changes: the `FireFlower` `-1`, the `Laser` window, log text, `DarknessV3` deleted.
- Contract approved (owner, 2026-10-28: "064b contract approved"), with the `Laser` window as an intended change.
- Draft `docs/PLAN.md` (12 steps): merge `master` and baseline (with `SpiderWebDestroyTest`/`OnDestroySuperCallTest`);
  `Laser.setReflector` as a pure move (so the temporary suite runs the real reflect code); both temporary suites proved
  on the old code; `MapObjectComponent` and the registry; readers of classes 0 to 2; `Laser` exact option; delete
  `DarknessV3`; remove the old id; permanent tests; docs; contract checks and smoke launches; delete the suites after
  the play test. `docs/CONTRACT.md`: the frozen `Laser` copy now has its name (`Temp064bFrozenLaserIgnore`), as the
  rule for temporary suites asks; no change of meaning. `RUMBLECADE_FACTORY` (coordinator note) is not in any check or
  scan list of this branch.
- Plan approved (owner, 2026-10-28: "Yes to both"), including `Laser.setReflector` (`internal`, a pure move).
- Step 1: merged `master` (`d711798da`: the `SpiderWeb` fix `9d3f67fc5`, the `RUMBLECADE_FACTORY` change, docs) with
  no conflict. Baseline, clean build, JDK 17 Corretto: build and jar pass; engine 571 tests, core 200 tests, 0
  failures; `SpiderWebDestroyTest`, `OnDestroySuperCallTest` and `StarknerAliveTest` pass; 88 unique warnings
  (scratchpad `064b/064b-warnings-baseline.txt`).
- Step 2: `Laser.setReflector(reflector, parent)` (`internal`) holds the moved reflect lines in the same order
  (`ignoring.clear()`, `addAll(parent.ignoring)`, `add(reflector.id)`, `on = true`, `owner = reflector`); `on = true`
  was between them on the base, so it moved with them. The updatable calls it at the old place. `core:test` passes.
- Step 3: temporary suites on the old code (core test sources):
  - `Temp064bFrozenIdRegistry` and `Temp064bMapIdEquivalenceTest` (200 seeds x 300 operations, 12 entities, 4 map
    ids per seed plus spawns with no id). Passes on the old registry: 166119 non-empty comparisons, 67326 with a
    shared id, 17195 probe comparisons during an update, 7127 respawns with another or no map id, 1201 resets, 7320
    spawn/destroy pairs in one frame.
  - `Temp064bFrozenLaserIgnore` and `Temp064bLaserIgnoreEquivalenceTest` (200 seeds x 200 operations, real `Laser`
    objects through the real `onSpawn` and `setReflector`). Passes on the old code: 304906 live comparisons (89148
    ignored, 49737 of them through a shared map id), 290816 window comparisons, owners 1384 map / 711 runtime,
    reflectors 2651 map / 1394 runtime.
  - Changes against the plan text: (1) only live candidates are compared: `isLaserIgnoring` runs for contact
    entities, which are live; a destroyed entity keeps a stale id that can equal a live id. (2) The window count is
    not 0 on the old code (158): the real random pool is shared with the `WhiteBurst` and `Laser` spawns, so the
    frozen ids differ from the real ids after a free. The window is counted, not asserted, as in the contract.
    (3) One game mock, engine and `MegaEntityFactory.init` for all seeds, `clearAllMocks(recordedCalls)` per seed,
    and `RADIUS 1` for the lasers: a laser builds one sprite per quarter tile, and the relaxed mocks record each call,
    which ran out of heap.
  - Mutation check: a reversed `getOfId` order and an owner that is not ignored each fail the suites (then reverted).
  - Engine 571, core 202 (200 + 2 temporary), 0 failures.
- Step 4: new `entities/state/MapObjectComponent.kt` (`var id: Int`, empty `reset()`) and `entities/state/MapObjects.kt`
  (`val IGameEntity.mapObjectId`). `MegaGameEntity.onSpawn`, right after the old `id` line: with `ConstKeys.ID` it
  gets or adds the component and sets `id`; without it, it removes the component. `MegaGameEntities.add`/`remove`
  index `idToEntities` by `mapObjectId`, only when it is not `null`. Both suites pass with the same counts (the id
  suite now checks the new index); `StarknerAliveTest` and `SpiderWebDestroyTest` pass. Core 202, 0 failures.
- Step 5: readers of classes 0 to 2 read `mapObjectId` (21 files). Class 0 (log text): `Block`, `FallingLeaves`,
  `Gate`, `GauntletMaster`, `MockingByte`, `Starkner`. Class 1: `AstroAssAssaulter`, `BikerKibbo`, `Jetto`,
  `GreenKoopa`, `RedKoopa`, `Starkner` (`ALIVE` and the 3 successors), `SteamRollerMan` (and `it.mapObjectId == id`
  in `canSpawn`), `MockingByte` (`sortedNests[0].mapObjectId!!`) use `mapObjectId!!`. Class 2: `Rain`, `Bat`,
  `BulletBill`, `FallingIcicle` (2), `FireWall`, `Fireball` (2), `SpitFireball`, `SmallIceCube`, `TubeBeamerV2` check
  "has a map id and the list holds it" (`ObjectSet.contains(null)` throws in LibGDX, so the `null` check comes first).
  The casts in `Rain`, `SmallIceCube` and `TubeBeamerV2` are gone; the `Laser` cast goes in step 6. Each changed
  `onDestroy()` keeps `super.onDestroy()` (`OnDestroySuperCallTest` passes). Core 202, 0 failures; both suites pass
  with the same counts.
- Step 6: `Laser`: new private `ignoringEntities`; at spawn it holds the owner, and `ignoring` gets the owner's map id
  when it has one; `setReflector` copies the parent's two sets and adds the reflector to `ignoringEntities`;
  `isLaserIgnoring` checks the reference set, then the map id against `ignoring`; `ignoringEntities` is cleared in
  `onDestroy` and `resetReflectingLaserIfAny`. The last cast to `MegaGameEntity` for the id is gone.
  - Finding, needs the owner's confirmation: with the owner's map id only (the literal answer to scope question 5),
    `Temp064bLaserIgnoreEquivalenceTest` fails at seed 0: a reflector with a map id that another live entity shares
    (for example a `SteamRollerMan` and its `SteamRoller`, or a `Starkner` and its successor) was ignored by its id on
    `master`, but not by the reference. So `setReflector` also adds the reflector's map id to `ignoring` (the same
    rule as for the owner). With that, the suite gives equal results in all 304906 live comparisons; window
    comparisons 290816, of which 11326 differ (all in the accepted window). This keeps the owner's intent ("behavior
    matches today in every case").
  - Core 202, 0 failures.
- Step 7: deleted `entities/special/DarknessV3.kt`; `SpecialsFactory.kt:49` loses the comment `/* DarknessV3(game) */`
  (the code of the line is the same). No `DarknessV3` left in `core/`, `engine/`, `lwjgl3/` or `assets/`. Build and
  `core:test` pass.
- Step 8: `MegaGameEntity` loses `id`, `CURRENT_NEW_ID`, `RANDOM_ID_POOL` (and the `Pool` import), the pool fetch in
  `onSpawn` and the pool free in `onDestroy`; `toString` prints `mapObjectId`. The compile proves that no reader of
  the field is left. No `RANDOM_ID_POOL`/`CURRENT_NEW_ID` in `core/`. `core:test` and `lwjgl3:jar` pass; both suites
  pass with the same counts.
- Step 9: permanent core tests `entities/state/MapObjectComponentTest` (7 cases: added with `ConstKeys.ID`, not added
  without, removed at a pooled respawn without it, new id at a respawn with another, id readable in `onDestroy`,
  `getOfId` order and removal, no index for a runtime entity) and `entities/hazards/LaserIgnoreTest` (4 cases: runtime
  owner by reference, map owner and shared map id, Tiled ignore objects, reflecting laser with map and runtime
  reflectors and the parent's ignores). Core 213 (202 + 11), 0 failures.
- Step 10: root `CLAUDE.md`: the `MegaGameEntity` line drops `id` (lookup by "map object id"); a new line says what
  the map object id is, how to read it (`mapObjectId`), that `getOfId` finds the entities of a map object, and that a
  runtime entity is remembered by reference.
- Step 11 (contract checks, clean build, JDK 17 Corretto): `master` changed after step 1 only in docs (items 068 to
  070), so no new merge. Build and jar pass; engine 571, core 213 (200 + 2 temporary + 11 permanent), 0 failures.
  Warnings: 87 unique lines; the only change against the 88 of the baseline is one line less (the unchecked cast in
  the deleted `DarknessV3.kt`); no new warning. Greps: no `RANDOM_ID_POOL`/`CURRENT_NEW_ID` in `core/` or `engine/`
  main code, no `var id` in `MegaGameEntity`, no `as MegaGameEntity).id`, no `DarknessV3` in `core/`, `engine/`,
  `lwjgl3/` or `assets/`; `contact-rule-selection.txt` unchanged. `SpiderWebDestroyTest`, `OnDestroySuperCallTest` and
  `StarknerAliveTest` pass. Review items in `docs/CONTRACT.md`: done (component written before `MegaGameEntities.add`;
  `add`/`remove` read the same component; class 2 checks test `null` first; class 1 own-id readers use `!!`; `Laser`
  copies and clears both sets; every changed `onDestroy()` keeps `super.onDestroy()`).
  Smoke launches (25 s each with the Gradle start, stopped by a timeout): `--level MOON_MAN --room room12`,
  `--level PRECIOUS_WOMAN --room room2`, `--level SMB3_1 --room room2`: the levels load, no exception from this
  branch. `PRECIOUS_WOMAN` logs the known `FirstSpawn` spawner error (`docs/issues/2026-10-22-misplaced-tmx-objects.md`).
- Correction to `docs/CONTRACT.md` (owner, 2026-10-28: "The blocks can be broken only by moon scythes."): the
  `RODENT_MAN` `room1` check now says to break the connected `BreakableBlock`s with the Moon Scythe weapon. No change of
  meaning.
- Owner decision (2026-10-28: "yes, accept the reflector map id"): the reflector's map id in `Laser` is accepted.
  Recorded in `docs/NOTES.md`; `docs/SCOPE.md` (item 6, item 11, open question 5) and `docs/CONTRACT.md` (the `Laser`
  rule and the permanent test text) now match the code. Noted: `fix/weapons-launch-option` merges into `master` first;
  the coordinator then merges `master` into this branch.
- Coordinator merged `master` into the branch (`0d30c7f75`: the `--weapons` dev flag from `fix/weapons-launch-option`,
  and docs). After it (coordinator run): clean build passes; engine 571, core 218, 0 failures; 87 unique warnings; both
  temporary suites pass.
- Owner play test (2026-10-28: "All tests pass"): all manual checks in `docs/CONTRACT.md` pass; the `RODENT_MAN`
  `room1` check ran with `--weapons MOON_SCYTHES`.
- Step 12: deleted `Temp064bMapIdEquivalenceTest`, `Temp064bFrozenIdRegistry`, `Temp064bLaserIgnoreEquivalenceTest`
  and `Temp064bFrozenLaserIgnore` in their own commit (`9d8e8ce93`). Automated checks again (clean build, no game
  launch): build and jar pass; engine 571, core 216 (218 - 2 temporary), 0 failures; warnings the same 87 lines as in
  step 11; no `Temp064b` left in `core/` or `engine/`. No other active `mega-refactor-` branch to read for overlap
  (`git worktree list` shows only this branch).
