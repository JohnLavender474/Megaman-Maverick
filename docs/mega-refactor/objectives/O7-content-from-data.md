# `O7`: Content from data

- Status: planned

## Problem

- `P1`: A name in a map must match a Kotlin class name and package. A rename in code breaks a map with only an error
  in the log.
- `P2`: Spawn properties have no declared type or default. A level author must read the entity code to learn them, and
  84% of the `spawn_room` and cull values are redundant (`041`).
- `P3`: Code checks which level is running, for example 9 `MOON_MAN` checks (`025`). A new level cannot reuse that
  behavior with no code change.
- `P4`: A new entity needs Kotlin work, so other developers and level creators cannot add one (`050`).

## Goal

Level authors and other developers build and change content from data and tools, with few code changes.

## Done means

- Tiled shows typed spawn properties for each entity.
- A level describes its own traits.
- A new simple entity comes from a data file.

## Rules

### `O7-R1`: Map names are not class names

- Rule: a name in a map resolves through a registry, not through a Kotlin class name.
- Problem: `P1`
- Check: read `SpawnersLayerBuilder` and the registry.
- Applies: `linked items`
- Active: after `021`

### `O7-R2`: Declared spawn properties

- Rule: a new spawn property has a declared type and default.
- Problem: `P2`
- Check: review the new `spawnProps` reads in the branch diff.
- Applies: `linked items`
- Active: after `022`

### `O7-R3`: Traits, not level identity

- Rule: new code does not check which level is running. A level describes itself with traits.
- Problem: `P3`
- Check: search for `LevelDefinition.` comparisons in gameplay code.
- Applies: `always-on`
- Active: `now`
- Strength: `preference` (owner, 2026-11-10)

## Current state

Audit of `master` at `30d88f0b7`, 2026-11-01. Counts are `grep` or script results, so they can include a few comments.

| Rule | State | Evidence | Closing item |
|------|-------|----------|--------------|
| `O7-R1` | `broken` (waiting) | No registry exists. 5 paths turn a map name into a class: `SpawnersLayerBuilder.kt:76` through `MegaEntityFactory.kt:49`, `SensorsLayerBuilder.kt:30`, `GauntletMaster.kt:143`, `HeartTank.kt:143-146`, `GameEntityUtils.kt:123`. | `021` |
| `O7-R2` | `broken` (waiting) | No schema type exists. `spawnProps` reads: 1413 lines in 416 files. `propertyTypes` in the Tiled project is empty. Entity objects in live maps use 251 property names. | `022` |
| `O7-R3` | `partly` | 0 new checks since `5b4d76c5a`. Old code remains: 14 entity lines in 8 files (9 `MOON_MAN`), 8 in `LevelRewards.kt:11-18`, 2 in `LevelUtils.kt:7-8`. 56 non-import `LevelDefinition.<NAME>` lines in total. | `025` |

## Redo risk

- `029e` `LevelRewards.kt`: `025` moves this table into the manifest (8 lines).
- `016c` `MegaEntityFactory.kt:70-72`: the `primaryConstructor` reflection goes with `021` (1 function). `EntityConstructorsTest` changes too.
- `021`: "the layer stops being a namespace" makes 9 dead objects on real stages start to spawn (`Cactus` x4, `Bat`, `Sealion`, `DuoBallCanon`, `Saw` x2), which changes gameplay on `DESERT_MAN`, `GLACIER_MAN`, `WILY_STAGE_1` and `WILY_STAGE_2` (inference from the resolve rule). The `021` contract must decide this first.

## Items

| Item | Status | Note |
|------|--------|------|
| `021` | planned | [Entity registry with stable readable IDs and aliases](../items/021-entity-registry/ITEM.md) (main) |
| `022` | planned | [Typed property schema per entity, exported to Tiled](../items/022-spawn-prop-schema/ITEM.md) (main) |
| `025` | idea | [Level manifest: traits, variants and rewards instead of level-identity checks](../items/025-level-manifest/ITEM.md) (main) |
| `036` | planned | [Checkpoints as typed Tiled data: respawn point, triggers, order](../items/036-checkpoints-as-data/ITEM.md) (main) |
| `041` | planned | [Lifecycle policy per entity, room inferred from geometry](../items/041-lifecycle-policy/ITEM.md) (main) |
| `050` | planned | [Define entities in data files](../items/050-entities-in-data-files/ITEM.md) (main) |
| `003` | planned | [Level content validator (local tool in `utils/`, CI test)](../items/003-level-validator/ITEM.md) |
| `052` | idea | [Make values configurable](../items/052-configurable-values/ITEM.md) |
| `092` | idea | [Remove old entity versions and the version suffixes](../items/092-old-entity-versions/ITEM.md) |
| `101` | idea | [Typed Tiled fields and checks for camera sections and rooms](../items/101-map-object-schema/ITEM.md) (main) |
| `149` | idea | [Tag changes at runtime, with an index that follows them](../items/149-runtime-tag-changes/ITEM.md) |

## Gaps with no item

None. The objectives audit of 2026-11-01 gave each gap an item or a note on an existing item.

## Decisions

## Open questions
