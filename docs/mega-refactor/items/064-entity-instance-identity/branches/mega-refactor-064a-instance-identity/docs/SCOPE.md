# Scope: mega-refactor-064a-instance-identity

- Status: approved
- Approved: 2026-10-27

## Goal

Entity groups are an engine feature in their own system. An entity holds its group names in an engine
`GroupsComponent`. An engine `GroupsSystem` (mask `GroupsComponent`) keeps a group-to-entities index like the one in
today's `MegaGameEntities`, and owns the query `getEntitiesInGroup(group)`. `GameEngine` gets no group code.
`MegaGameEntity` has no `group` field and `MegaGameEntities` has no group map. Group lookups give the same results at
the same moments as today. No gameplay change.

## Item

From `064` `ITEM.md`, "Decisions" (2026-10-27; the later entries replace the earlier ones):

- "`064a`: groups become an engine `GroupsComponent` (a set of group names) with an engine group query, kept up to
  date on spawn and destroy. `MegaGameEntity` loses `group`; `MegaGameEntities` loses its group map; the 3 readers use
  the engine. Groups are a genre-neutral engine feature (as in Godot); the group lookups must give the same results at
  the same moments as today (proved by a temporary old-versus-new suite)."
- Owner: "I'm not sure the engine should be responsible for handling groups. That seems more like it should be a
  separate system (literally a system)." and "I think having an index like today's `MegaGameEntities.kt` is
  preferable. We can open up `GameSystem`'s funcs and/or add hooks as needed." So a `GroupsSystem` keeps the index and
  owns the query; `GameEngine` does not get a group index.
- Approved at gate 1 (2026-10-27): the query name `getEntitiesInGroup(group)`; a read-only return type with no
  allocation per call; the component is added in `MegaGameEntity.onSpawn` only when the spawn has a group; a set of
  names; real rooms for the manual checks (no `RefactorTest064` map); an entity that skips `super.onDestroy()` and has
  a group leaves the index (intended change with no effect on current content, with a permanent test).
- Not in this branch: `id` (`064b`); `super.init()` (item `065`); the `SpiderWeb` `super.onDestroy()` fix (a `fix/`
  branch after `064a`).

## Current state

Full scan: `docs/SCAN.md`. Base: `master` at `ce10fc4b1`. `E/` = `engine/src/main/kotlin/com/maverick/game/engine/`,
`C/` = `core/src/main/kotlin/com/megaman/maverick/game/`.

- `C/entities/contracts/MegaGameEntity.kt:33-34` holds `group`. `onSpawn` (`:55`) reads `ConstKeys.GROUP` and then
  calls `MegaGameEntities.add`; `onDestroy` (`:69`, `:72`) calls `MegaGameEntities.remove` and then sets
  `group = null`.
- `C/entities/MegaGameEntities.kt:13`, `:20`, `:34-40`, `:73`: the group map, its updates, and `getOfGroup`.
- The 3 readers: `MegaGameEntities.kt:20` and `:34`, and `C/entities/utils/GameEntityUtils.kt:91` (with a cast to
  `MegaGameEntity`). `getOfGroup` has one caller (`GameEntityUtils.kt:79`). Group queries run only from the
  `doUpdate` predicate of `Block` and `GravityBlock`, so only during a system's `update` (`WorldSystem`).
- Only Tiled sets groups: `MOON_MAN` room `room10` and `WILY_STAGE_3` room `room5` (one name per object).
- `GameSystem` (`SCAN.md` section 3a): the engine adds an entity to every qualifying system in `spawnNow` right after
  `onSpawn`, and removes it from every system in `destroyNow` before the component `reset()`. `add`/`remove` are
  deferred only during the system's own `update`, which the engine never does today. `update` also drops members that
  no longer qualify; `reset()` clears the members. `GameSystem` has no hook for a join or a leave.
- The old registry adds inside `onSpawn` and removes inside `onDestroy` (after the component `reset()`). A respawn
  always leaves before it joins again; at `engine.reset()` every grouped entity is destroyed first.
- `SpiderWeb.onDestroy` skips `super.onDestroy()`. It never has a group. Bug candidate in `TODO.md`.

## In scope

