# 064: Entity instance identity: `id` and `group`

- Status: done
- Type: refactor
- Depends on: none
- Related: `021`, `022`, `048`, `063`
- Branches: `mega-refactor-064a-instance-identity` (merged 2026-10-28, squash `c86d67421`; groups), `mega-refactor-064b-map-object-id` (merged 2026-10-28, squash `897d87f27`; `id`)

## Goal

Decide where an entity's instance identity lives. Today `MegaGameEntity` has two fields, `id` (the Tiled map object
id from `ConstKeys.ID`, or a negative id from `RANDOM_ID_POOL`) and `group` (the optional `ConstKeys.GROUP` spawn
property). `MegaGameEntities` uses them as lookup keys. Code that holds a contract type casts to `MegaGameEntity` to
read `id` (4 sites: `Rain.kt:200`, `TubeBeamerV2.kt:208`, `Laser.kt:677`, `SmallIceCube.kt:195`, and
`GameEntityUtils.kt`).

## Proposed scope

Not approved. The branch `SCOPE.md` decides the final scope. From the owner decision below:

- `val id` moves to `IMegaGameEntity` (implemented by `MegaGameEntity`). The 4 `(x as MegaGameEntity).id` casts and
  the `GameEntityUtils.kt` casts that exist only for `id` go.
- `group` leaves the entity. `MegaGameEntities` keeps both sides (group to entities, and entity to group for the
  cleanup at destroy); the spawn code gives the registry the `ConstKeys.GROUP` value. The 3 readers ask the registry.

## Owner's input

2026-10-27 (verbatim, about the `063a` backlog candidate "`id` and `group` on `IMegaGameEntity`"): "Do other game
engines enforce fields like this on entity objects? If not, then do you think id and group are game concepts or engine
concepts? If game concepts, then would props instead of fields be better? Or components?"

## Decisions

- 2026-10-27 (owner, "Agreed"): both are game concepts and stay in `core`. Engines built on objects (Unity, Godot,
  Unreal) give every object a name and tags or groups as fields; pure ECS engines (Bevy, EnTT, flecs) give only an
  entity ID and make a name or a group an optional component. Here the engine identity is the object reference.
  - `id`: a field on `IMegaGameEntity`. Every game entity has one, it has about 20 reads, some on hot paths (the
    registry lookup, `Starkner.canSpawn`); a component would add a map lookup to each read for no gain.
  - `group`: not a field. Only 3 reads; the registry (`MegaGameEntities`) already holds the group-to-entities map and
    also holds the entity-to-group side. If `048` later needs groups in queries, a marker component is the form.
  - Not spawn properties: an untyped bag with string keys and a map lookup per read; `022` moves to typed schemas.
- 2026-10-27 (owner: "Why `IMegaGameEntity` and not `IGameEntity`? And could the group-registry logic be moved as a
  first-class component into the engine? Maybe a \"groups component\"? I don't mind the component lookup if it leads
  to better design"; then: "Would it still make sense to make core's concept of id be a component or prop as well?";
  then "Yes to all"). This replaces the first decision above:
  - `064a`: groups become an engine `GroupsComponent` (a set of group names) with an engine group query, kept up to
    date on spawn and destroy. `MegaGameEntity` loses `group`; `MegaGameEntities` loses its group map; the 3 readers
    use the engine. Groups are a genre-neutral engine feature (as in Godot); the group lookups must give the same
    results at the same moments as today (proved by a temporary old-versus-new suite).
  - `064b`: `id` becomes a `core` component (`MapObjectComponent`, name to confirm in scope) present only on entities
    spawned from a map. `RANDOM_ID_POOL` goes; readers that use `id` as a key for runtime entities (`Laser` owner,
    `DarknessV3` light sources) change to entity references. The 4 `(x as MegaGameEntity).id` casts go through
    `getComponent` on `IGameEntity`. Reason: `id` is level data, not an instance identity; it is not unique
    (`idToEntities` maps an id to a set; `Starkner` successors share an id) and it changes on each spawn of a pooled
    entity. Not on `IGameEntity`: the engine identity is the object reference. Not a property: untyped, string keys.
- 2026-10-27 (owner, about the `064a` scope draft): "I'm not sure the engine should be responsible for handling
  groups. That seems more like it should be a separate system (literally a system)." and "I think having an index like
  today's `MegaGameEntities.kt` is preferable. We can open up `GameSystem`'s funcs and/or add hooks as needed." So
  `064a` adds a `GroupsSystem` (mask `GroupsComponent`) that keeps a group-to-entities index and owns the query;
  `GameEngine` does not get a group index. `GameSystem` may get join/leave hooks or open functions for it. The engine
  already updates system membership right after `onSpawn` and before the component `reset()` at destroy.
- 2026-10-27 (owner, "Agreed"): `SpiderWeb.onDestroy` skips `super.onDestroy()` (the `064a` bug candidate). A
  `fix/` branch after `064a` adds the call; then a permanent test checks that every `onDestroy()` override calls
  `super.onDestroy()`. `init()` and `super.init()` go to new item `065`.

## Open questions

None.

## Sources

- `NOTES.md` in this folder.
- `063a` branch `docs/TODO.md`.
