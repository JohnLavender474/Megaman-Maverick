# Notes: 092 Remove old entity versions and the version suffixes

Research for this item. All counts are from 2026-10-31, on `master` at `98898fb09`, unless a line names `084e` (worktree at `44660dced`).

Method:

- "Live map" means a file that `LevelDefinition` names (23 files). The object counts read the `name` of each TMX object, on all object layers. The folder
  `assets/tiled_maps/tmx/` holds 92 files.
- "Refs" are lines and files in `core/src` that hold the class name as a whole word, including the class's own file, imports, comments and tests.
- The class search covered file names and class declarations with `V<number>`, `_v<number>`, `Old`, `_OLD`, `Legacy`, `New`, `Copy`, `Backup`, and
  numbers. `engine/` and `lwjgl3/` have no match.

## Research table

| Class | Lines | Live map objects | Objects in old maps | Refs (lines, files) | Status |
|---|---:|---|---:|---|---|
| `Cart` | 191 | 2 (`Test1.tmx`) | 13 | 8, 3 | Old. No play effect. |
| `CartV2` | 225 | 3 (`DesertMan_16x14_v4.tmx`) | 2 | 9, 4 | In use. |
| `TubeBeamer` | 142 | 0 | 38 | 4, 2 | Old. Dead. |
| `TubeBeamerV2` | 402 | 22 (`ReactorMan_v3.tmx` 21, `Test1.tmx` 1) | 22 | 5, 2 | In use. `ReactorMan` also spawns it. |
| `TubeBeam` | not counted | 0 | not counted | 6, 3 | Only `TubeBeamer` fetches it. |
| `Darkness` | 389 | 1 (`Test1.tmx`) | 5 | 4, 2 | Old. No use in a real stage. |
| `DarknessV2` | 414 | 2 (`DesertMan_16x14_v4.tmx`, `WilyStage1_v2.tmx`) | 4 | 15, 7 | In use. |
| `DarknessV4` | 751 | 0 | 0 | 2, 1 | Dead. Differs from `DarknessV2`. |
| `MegamanTrailSprite` | 112 | 0 | 0 | 4, 3 | Commented out in full. |
| `MegamanTrailSpriteV2` | 140 | 0 | 0 | 7, 4 | In use. `EffectComponents.kt:85` fetches it. |
| `SpikeBot_OLD` | 336 | 0 | 0 | 2, 2 | Old. Dead. `SpikeBot` has 13 live objects. |
| `SniperJoe_OLD` | 574 | 0 | 0 | 4, 3 (see below) | Old. No live path. `SniperJoe` has 68 live objects. |
| `ReactorMan_OLD` | 384 | 0 | 0 | 2, 2 | Commented out in full. |
| `MechaDragon_OLD` | 551 | 0 | 0 | 2, 2 | Commented out in full. |

No entity class has an old version that a real stage uses. The list of 7 `*V[0-9]*.kt` files on `core/src/main/kotlin` is `CartV2`, `DarknessV2`, `DarknessV4`,
`TubeBeamerV2`, `MegamanTrailSpriteV2`, `LevelSelectScreenV2` and `RobotMasterIntroScreenV2`. `DarknessV3` was deleted by `064b`. The 4 `_OLD` files do not
match that pattern.

## Sorted

**Safe to include:** the old version has no live use, or only a use in `Test1.tmx`.

- `Cart` / `CartV2`.
- `TubeBeamer` / `TubeBeamerV2`, with `TubeBeam`.
- `Darkness` / `DarknessV2`.
- `MegamanTrailSprite` / `MegamanTrailSpriteV2`.
- `SpikeBot_OLD`.
- `SniperJoe_OLD`.

**Needs an owner decision:**

- `DarknessV4`: dead, but it differs from `DarknessV2`, and the `039` answer names experimentation.
- `ReactorMan_OLD`, `MechaDragon_OLD`: `029c` commented them out and did not delete them, on purpose.

**Out of scope:**

