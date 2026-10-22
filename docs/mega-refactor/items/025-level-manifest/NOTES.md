# Notes: 025 Level manifest: traits, variants and rewards instead of level-identity checks

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Data-driven design (2026-10-22)

Copied as written from the review section "ITEM 24: LEVEL MANIFEST AND NO LEVEL-IDENTITY CHECKS".

````text
(1) Current state: concrete examples

Adding one robot master level today touches these places:
1. `LevelDefinition.kt`: the enum entry, the TMX path, the music and the mugshot.
2. `Megaman.kt:79-93` `getWeaponsFromLevelDef`: the weapon reward.
3. `RobotMasterIntroScreen.kt`: one `when` branch of ~35-50 lines that builds the intro animation queue (8 branches, lines 304-581). `RobotMasterIntroScreenV2.kt` has the same 8 branches but is not registered (`MegamanMaverickGame.kt:392`).
4. `LevelSelectScreenV2.kt:40-49` `MUGSHOT_POSITIONS`.
5. `MusicAsset` (a new enum value).
6. The boss class.
7. `MegamanWeapon` and `MegamanWeaponsHandler` (item 23).

Save keys:
- The save stores `LevelDefinition` enum names (`GameState.kt:267`, read back with `valueOf` at `GameState.kt:298`).
- If you rename an enum value, old saves break. This is the same stable-ID problem as item 20.

Level-identity checks in entity code:
- MOON_MAN: 9 checks. They mean "low-gravity / vacuum environment":
  - `MoonMan.kt:246,322,664,693`
  - `AbstractItem.kt:112,157` (item gravity and clamp)
  - `MegamanWeaponsHandler.kt:842` (the magma weapon becomes `MagmaExplosion`)
  - `MegamanWeaponsHandler.kt:1036` (axe slowed, the same as `IN_WATER`)
  - `BodyComponent.kt:377` (run speed, next to the `IN_WATER` case)
- In addition, every player spawn in `MoonMan_16x14_v5.tmx` repeats `gravity_scalar=0.35`, `movement_scalar=0.8` and `apply_scalar_to_children=true` (all 10 spawns). So part of the environment is data already, written 10 times.
- Inferno frozen variant:
  - `LevelUtils.kt:7` defines frozen = Glacier beaten and Inferno not beaten.
  - `FrozenBlock.kt:46-49` spawns only when frozen.
  - `RisingLavaRiver.kt:164` spawns only when not frozen.
  - `InfernoDeath.kt:21-24` repeats the same rule by hand.
  - So three entities each know the rule.
- `HeartTank.kt:153`: in DESERT_MAN it sends a light event.
- `LevelSelectScreen.kt:331`: moon pieces.
- Gauntlets: the same `LevelOverride` block is copied 3 times (`LevelDefinition.kt:131-188`).

(2) Options

A. A Kotlin manifest. `LevelDefinition` gets typed fields:
- `reward: List<WeaponId>`
- `environment: Set<Trait>` (`LOW_GRAVITY`, `VACUUM`, `DARK`)
- `variants: Map<String, Condition>`, for example `"frozen" to defeated(GLACIER) and !defeated(INFERNO)`
- `introDef`
- `selectPosition`
- `preset` (for example `EXTRA_HARD_FULL_LOADOUT`)
- `saveId`
- Entities ask `level.has(Trait.VACUUM)` or `level.variant("frozen")`.
- In TMX, an object property `variant=frozen` / `variant=!frozen` replaces the three `canSpawn` overrides with one generic check in the spawner.

B. A manifest as JSON/YAML next to the TMX, or as TMX map properties.
- Pro: a modder can add a level without Kotlin.
- Con: the conditions and intro animations need a small expression or data language.

C. Leave it as it is, and only replace the `== MOON_MAN` checks with traits.

What still needs code:
- Boss behaviour.
- New traits: what "vacuum" does to a weapon is still code, but it is written once.
- Intro animations, until animation definitions are data (item 39).
- The select-screen layout logic.

When it is not worth it:
- With 9 robot master levels, one developer and no plans for modders or new levels, the full manifest (option B) costs more than it saves.
- Most of the gain comes from the traits and the variant flag (option C plus the variants). Those remove the class-to-level coupling, and you need them anyway for co-op and testing (a test level can turn on `LOW_GRAVITY` without being MOON_MAN).

(3) Recommendation

- Do option A in a reduced form: traits, variants, reward, `saveId` and preset in `LevelDefinition`.
- Move the intro definition to the boss def, or to data after item 39.
- Postpone option B until there is a real need for levels made by others.

(4) Open questions for the owner

- Will more robot master levels or user levels be added?
- Should "moon" be a level trait or a room trait? The room is more flexible, for example a moon section inside another level.

(5) Backlog wording

Title: "Level manifest: traits, variants and rewards instead of level-identity checks"
- Add `environment` traits (low gravity, vacuum, dark) and `variants` conditions to `LevelDefinition`. Replace the 9 `MOON_MAN` checks and the Inferno frozen checks.
- TMX objects can carry `variant=<name>` / `variant=!<name>`, and the spawner filters them.
- Move the weapon reward, the select position and the gauntlet preset into the manifest. Move the boss intro definition to the boss def.
- A stable `saveId` for each level, separate from the enum name.
- Move the per-spawn moon scalars to level defaults (see item 35).

(6) Dependencies

Item 6 (save IDs), 20, 23 (weapon IDs), 35 (level-wide defaults), 39 (intro animations as data), 12.
````
