# Scope: mega-refactor-064b-map-object-id

- Status: approved
- Approved: 2026-10-28

## Goal

The Tiled map object id is level data in a `core` component, not a field of every entity. An entity has a
`MapObjectComponent` only when its last spawn had `ConstKeys.ID` in the spawn properties. `MegaGameEntity` loses `id`
and `RANDOM_ID_POOL`. The registry indexes by the component. Readers that use the id as a key for runtime entities use
entity references. No gameplay change, except the intended change listed below.

## Item

From `064` `ITEM.md`, "Decisions" (2026-10-27): "`064b`: `id` becomes a `core` component (`MapObjectComponent`, name to
confirm in scope) present only on entities spawned from a map. `RANDOM_ID_POOL` goes; readers that use `id` as a key
for runtime entities (`Laser` owner, `DarknessV3` light sources) change to entity references. The 4
`(x as MegaGameEntity).id` casts go through `getComponent` on `IGameEntity`. Reason: `id` is level data, not an
instance identity; it is not unique (`idToEntities` maps an id to a set; `Starkner` successors share an id) and it
changes on each spawn of a pooled entity. Not on `IGameEntity`: the engine identity is the object reference. Not a
property: untyped, string keys."

Approved at gate 1 (owner, 2026-10-28: "064b scope is approved."), with the answers in "Open questions" below.

## Current state