- `LevelSelectScreen`, `LevelSelectScreenV2`, `RobotMasterIntroScreen`, `RobotMasterIntroScreenV2`, `MugshotV2`: screens, not entities (see below).
- `Level.kt`: a 66-line enum with no reference outside its file. It is not an entity. Add it to `039`.
- `SphinxMiniBoss`: commented out by `029c`, but it has no version.
- `SMB3_GoldBreakableBlock`: the number is in the name of the game.
- `CartinJoe`: a separate enemy. Its file names neither cart, and it has 6 live objects.
- The Megaman `cartin` animations: 9 keys in `MegamanAnimationDefs.kt:22-30`. A search for `cart` finds no code that builds these keys. They do not depend on
  either cart class, so `039` can review them.
- Atlases such as `MegamanTrailSprite.txt`, and old map versions: `039`.
- Other entities with no version in the name and no live use: `039`. This item did not review them.

## `Cart` and `CartV2`

The two are different entities, not an old and a new form of the same behavior:

- `CartV2` extends `Block`. Its body is `STATIC` and solid (`Block.kt:256`). It moves in `shouldMove()`, when the feet of a player overlap its body. It does
  not use `FeetCartRule`. It kills an `AbstractEnemy` that its side fixtures hit while it moves.
- `Cart` has a `DYNAMIC` body with no `BLOCK` fixture, so Mega passes through it. Its `CART` fixture is what `FeetCartRule` reads. The rule sets `owner`,
  `BodySense.TOUCHING_CART` and the `ConstKeys.CART` property, and no code reads these values.

Live objects:

- `DesertMan_16x14_v4.tmx`, layer `specials`: ids 305, 420, 421, each with `facing=right`. `room9` has 1 and `room10` has 2.
- `Test1.tmx`, layer `specials`: `Cart` ids 73 (`room_8`) and 256 (`room16`), with no properties.
- `RefactorTest084.tmx` on `084e`: `CartV2` id 53, room `cart`, `facing=right`.

Old maps name `Cart` 13 times in 6 files, and `CartV2` 2 times in `DesertMan_16x14_v3.tmx`. These files do not load. After the rename, a `Cart` object in an old map
gets the solid cart, and it throws if it has no `facing`.

### Code that goes with the old `Cart`

| Target | Place on `master` |
|---|---|
| `FeetCartRule` | `world/contacts/rules/FeetCartRule.kt` |
| `FixtureType.CART` | `world/body/FixtureType.kt:17`; old `Cart.kt:116` is the only fixture |
| `BodySense.TOUCHING_CART` | `world/body/BodySense.kt:21`; only `FeetCartRule` uses it |
| `ConstKeys.CART` | `Constants.kt:340`; only `FeetCartRule` uses it |
| `ContactRuleIds.FEET_CART` | `world/contacts/ContactRuleIds.kt:35` |
| Rule table entry | `world/contacts/MegaContactRules.kt:76` |
| Filter entry and check | `world/contacts/MegaContactFilter.kt:65` and `:114-116` |
| Reference file | `core/src/test/resources/contact-rule-selection.txt`: 171 of 2523 lines name `CART` (57 for each of 3 phases) |
| Factory | `entities/factories/impl/SpecialsFactory.kt:18,45`; no code reads `SpecialsFactory.CART` |

`.ordinal` in `world/` on `core` and `engine` is used only for the contact phase, so a removed `FixtureType` value moves no phase. `ContactRuleSelectionTest` reads
`FixtureType.entries`. The branch must still check that the order of the other contacts does not change.

The old `Cart` also has a `CONSUMER` rule in the reference file, as every fixture type does. This is not a special case.

### Uses of `CartV2` outside its file

- `AbstractItem.kt:182`: the item `block` filter from `fix/cart-pushes-dropped-items`. It means `CartV2`, and it stays with the renamed class.
- `AbstractEnemy.kt:189`: `canSpawnItemOnHealthDepleted() = lastDamager != CartV2`. It means `CartV2`, but see the next section.
- `CartV2ItemTest`: 3 lines. The test file changes its name too.

### `AbstractEnemy.kt:189` is always true

`CartV2` in this expression is the companion object, not the class. `lastDamager` is an `IDamagerEntity?`, set at `AbstractHealthEntity.kt:96`. `CartV2` is
not an `IDamagerEntity`, and its receivers call `depleteHealth()`, which does not write `lastDamager`. So an enemy that a cart kills can drop an item. The
likely intent is that it does not. The item keeps the current behavior, because gameplay is the contract. See open question 7.

