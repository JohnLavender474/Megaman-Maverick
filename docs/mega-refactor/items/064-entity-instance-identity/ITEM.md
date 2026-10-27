# 064: Entity instance identity: `id` and `group`

- Status: idea
- Type: refactor
- Depends on: none
- Related: `021`, `022`, `048`, `063`
- Branches: none yet

## Goal

Decide where an entity's instance identity lives. Today `MegaGameEntity` has two fields, `id` (the Tiled map object
id from `ConstKeys.ID`, or a negative id from `RANDOM_ID_POOL`) and `group` (the optional `ConstKeys.GROUP` spawn
property). `MegaGameEntities` uses them as lookup keys. Code that holds a contract type casts to `MegaGameEntity` to
read `id` (4 sites: `Rain.kt:200`, `TubeBeamerV2.kt:208`, `Laser.kt:677`, `SmallIceCube.kt:195`, and
`GameEntityUtils.kt`).

## Proposed scope

Not approved. Options: fields on `IMegaGameEntity` (the `063a` backlog candidate); spawn properties; a component; or
keys that only the entity registry (`MegaGameEntities`) holds.

## Owner's input

2026-10-27 (verbatim, about the `063a` backlog candidate "`id` and `group` on `IMegaGameEntity`"): "Do other game
engines enforce fields like this on entity objects? If not, then do you think id and group are game concepts or engine
concepts? If game concepts, then would props instead of fields be better? Or components?"

## Open questions

1. Engine or game concept? Fields, properties, a component, or registry-only keys?

## Sources

- `NOTES.md` in this folder.
- `063a` branch `docs/TODO.md`.
