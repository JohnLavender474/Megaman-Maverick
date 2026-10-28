# Scan: `064a` (entity groups in the engine)

Base: `master` at `ce10fc4b1`. `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`, `C/` =
`core/src/main/kotlin/com/megaman/maverick/game/`.

The scope changed on 2026-10-27 (owner decision, item `ITEM.md`): `064a` does groups only; `id` goes to `064b`. The
first scan (commit `bf74e0a06`) also covered `id`; that part is removed here.

## 1. Where `group` lives today

- `C/entities/contracts/MegaGameEntity.kt:33-34`: `var group: String? = null` with a `private set`.
- `MegaGameEntity.onSpawn` (`:53-60`), in this order: `id`, `group = spawnProps.get(ConstKeys.GROUP, String::class)`,
  `timeSpawned`, `MegaGameEntities.add(this)`, then `runnablesOnSpawn`.
- `MegaGameEntity.onDestroy` (`:68-74`), in this order: `MegaGameEntities.remove(this)`, `runnablesOnDestroy`, free a
  negative `id`, `group = null`.
- `C/entities/MegaGameEntities.kt` (a global `object`): `groupToEntities: OrderedMap<String,
  OrderedSet<MegaGameEntity>>` (`:13`). `add` (`:20`) puts the entity in the set of `entity.group`; `remove`
  (`:34-40`) takes it out of the set of `entity.group` and drops the set when it is empty. `getOfGroup` (`:73`) returns
  the live set, or a new empty set for an unknown group.

## 2. Readers of `group`

| File:line | Read |
|---|---|
| `C/entities/MegaGameEntities.kt:20` | `add` |
| `C/entities/MegaGameEntities.kt:34` | `remove` |
| `C/entities/utils/GameEntityUtils.kt:91` | `getStandardBodyDoUpdatePredicate`: `(entity as MegaGameEntity).group`; if not null, the body updates when any member of the group is near the camera (`getUpdateWhenGroupNearCameraPredicate`, `:72-85`, the only caller of `getOfGroup`, at `:79`) |

`getStandardBodyDoUpdatePredicate` has 2 callers: `Block.onSpawn` (`C/entities/blocks/Block.kt:102`) and
`GravityBlock.onSpawn` (`C/entities/blocks/GravityBlock.kt:111`). Both call it after `super.onSpawn(spawnProps)`. The
predicate is the body's `doUpdate`; `GravityBlock` also gives it to its inner `Block` (`ConstKeys.DO_UPDATE`). So
`getOfGroup` runs only when a system calls `doUpdate`, that is, during `engine.update()`, in the systems loop
(`E/GameEngine.kt:240`). It never runs inside `onSpawn` or `onDestroy`.

The only writer of the `group` key is Tiled. No Kotlin code puts `ConstKeys.GROUP` in a `Properties`. Live maps (the 20
files that `LevelDefinition` names) that use `group`:

- `MoonMan_16x14_v5.tmx`, room `room10`: `GravityBlock` and plain `Block` objects in groups `room10_group1` to
  `room10_group6`, all with `update_distance=3`.
- `WilyStage3_v4.tmx`, room `room5`: `GravityBlock` (`update_distance=3`) and plain `Block` (`update_distance=6`) in
  groups `room5_group1` and `room5_group2`.

Each object has one group name. No name has a comma.

## 3. Engine lifecycle (`E/GameEngine.kt`)

- `spawnNow` (`:119-131`): `entities.add`, `init()` on the first spawn, `onSpawn(spawnProps)`, write `existenceState`
  (`LIVE`, or `DESTROY_QUEUED`/`RESPAWN_QUEUED`), `updateSystemMembershipsFor(entity)`.
- `destroyNow` (`:186-198`): `entities.remove`, remove from each system, `reset()` on each component, `onDestroy()`,
  write `existenceState`.
- `update` (`:214-245`): all queued spawns (`spawnNow`), then all kills (`destroyNow`), then respawns (`spawnNow`), then
  the systems. `destroyNow` and `spawnNow` never run during the systems loop.
- `reset()` (`:247-260`; deferred to the end of `update` if called during it) and `dispose()` (`:263-273`): `destroyNow`
  for each entity in `entities` with `existenceState.liveNow`, then `entities.clear()`, the queues and the systems.
  `spawnNow` always leaves a `liveNow` state, and the only writes that clear `liveNow` happen in `destroyNow` or for
  entities that are not in `entities`, so every entity in `entities` is `liveNow` at `reset()`. No `core` code writes
  `existenceState`.
- `IGameComponent.reset()` has an empty default. The engine calls it on every component at destroy, before
  `onDestroy`. No code calls `clearComponents()`.
- Many `init()` overrides do not call `super.init()` (for example `DynamicBlock`, `WilyFinalBoss`, `Tree`), so
  `MegaGameEntity` cannot add a component in `init()`. A Kotlin `init {}` block or `onSpawn` can.

## 3a. `GameSystem` membership (`E/systems/GameSystem.kt`)

All points where an entity joins or leaves `entities` (a `MutableOrderedSet`, so the order is the join order):