### Atlas

`assets/sprites/sprite_sheets/Specials1.txt` holds these regions:

| Region | Position | Size |
|---|---|---|
| `Cart` | 32, 97 | 48x48 |
| `Cart/idle` | 80, 97 | 48x48 |
| `Cart/move` | 0, 401 | 48x96 |
| `CartV2/idle` | 128, 97 | 48x48 |
| `CartV2/move` | 48, 401 | 48x96 |

The name is a clash, and the art is different:

- `Cart/idle` and `CartV2/idle` differ in 60 of 2304 pixels. The wheels are not the same.
- `Cart/move` and `CartV2/move` differ in 12 of 4608 pixels.
- `Cart` and `Cart/idle` have the same pixels.

`CartV2.kt:78` loads `$TAG/$key`. With `TAG = "Cart"` and no atlas change, the game shows the `Cart/idle` art and logs no error. The old class reads only the
region `Cart`. The frames are `assets/sprites/frames/Specials1/Cart.png`, `Cart/` and `CartV2/`, and `assets/sprites/texture_packer/Specials1.tps:242-274`
lists them.

### `Test1.tmx`

`CartV2.kt:103` reads `spawnProps.get(ConstKeys.FACING, String::class)!!`. The 2 `Cart` objects have no properties, so after the rename each one throws on
spawn. The old `Cart` defaults to `right`. Open question 5 gives the fix. `084e` `SCOPE.md` says that no `Test1.tmx` room with a cart has a player spawn.

### Overlap with `084e`

`git diff master...mega-refactor-084e-class-line` touches these files of this item:

- `CartV2.kt`, `FeetCartRule.kt`, `MegaContactFilter.kt`, `ICartRider.kt` (deleted).
- `MegamanTrailSpriteV2.kt`, `EffectComponents.kt`.
- `RefactorTest084.tmx`, `MegaContactFilterTest.kt`, `FeetCartRuleTest.kt` (new).

So this item starts after `084e` merges. `mega-refactor-031e-bindings`, `mega-refactor-051-master` and `mega-refactor-051-fix-scroll-room-scope` touch none of these
files.

## `TubeBeamer` and `TubeBeamerV2`

- The two differ. `TubeBeamer` shoots `TubeBeam` projectiles from an invisible body. `TubeBeamerV2` draws a line beam, with `ILaserEntity`, `IHazard` and
  obstacles. Only `TubeBeamerV2` has live objects, so the difference has no play effect.
- `TAG` is not an atlas prefix on either class. `TubeBeamerV2` loads `TubeBeam_short` by its own key, so the atlas does not change.
- Live change: 21 objects in `ReactorMan_v3.tmx`, 1 in `Test1.tmx`, and `ReactorMan.kt:210,678`.
- Old maps hold `TubeBeamer` 38 times in 4 files, and `TubeBeamerV2` 22 times in `ReactorMan_v2.tmx`.
- Deletions: `HazardsFactory.kt:23,55`, and `TubeBeam.kt` with `ProjectilesFactory.kt:32,85`. `TubeBeamExplosion` stays, because `TubeBeamerV2` uses it.

## `Darkness`, `DarknessV2`, `DarknessV4`

- `Darkness` has 1 object: `Test1.tmx` id 123, with `key=1`, `room=room10`, `hidden=true`. No class reads `hidden`. After the rename, this object spawns the former
  `DarknessV2`, which reads the same `key`, `room` and bounds. The behavior in that test room changes; no real stage changes. The map does not change.
- `TAG` is not an atlas prefix: all 3 use the `black` region of `TextureAsset.COLORS`.
- Refs to rename: `AbstractEnergyItem.kt`, `DarkSerketClaw.kt`, `Laser.kt` (each uses `DarknessV2.TAG` and a cast), `DarknessGrid.kt` and `DarknessRoomRules.kt`
  (KDoc), and `SpecialsFactory.kt:49`.
