# 064: Entity instance identity: `id` and `group`

- Status: active
- Type: refactor
- Depends on: none
- Related: `021`, `022`, `048`, `063`
- Branches: `mega-refactor-064a-instance-identity` (active, started 2026-10-27)

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

## Open questions

None.

## Sources

- `NOTES.md` in this folder.
- `063a` branch `docs/TODO.md`.