Engine, `GameSystem` (`E/systems/GameSystem.kt`):

1. Two hooks: `protected open fun onEntityJoined(entity: IGameEntity) {}` and `protected open fun
   onEntityLeft(entity: IGameEntity) {}`. They fire only on a real change of `entities`, at every point where the
   change happens:
   - `add` when not updating, if `entities.add(e)` returns `true`;
   - `update`, for each deferred add that joins and each deferred remove that leaves (per element, in place of
     `addAll`/`removeAll`), and for each member that `removeAll { !qualifies(it) }` drops;
   - `remove` when not updating, if the entity was a member;
   - `reset()`, for each member, before `entities.clear()`.

   With the empty defaults, every existing system behaves as before (same members, same order). No new allocation per
   frame.

Engine, new package `E/groups/`:

2. `GroupsComponent`: an `IGameComponent` with a set of group names. `reset()` clears the set.
3. `GroupsSystem : GameSystem(GroupsComponent::class)`:
   - an index `OrderedMap<String, MutableOrderedSet<IGameEntity>>` (group to members in join order, as
     `groupToEntities`), and `ObjectMap<IGameEntity, ...>` with the names each entity was indexed under;
   - `onEntityJoined`: reads the names from the entity's `GroupsComponent` and adds the entity to each group;
   - `onEntityLeft`: removes the entity from the groups stored at the join (not from the component, which `reset()`
     empties later), and drops a group set when it is empty (as today);
   - `process` does nothing;
   - `getEntitiesInGroup(group: String): Collection<IGameEntity>`: the internal set, typed as the read-only Kotlin
     `Collection`, or one shared empty set. No allocation per call; iteration uses the LibGDX cached iterator, as
     today.

Core:

4. `MegamanMaverickGame.createGameEngine()` adds a `GroupsSystem` as the last system, after `AudioSystem`. It is then in
   `ConstKeys.SYSTEMS` and found by `getSystem(GroupsSystem::class)`.
5. `MegaGameEntity` loses `group`. `onSpawn` reads `ConstKeys.GROUP` at the same place as today; if the value is not
   `null`, it gets or adds the entity's `GroupsComponent` and puts the name in it. `onDestroy` loses `group = null`.
6. `MegaGameEntities` loses `groupToEntities`, the group updates in `add`/`remove`, and `getOfGroup`.
7. `GameEntityUtils.getStandardBodyDoUpdatePredicate` reads the names from the entity's `GroupsComponent` (no cast for
   the group). `getUpdateWhenGroupNearCameraPredicate` asks `GroupsSystem.getEntitiesInGroup`, with the system from
   `game.getSystem(GroupsSystem::class)` taken once when the predicate is built, and checks
   "any member of any of the entity's groups is near the camera". With one name the result is the same as today.

Docs: root `CLAUDE.md`: the `MegaGameEntity` line drops `group`; the system order line names `GroupsSystem`; the engine
section says that groups are a `GroupsComponent` and a `GroupsSystem`.

Tests:

8. Temporary `Temp064aGroupIndexEquivalenceTest` (core test sources) with `Temp064aFrozenGroupRegistry`, a frozen copy
   of the old group logic of `MegaGameEntities` and `MegaGameEntity`. The test entity keeps an old-style group field and
   feeds the frozen copy at the old points (in `onSpawn`, and in `onDestroy` only when it calls `super.onDestroy()`).
   Seeded random sequences of spawn, destroy, a destroy and a spawn in one frame, a respawn with another group or with
   none, pooled and non-pooled entities, `engine.reset()`, and systems turned off (`on = false`). The suite compares
   the ordered members of every group in `GroupsSystem` and in the frozen copy: from probe systems placed before and
   after `GroupsSystem` in the system list, during their `update` (a query from inside another system's update), and
   after each operation between updates. An entity that skips `super.onDestroy()` is included without a group.
   Deleted before the merge.
9. Permanent engine tests:
   - `GameSystemHooksTest`: each join and leave path in item 1 (including the deferred paths, the drop of a member that
     no longer qualifies, and `reset()`) fires the hook once, in order; an `add` of a member fires nothing; the members
     and their order are the same as without hooks.
   - `GroupsSystemTest`: through a real `GameEngine`: an entity is in its groups from the join after `onSpawn` to the
     leave before the component `reset()`; destroy then spawn in one frame; respawn with another group or with none;
     the member order is the join order; an unknown group gives an empty result; `reset()` and `dispose()` leave the
     index empty; a query from inside another system's `update` sees the spawns of the same frame.
10. Permanent core tests: `MegaGameEntity` puts the `ConstKeys.GROUP` value into its `GroupsComponent` and adds no
    component with no group; a `MegaGameEntity` that skips `super.onDestroy()` and has a group leaves the index at
    destroy (the accepted change).

New API and first production caller:

- `GameSystem.onEntityJoined` / `onEntityLeft`: `GroupsSystem` (overrides both).
- `GroupsComponent`: `MegaGameEntity.onSpawn` (writes it), `GroupsSystem.onEntityJoined` and
  `GameEntityUtils.getStandardBodyDoUpdatePredicate` (read it).
- `GroupsSystem`: `MegamanMaverickGame.createGameEngine()`.
- `GroupsSystem.getEntitiesInGroup`: `GameEntityUtils.getUpdateWhenGroupNearCameraPredicate`.

Removed API: `MegaGameEntity.group`, `MegaGameEntities.getOfGroup`.

## Out of scope

- `id`, `RANDOM_ID_POOL`, the `(x as MegaGameEntity).id` casts: `064b`.
- `super.init()` in `init()` overrides: item `065`.
- The `SpiderWeb.onDestroy` fix: a `fix/` branch after `064a`.
- The `getGameCamera()` casts in `GameEntityUtils.kt:56`, `:62`, `:73`.
- `GameSystem.reset()` does not clear `entitiesToRemove`; `MutableOrderedSet.retainAll` iterates the wrong collection.
  Neither path is reached today. The first is a backlog candidate in `TODO.md`; the second is filed as
  `docs/issues/2026-10-22-mutable-ordered-set-retain-all.md`.
- Group changes during an entity's life: the index reads the component only at the join. No caller changes groups
  during a life.
- More than one group per Tiled object (for example a comma-separated `group`).
- Groups in a general entity query (`048`).

## Affected areas and risks

- Engine: `GameSystem.kt` (hooks), new `E/groups/GroupsComponent.kt` and `GroupsSystem.kt`. Core:
  `MegamanMaverickGame.kt` (one system), `MegaGameEntity.kt`, `MegaGameEntities.kt`, `GameEntityUtils.kt`. Root
  `CLAUDE.md`.
- `GameSystem` is the base of every system. The hook calls replace `addAll`/`removeAll` with loops that give the same
  members in the same order. Risk: a change in member order or a missed member in another system. The existing engine
  tests and `GameSystemHooksTest` cover it.
- The group index moves from `onSpawn`/`onDestroy` to the system join and leave. Risk: blocks in `MOON_MAN` `room10` or
  `WILY_STAGE_3` `room5` that freeze or move at the wrong time. The temporary suite and the play test cover it.
- A new system in the list: the pause and level-end code turns every system off and on (`on`), which does not affect
  the index. Code that iterates `engine.systems` (diagnostics, the `ConstKeys.SYSTEMS` map) sees one more system.
- Cost: one hook call per join and leave for each system (an empty call for all but `GroupsSystem`); one component
  lookup per grouped entity at spawn; the query costs one map lookup, as today.

## Overlap with other branches

`063b` (rename of `IDamageable`/`IDamager`, kept in `engine`; `IChildEntity`/`IParentEntity : IGameEntity`; at
`02d1a4eb7`, no code yet) names none of the files above and does not touch `GameSystem`. Only root `CLAUDE.md` can be
shared (different lines). Details in `docs/NOTES.md`.

## Open questions

None. Resolved by the owner (2026-10-27, "Yes to both", with the recommendations):

1. `GroupsSystem` is last in the system list, after `AudioSystem`.
2. `GameEntityUtils` gets the system once through `game.getSystem(GroupsSystem::class)` when the predicate is built in
   `onSpawn`, and keeps it in the predicate. No new `MegamanMaverickGame` property.
3. The hooks are `onEntityJoined`/`onEntityLeft`.