- `SpecialsFactory.DARKNESS` has the key `"Darkness"` but makes a `DarknessV2`, with the old call in a comment. No code reads the key.
- Live map change: 1 object in `DesertMan_16x14_v4.tmx`, 1 in `WilyStage1_v2.tmx`.
- `DarknessV4` does not use `DarknessGrid`. `DarknessGrid` and `DarknessRoomRules` came from `DarknessV2` on 2026-08-31, with the fixes `53105c0c2` and `5702f5515`, and
  `DarknessV4` has no such change. A `diff` of the two files, with the class name made equal, gives 740 lines.

## `MegamanTrailSprite` and `MegamanTrailSpriteV2`

- `MegamanTrailSprite.kt` is commented out in full (from `029f`). Its top `TODO` says that it may be out of date. `039` already names it as a known candidate.
- `TAG` of the class in use is `MegamanTrailingSprite_v2`, and it is used only in logs and `getTag()`. The atlas does not use it as a prefix.
- Refs to rename: `EffectComponents.kt:14,85`, `DecorationsFactory.kt:8,69`. The const `MEGAMAN_TRAIL_SPRITE` and the commented pool at `DecorationsFactory.kt:33,68` go.
  No code reads the const `MEGAMAN_TRAIL_SPRITE_V2` (`:34`).
- `TextureAsset.MEGAMAN_TRAIL_SPRITE` has only a commented reader, and `TextureAsset.MEGAMAN_TRAIL_SPRITE_V2` stays as it is. The atlas files are a matter for `039`.

## `SpikeBot_OLD` and `SniperJoe_OLD`

Each has `TAG = "SpikeBot"` or `"SniperJoe"`, the same as the plain class. So the deprecated pool keys and the `block_filters` values in the gauntlet maps (for
example `Met,SniperJoe`) do not depend on which class exists.

- `SpikeBot_OLD`: the only other ref is the pool at `EnemiesFactory.kt:161`. No code reads `EnemiesFactory.SPIKE_BOT`. That pool line then uses `SpikeBot` or goes.
- `SniperJoe_OLD`: not entirely unreferenced. `EnemiesFactory.kt:111` pools it under `SNIPER_JOE`, and `SwingingPlatform.kt:195-196` fetches that key when the map
  property `ConstKeys.ENEMY_SPAWN` is `"SniperJoe"`. The live maps have 2 `SwingingPlatform` objects, both in `Test1.tmx`, and neither has that property. So the path
  does not run. `SniperJoe.onSpawn` accepts `ConstKeys.POSITION`, so the pool and `SwingingPlatform.kt:37,195` can use `SniperJoe`. A `diff` of the two files, with the
  class name made equal, gives 696 lines, so the two act differently.

## `ReactorMan_OLD` and `MechaDragon_OLD`

`029c` commented out both in full, with a `TODO` at the top. Its `SCOPE.md` lists "commented out in full (not deleted)". The only other ref of each is a commented line
in `BossesFactory.kt` (`:28`, `:40`).

## Screens (out of scope)

| Class | Lines | State |
|---|---:|---|
| `LevelSelectScreen` | 485 | Dead. Only its own file names it. |
| `LevelSelectScreenV2` | 339 | Live: `MegamanMaverickGame.kt:462`. It reads the atlas `LEVEL_SELECT_SCREEN_V3`. |
| `RobotMasterIntroScreen` | 641 | Live: `MegamanMaverickGame.kt:464`. |
| `RobotMasterIntroScreenV2` | 645 | Dead. Only a comment at `MegamanMaverickGame.kt:463` names it. A `diff` gives 402 lines. |

The older `RobotMasterIntroScreen` is the live one, so the rule "keep the highest version" does not apply to screens.

## Existing items that overlap

- `039` (retire unused assets, review dead code): the closest overlap. It names `MegamanTrailSprite` as a candidate, and the owner's answer asks for a review of dead
  code first. This item does that review for the versioned entities only. `039` keeps the assets, old maps, screens and unversioned dead code.
- `084` (`084e`): see "Overlap with `084e`".
- `014`: the issue `2026-10-22-unused-contact-hooks.md` names `TOUCHING_CART` and `ConstKeys.CART`.
- `003` (level content validator): it can later check that a live map object names a class.