| Point | When | Effect |
|---|---|---|
| `add(e)` (`:56-64`), not updating | from `GameEngine.updateSystemMembershipsFor` (`spawnNow`) | if `qualifies(e)`: `entities.add(e)` at once (no change if `e` is already a member) |
| `add(e)`, updating | only during the system's own `update` | `entitiesToAdd.add(e)` |
| `remove(e)` (`:46-54`), not updating | from `destroyNow` (`systems.forEach { it.remove(entity) }`), and from `updateSystemMembershipsFor` when `e` does not qualify | if a member: `entities.remove(e)` at once |
| `remove(e)`, updating | only during the system's own `update` | `entitiesToRemove.add(e)` |
| `update` (`:68-81`) | start of the system's update | `addAll(entitiesToAdd)`, `removeAll(entitiesToRemove)`, `removeAll { !qualifies(it) }`, then `process` |
| `reset()` (`:83-86`) | `engine.reset()` and `dispose()`, after the `destroyNow` loop | `entities.clear()`, `entitiesToAdd.clear()` (`entitiesToRemove` is not cleared) |

Facts that matter for a `GroupsSystem`:

- `spawnNow` and `destroyNow` never run during a system's `update` (they run at the start of `engine.update()` or in a
  deferred `reset()`). So for the engine calls, `add` and `remove` always apply at once. The deferred paths need a
  `remove` or `add` call from inside the same system's `process`; no code does that today (no `core` code calls
  `updateSystemMembershipsFor`, and `CullablesSystem` only queues a destroy).
- The join happens in `spawnNow` after `onSpawn` and after the `existenceState` write. The leave happens in
  `destroyNow` before the component `reset()` and `onDestroy`.
- An entity is never a member when `spawnNow` runs: `destroyNow` removed it, or it never spawned, or `reset()` cleared
  the set. So `updateSystemMembershipsFor` does not re-join a member. A hook must still fire only on a real change
  (`entities.add`/`remove` return `true`).
- `qualifies` checks only that the components exist. A `GroupsComponent` emptied by `reset()` still qualifies, but the
  `reset()` happens after the leave. A pooled entity that respawns with another group joins again after its `onSpawn`
  wrote the new name. A pooled entity that respawns with no group keeps an empty `GroupsComponent`, so it joins with no
  names.
- `removeAll { !qualifies(it) }` drops a member whose component was removed. No code removes a `GroupsComponent`.
- `on = false` (pause, level end: `LevelStateHandler.kt:16-18`, `PlayerStatsHandler.kt:140`, `:173`, `:317`) changes
  only the `on` argument of `process`; membership still changes. Old group lookups did not depend on `on`.
- `process` is `internal abstract`, so a `GameSystem` subclass must be in the `engine` module.
- The game keeps the systems in `ConstKeys.SYSTEMS` (`MegamanMaverickGame.kt:934-936`) and `getSystem(KClass)`
  (`:991-995`) finds one by its class name.
- `MutableOrderedSet.retainAll` (`E/common/objects/MutableOrderedSet.kt:10-19`) iterates its argument, not itself, so it
  does not do what its name says. No `GameSystem` path uses it. Not in scope.

## 4. When the old registry registers and unregisters

- Register: inside `MegaGameEntity.onSpawn`, so inside `spawnNow` between `onSpawn` start and the `existenceState`
  write. During `onSpawn` the state is still `SPAWN_QUEUED`, `RESPAWN_QUEUED` or `UNSPAWNED` (a spawn and a destroy in
  one frame).
- Unregister: inside `MegaGameEntity.onDestroy`, so inside `destroyNow` after the system removal and the component
  `reset()`. From `destroy()` until the next `engine.update()` the entity stays in its group with the state
  `DESTROY_QUEUED`.
- A respawn runs `destroyNow` and then `spawnNow`, so a pooled or respawned entity always gets `remove` before the next
  `add`. A pooled entity that spawns again reads `ConstKeys.GROUP` again; with no `group` key the value is `null`.
- `engine.reset()`: every indexed entity is `liveNow`, so `destroyNow` (and `remove`) runs for each. Queued spawns
  never reach `add`.
- Exception: `SpiderWeb.onDestroy` (`C/entities/projectiles/SpiderWeb.kt:118-132`) does not call `super.onDestroy()`.
  A destroyed `SpiderWeb` stays in the registry. `SpiderWeb` is never in a group (code spawns it; no map names it), so
  the group index is not affected today. Bug candidate in `TODO.md`.
- Every other `onSpawn`/`onDestroy` override in `core/` calls `super`.

Consequence: a `GroupsSystem` that indexes an entity when it joins (in `spawnNow`, after `onSpawn`) and drops it when
it leaves (in `destroyNow`, before the component `reset()`, with the names stored at the join) holds the same entities
in the same order as the old registry at every moment when a group query can run: during any system's `update`
(including from inside another system, for example a `doUpdate` predicate in `WorldSystem`), and at any time between
two `engine.update()` calls. The old and new differ only inside `spawnNow` (between the old add in `onSpawn` and the
join) and inside `destroyNow` (between the leave and the old remove in `onDestroy`), where no query runs. One case
differs on purpose: an entity that skips `super.onDestroy()` and has a group (none exists; accepted by the owner).

## 5. Overlap with `063b`

`063b` (`.worktrees/mega-refactor-063b-damage-entity-rename`, at `02d1a4eb7`, scope and contract approved, plan in draft, no code yet) is a
mechanical rename of `IDamageable`/`IDamager` to `IDamageableEntity`/`IDamagerEntity` (kept in `engine`), plus
`IChildEntity`/`IParentEntity : IGameEntity`. None of the files that `064a` changes (`GameSystem.kt`, the new engine
groups files, `MegamanMaverickGame.kt`, `MegaGameEntity.kt`, `MegaGameEntities.kt`, `GameEntityUtils.kt`)
names these types, and `063b` does not touch `GameSystem`. Possible shared file: root `CLAUDE.md`, if both branches
edit it (different lines).
