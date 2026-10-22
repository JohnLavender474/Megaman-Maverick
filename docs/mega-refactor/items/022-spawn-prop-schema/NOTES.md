# Notes: 022 Typed property schema per entity, exported to Tiled

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Data-driven design (2026-10-22)

Copied as written from the review section "ITEM 21: TYPED SPAWN PROPERTIES AND SCHEMA PER ENTITY".

````text
(1) Current state

- `MapObject.convertToProps()` copies all Tiled properties into an untyped `Properties` bag (`Extensions.kt:54-77`).
- Entities read the bag in `onSpawn` with string keys and inline defaults. I count 1411 `spawnProps.get` / `getOrDefault` / `containsKey` calls in 415 files.
- Example, `SniperJoe.kt:198-223`:
  - `direction` default `UP`
  - `type` default `ORANGE`
  - `gravity_scalar` default 1
  - `scale_bullet` default true
  - `jump` default true
- The same bag is also used for spawns made in code (for example `ConstKeys.POSITION`, `SniperJoe.kt:203`).
- `megaman-maverick.tiled-project` has `"propertyTypes": []`. The repo has no `.tx` templates.
- The maps use Tiled 1.12.2, and object-reference properties are already in use (`Gauntlet1_v2.tmx` has 143).

(2) Options

A. A Kotlin DSL of property descriptors on the entity def (recommended). Example:
```
val TYPE = enumProp("type", SniperJoeType.ORANGE)
val JUMP = boolProp("jump", true)
```
- Shared groups: `EnemyLifecycleProps` has `cull_out_of_bounds`, `cull_time`, `cull_events`, `spawn_type`, `spawn_room` and `respawnable`.
- Entities read `TYPE.read(spawnProps)`.
- Pro: type-safe, one source of truth for the default, and works for code spawns too.
- Con: you touch each entity once.

B. One data class per entity, parsed from `Properties` by a generated parser.
- Pro: very clean entity code.
- Con: needs codegen; code spawns must build the data class.

C. A JSON schema file per entity.
- Pro: modders can read it.
- Con: defaults are in two places (the file and the code), and they drift.

How option A drives the other parts:
- Tiled: a Gradle task or test writes the `propertyTypes` into `megaman-maverick.tiled-project`:
  - one `class` type for each entity, with its members and defaults;
  - `enum` types for `SniperJoeType`, `Direction`, `EventType`;
  - one `.tx` template for each entity under `assets/tiled_maps/templates/<layer>/`.
- Designers then place a template and see typed fields. Tiled shows the defaults until you change them.
- Validator (item 2): the same task writes `schema.json`. The Python tool in `utils/` reads it and reports:
  - unknown properties;
  - wrong types;
  - enum values that do not parse;
  - required properties that are missing;
  - object references that do not resolve.
- Spawn parsing: `prop.read(props)` replaces `getOrDefault`.

Incremental migration with no change to defaults:
1. Add the descriptor API, and add the shared lifecycle group to `AbstractEnemy`.
2. Migrate one entity at a time. Copy each default exactly from its `getOrDefault` call. A schema test asserts that the descriptor default equals the old literal; read it once from the old code by hand.
3. Entities that are not yet migrated export as "untyped". The validator then only checks that the ID exists.
4. Re-export `propertyTypes` in CI and fail if the file is out of date.

(3) Recommendation

Option A, migrated in order of TMX usage (enemies first).

(4) Open questions for the owner

- Should the exported Tiled files be committed to the repo or generated locally?
- Should old maps get object classes set by a script, or only new placements?

(5) Backlog wording

Title: "Typed property schema per entity, exported to Tiled"
- Property descriptors (name, type, default, required, doc) on each `EntityDef`, plus shared groups (lifecycle, gravity).
- Export `propertyTypes`, enums and `.tx` templates into `megaman-maverick.tiled-project`. Export `schema.json` for the validator.
- Migrate entity by entity. The defaults stay byte-equal to the current `getOrDefault` literals.
- CI fails when the exported files are out of date.

(6) Dependencies

Needs 20. Feeds 2, 22, 35, 40, and the "Define entities in data files" backlog item.
````