Full scan: `docs/SCAN.md`. Base `3f7329489`. `C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

- `MegaGameEntity.onSpawn` sets `id` from `ConstKeys.ID`, or from `RANDOM_ID_POOL` (negative). The pool value is
  fetched also for a map spawn and then leaks (eager default argument). `onDestroy` frees a negative id.
- Today `id > 0` exactly when the spawn properties hold `ConstKeys.ID` (all map spawn paths, plus the code that passes
  an id on: `Starkner` successors, `SteamRollerMan` to `SteamRoller`, `DecoratedGravityForce` children). `Megaman`
  gets the id of its player spawn object.
- 57 reads and writes of the field in 25 files (compiler list). Classes (`SCAN.md` sections 4 to 7):
  - 0, log text only: 18 reads.
  - 1, the entity at a map object or state of one map object: the registry callers (`InfernoMan`, `TorikoPlundge`,
    `MockingByte`, `BreakableBlock`), and own-id keys in `AstroAssAssaulter`, `BikerKibbo`, `Jetto`, `GreenKoopa`,
    `RedKoopa`, `Starkner`, `SteamRollerMan`, `MockingByte`. All these classes spawn only with `ConstKeys.ID` in live
    content.
  - 2, a hit entity against a Tiled id list: `Rain`, `Bat`, `BulletBill`, `FallingIcicle`, `FireWall`, `Fireball`,
    `SpitFireball`, `SmallIceCube`, `TubeBeamerV2`, part of `Laser`.
  - 3, a key for runtime entities: `Laser` owner and reflector; `DarknessV3` (not used by any map, and its event path
    throws); the `FireFlower` sentinel `-1`.
- Every live registry query asks for a map id (> 0). So an index of only the entities with a component gives the same
  results for every live query.
- `BikerKibbo`, `Jetto` and `Starkner` read their id in `onDestroy`, after the engine `reset()` of every component.

## In scope

Core:

1. New `MapObjectComponent(var id: Int) : IGameComponent` (package `C/entities/state/`, next to `PlayPhaseComponent`).
   `id` is a non-null `Int`. `reset()` keeps the id: it stays readable in `onDestroy` (after the engine `reset()`), and
   the next spawn writes it again or removes the component.
2. New extension `val IGameEntity.mapObjectId: Int?` = `getComponent(MapObjectComponent::class)?.id` (same file
   style as `PlayPhases.kt`).
3. `MegaGameEntity`:
   - loses `id`, `CURRENT_NEW_ID` and `RANDOM_ID_POOL`;
   - `onSpawn`, at the place where `id` is set today (before `MegaGameEntities.add`): if `ConstKeys.ID` is in the spawn
     properties, get or add the component and set `id`; else remove the whole component if a former spawn added it (a
     pooled object that was map-spawned before);
   - `onDestroy` loses the pool free;
   - `toString` prints `mapObjectId` (`null` for a runtime entity).
4. `MegaGameEntities`: `add`/`remove` index the entity in `idToEntities` only when it has the component, under the
   component id. `getOfId` and `existsAnyOfId` stay. The registry reads the component at `add` (in `onSpawn`) and at
   `remove` (in `onDestroy`), the same points as today; only `MegaGameEntity.onSpawn` writes it. No
   `MapObjectsSystem`.
5. Class 0, 1 and 2 readers read `mapObjectId` (no cast). The 4 casts (`Rain.kt:200`, `TubeBeamerV2.kt:208`,
   `Laser.kt:677`, `SmallIceCube.kt:195`) go. A class 1 reader of its own id uses `mapObjectId!!` (`AstroAssAssaulter`,
   `BikerKibbo`, `Jetto`, `GreenKoopa`, `RedKoopa`, `Starkner`, `SteamRollerMan`; `MockingByte` for the nest id). A
   class 2 check becomes "the hit entity has a map id and the list holds it".
6. `Laser` (the exact option):
   - `ignoring: ObjectSet<Int>` keeps the Tiled ids and, when the owner has a map id, the owner's map id (added at
     spawn, as today), and, when the reflector has a map id, the reflector's map id (added at each reflect update, as
     today; owner decision 2026-10-28, see "Open questions");
   - a new private `ignoringEntities` set holds the owner (at spawn) and the reflector (each reflect update, after the
     copy of the parent's two sets) by reference;
   - `isLaserIgnoring` checks the reference set, then the hit entity's map id against `ignoring`;
   - both sets are cleared at the points where `ignoring` is cleared today.

   While the owner and the reflector are live in the spawn that was added, the result is the same as today: the map id
   rule is the old rule, and the reference adds only the object that today matches by its own id. One window is left:
   after the owner or the reflector is destroyed and before the laser refreshes its reflector or dies. In that window
   the old rule matched the entity that took a freed random id, and did not match the same pooled object after it
   spawned again with another id; the new rule does the opposite. Random ids no longer exist, so this window cannot be
   kept. For a `LaserBeamer` owner, the window lasts at most until the next `engine.update()` (the beamer destroys its
   laser in `onDestroy`), and the beamer object must spawn again at another map object in that time. The temporary
   suite counts the window cases.
7. `DarknessV3`: delete `C/entities/special/DarknessV3.kt`, and its commented mention in `SpecialsFactory.kt:49` if
   the line still compiles to the same code. No map, no factory and no other code uses it.
8. `FireFlower`: no code change. Intended change, listed in `CONTRACT.md`: a fireball from a `FireFlower` with no
   `block` property (`SMB3_1` object `117`) no longer ignores an entity that holds the random id `-1`. With no random
   ids, `-1` matches no entity.

Docs: root `CLAUDE.md`, the `MegaGameEntity` line: `id` is gone; add one line: the map object id is a
`MapObjectComponent` (`mapObjectId`), present only on entities spawned with `ConstKeys.ID`.

Tests:

9. Temporary `Temp064bMapIdEquivalenceTest` with `Temp064bFrozenIdRegistry` (core test sources): a frozen copy of the
   old id logic (`RANDOM_ID_POOL` with the eager fetch, the old `idToEntities`). Seeded sequences on a real
   `GameEngine` with pooled test entities: spawns with and without `ConstKeys.ID`, shared ids (successor pattern),
   destroy and spawn in one frame, respawn of a pooled object with another id or with none, `engine.reset()`. After
   each operation and from a probe during an update: `getOfId`/`existsAnyOfId` for every map id give the same ordered
   sets as the frozen copy.
10. Temporary `Temp064bLaserIgnoreEquivalenceTest`: the old `Laser` ignore rule (frozen, with frozen random ids) against
    the new rule, on seeded sequences of owner, reflector and other entities (spawn, destroy, pooled respawn, freed
    value reuse, shared map ids). Asserts equal results in every case where the owner and the reflector are live in the
    spawn that was added; counts the window cases of item 6 and checks that they are the only differences.
11. Permanent core tests: the component exists only after a spawn with `ConstKeys.ID`; a pooled respawn without it
    removes the component; the id is readable in `onDestroy`; the registry finds map entities by id, shared ids give a
    set in spawn order, runtime entities are not indexed; `Laser` ignores its owner and its reflector by reference, the
    owner's and the reflector's map ids, and the Tiled ids by map id.
12. Keep `StarknerAliveTest` and (after a merge of `master`) `SpiderWebDestroyTest` and `OnDestroySuperCallTest` green
    with no change: both id tests spawn with `ConstKeys.ID`.

New API and first production caller:

- `MapObjectComponent`: `MegaGameEntity.onSpawn` (writes it); the extension `mapObjectId` (reads it).
- `IGameEntity.mapObjectId`: `MegaGameEntities.add`/`remove`, the readers in item 5 (for example `Rain`,
  `TubeBeamerV2`, `Laser`, `SmallIceCube`, `Starkner`).
- `Laser.ignoringEntities` (private): `Laser.isLaserIgnoring`.

Removed API: `MegaGameEntity.id`, `RANDOM_ID_POOL`, `CURRENT_NEW_ID`, class `DarknessV3`.

## Out of scope

- A class 1 reader that could use a reference instead of a map id (for example `MockingByte` and its nest,
  `GreenKoopa` and its shell). They key one map object on purpose (a key that outlives the entity, or is shared by
  successors); they stay on map ids.
- `IFixture.getEntity()` and `IBody.getEntity()` cast to `MegaGameEntity` (backlog candidate).
- Typed spawn schemas (`022`). The `parent_id` keys of koopa shells and moon flags stay.
- The registry outside of ids (tags, types).
- `Darkness` and `DarknessV2` (they do not read the entity id).

## Affected areas and risks

- Files: `MegaGameEntity.kt`, `MegaGameEntities.kt`, new `MapObjectComponent.kt` and its extension, and the readers:
  `Block`, `FallingLeaves`, `Rain`, `AstroAssAssaulter`, `Bat`, `BikerKibbo`, `BulletBill`, `GreenKoopa`, `Jetto`,
  `MockingByte`, `RedKoopa`, `Starkner`, `SteamRollerMan`, `Laser`, `SmallIceCube`, `TubeBeamerV2`, `FallingIcicle`,
  `FireWall`, `Fireball`, `SpitFireball`, `Gate`, `GauntletMaster`. Deleted: `DarknessV3.kt`. Root `CLAUDE.md`.
- Risk: a reader that expected a negative id for a runtime entity. The scan lists all of them (class 3).
- Risk: an id read after the component is gone. The component is never cleared at destroy; it is only removed at the
  next spawn without `ConstKeys.ID`.
- Risk: `Starkner` `ALIVE` behavior (fixed by `fix/starkner-alive-cleanup`). `StarknerAliveTest` stays green.
- Cost: a component lookup (one map get by `KClass`) in place of a field read, per contact in the class 2 readers
  (`RainDrop` on each block or body hit, `Laser` on each contact). One fewer pool fetch and a smaller registry per
  runtime spawn (no `idToEntities` set for each runtime entity).

## Overlap with other branches

`fix/spider-web-super-on-destroy` merged into `master` (`9d3f67fc5`) after the base. It adds `SpiderWebDestroyTest`
(uses `ConstKeys.ID` and `getOfId`) and `OnDestroySuperCallTest` (every `onDestroy()` override calls `super`; the base
`MegaGameEntity.kt` is exempt). No shared file with this scope. The branch merges `master` before its merge. Details in
`docs/NOTES.md`.

## Open questions

None. Resolved by the owner (2026-10-28, "064b scope is approved."):

1. Readers use the extension `val IGameEntity.mapObjectId: Int?` over the component.
2. `MegaGameEntities` keeps `idToEntities` and reads the component at `add`/`remove`; no `MapObjectsSystem`.
3. Delete `DarknessV3.kt` in this branch.
4. Own-id readers use `mapObjectId!!`.
5. The exact option, not the recommendation: `Laser` holds the owner and the reflector by reference and also adds the
   owner's map id to the `Int` set when the owner has one. Added during step 6 (owner, 2026-10-28: "yes, accept the
   reflector map id"): the reflector's map id also goes in the `Int` set, because on `master` a reflecting laser also
   ignores entities that share the reflector's map id.
6. Accept the `FireFlower` `-1` change as an intended change with no code change.
7. Names: `MapObjectComponent` with the field `id`; extension `mapObjectId`.
8. Real rooms for the manual checks; no `RefactorTest064` map.

Also confirmed by the owner: `onSpawn` removes the whole component when a spawn has no `ConstKeys.ID`; the component
`id` is a non-null `Int`; its `reset()` keeps the id.
