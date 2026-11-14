# Class entries for `ProtoBuster` and `ProtoStrike`

Step 6 of `060c`. Owner decision D3a: an explicit entry in each table, set and light map, and no fallback rule in the lookup. `ProtoBuster` gets the value of `Bullet`. `ProtoStrike` gets the value of the full `ChargedShot`.

Scope: each line in `core/src/main/kotlin` that names `Bullet::class`, `ChargedShot::class`, `is Bullet` or `is ChargedShot`. Not in scope: `Bullet.kt`, `ChargedShot.kt`, `ProtoBuster.kt`, `ProtoStrike.kt`, `MegaEntityFactory.kt`, `MegamanWeaponsHandler.kt`, `MegamanXBuster.kt`. `LampeonBullet::class` lines are a different class and are not in the list.

Line numbers are the line of the `Bullet` or `ChargedShot` entry after the edit. Paths are under `core/src/main/kotlin/com/megaman/maverick/game/`.

## Changed

| File:line | What the line is | Choice | Reason |
|---|---|---|---|
| `damage/StandardDamageNegotiator.kt:25` | `LARGE_DMG_NEGS` `Bullet` (3) | add `ProtoBuster` (3) | exact-class map |
| `damage/StandardDamageNegotiator.kt:31` | `LARGE_DMG_NEGS` `ChargedShot` (5 full) | add `ProtoStrike` (5) | exact-class map |
| `damage/StandardDamageNegotiator.kt:66` | `MEDIUM_DMG_NEGS` `Bullet` (5) | add `ProtoBuster` (5) | exact-class map |
| `damage/StandardDamageNegotiator.kt:72` | `MEDIUM_DMG_NEGS` `ChargedShot` (15 full) | add `ProtoStrike` (15) | exact-class map |
| `damage/StandardDamageNegotiator.kt:107` | `SMALL_DMG_NEGS` `Bullet` (15) | add `ProtoBuster` (15) | exact-class map |
| `damage/StandardDamageNegotiator.kt:113` | `SMALL_DMG_NEGS` `ChargedShot` (`MAX_HEALTH` full) | add `ProtoStrike` (`MAX_HEALTH`) | exact-class map |
| `entities/contracts/AbstractBoss.kt:65` | `BOSS_DMG_NEG` `Bullet` (1) | add `ProtoBuster` (1) | exact-class map |
| `entities/contracts/AbstractBoss.kt:70` | `BOSS_DMG_NEG` `ChargedShot` (2 full) | add `ProtoStrike` (2) | exact-class map |
| `entities/bosses/GutsTankFist.kt:100` | `when`: `is ChargedShot` 2 full, else 1 | add `is ProtoStrike -> 2` | else branch gave 1 |
| `entities/bosses/SigmaRat.kt:125` | only a full `ChargedShot` does damage (1) | add `\|\| damager is ProtoStrike` | full charged shot |
| `entities/bosses/D1000.kt:137` | `is ChargedShot`: full gives `FULL_CHARGE_DAMAGE` | add `is ProtoStrike -> FULL_CHARGE_DAMAGE` | full charged shot |
| `entities/bosses/D1000Core.kt:120` | `canBeDamagedBy`: full `ChargedShot` or sabre | add `\|\| damager is ProtoStrike` | full charged shot |
| `entities/bosses/BigAssMaverickRobot.kt:152` | override map `Bullet` (1) | add `ProtoBuster` (1) | else the standard table gives more |
| `entities/bosses/BigAssMaverickRobot.kt:154` | override map `ChargedShot` (3 full) | add `ProtoStrike` (3) | else the standard table gives more |
| `entities/bosses/BigAssMaverickRobot.kt:452` | stun on hit by a full `ChargedShot` | add `\|\| damager is ProtoStrike` | full charged shot |
| `entities/special/Darkness.kt:73` | light `Bullet` (standard) | add `ProtoBuster` (standard) | exact-class light map |
| `entities/special/Darkness.kt:75` | light `ChargedShot` (brighter) | add `ProtoStrike` (brighter) | exact-class light map |
| `entities/special/DarknessV2.kt:79` | light `Bullet` (standard) | add `ProtoBuster` (standard) | exact-class light map |
| `entities/special/DarknessV2.kt:81` | light `ChargedShot` (brightest full) | add `ProtoStrike` (brightest) | exact-class light map |
| `entities/special/DarknessV4.kt:91` | light `Bullet` (standard) | add `ProtoBuster` (standard) | exact-class light map |
| `entities/special/DarknessV4.kt:93` | light `ChargedShot` (brightest full) | add `ProtoStrike` (brightest) | exact-class light map |
| `entities/projectiles/GroundPebble.kt:68` | damage map `Bullet` (15) | add `ProtoBuster` (15) | exact-class map |
| `entities/projectiles/GroundPebble.kt:70` | damage map `ChargedShot` (`MAX_HEALTH`) | add `ProtoStrike` (`MAX_HEALTH`) | exact-class map |
| `entities/projectiles/Asteroid.kt:60` | `HIT_PROJS` set `Bullet` | add `ProtoBuster` | both explode on contact |
| `entities/projectiles/Asteroid.kt:67` | `HIT_PROJS` set `ChargedShot` | add `ProtoStrike` | both explode on contact |
| `entities/projectiles/Petal.kt:61` | damage map `Bullet` (10) | add `ProtoBuster` (10) | exact-class map |
| `entities/projectiles/Petal.kt:64` | damage map `ChargedShot` (`MAX_HEALTH`) | add `ProtoStrike` (`MAX_HEALTH`) | exact-class map |
| `entities/projectiles/CactusMissile.kt:68` | damage map `Bullet` (10) | add `ProtoBuster` (10) | exact-class map |
| `entities/projectiles/CactusMissile.kt:71` | damage map `ChargedShot` (`MAX_HEALTH`) | add `ProtoStrike` (`MAX_HEALTH`) | exact-class map |
| `entities/projectiles/Needle.kt:54` | damage map `Bullet` (15) | add `ProtoBuster` (15) | exact-class map |
| `entities/projectiles/Needle.kt:57` | damage map `ChargedShot` (`MAX_HEALTH`) | add `ProtoStrike` (`MAX_HEALTH`) | exact-class map |
| `entities/enemies/Cactus.kt:87` | damage map `Bullet` (10) | add `ProtoBuster` (10) | exact-class map |
| `entities/enemies/Cactus.kt:94` | damage map `ChargedShot` (`MAX_HEALTH`) | add `ProtoStrike` (`MAX_HEALTH`) | exact-class map |
| `entities/enemies/Junkroid.kt:114` | damage map `Bullet` (8) | add `ProtoBuster` (8) | exact-class map |
| `entities/enemies/Junkroid.kt:116` | damage map `ChargedShot` (30 full) | add `ProtoStrike` (30) | exact-class map |
| `entities/enemies/HoverGunner.kt:174` | damage map `Bullet` (18) | add `ProtoBuster` (18) | exact-class map |
| `entities/enemies/HoverGunner.kt:176` | damage map `ChargedShot` (30) | add `ProtoStrike` (30) | exact-class map |
| `entities/hazards/SmallIceCube.kt:72` | `INSTANT_DEATH_ENTITIES` `Bullet` | add `ProtoBuster` | exact-class set |
| `entities/hazards/SmallIceCube.kt:78` | `INSTANT_DEATH_ENTITIES` `ChargedShot` | add `ProtoStrike` | exact-class set |
| `entities/hazards/DeadlyLeaf.kt:61` | `FULL_DMG_NEGS` set `ChargedShot` | add `ProtoStrike` | exact-class set |
| `entities/blocks/BreakableIce.kt:86` | `hitByProjectile`: full `ChargedShot` breaks it | add `is ProtoStrike -> explodeAndDie()` | full charged shot |
| `entities/blocks/BreakableIce.kt:100` | shield receiver: same rule | add `is ProtoStrike -> explodeAndDie()` | full charged shot |
| `entities/blocks/WoodCrate.kt:88` | `DAMAGERS` `Bullet` (5) | add `ProtoBuster` (5) | exact-class map |
| `entities/blocks/WoodCrate.kt:90` | `DAMAGERS` `ChargedShot` (`MAX_HEALTH` full) | add `ProtoStrike` (`MAX_HEALTH`) | exact-class map |

## Left as is

| File:line | What the line is | Choice | Reason |
|---|---|---|---|
| `entities/bosses/D1000.kt:136` | `is Bullet, is ChargedShotExplosion -> 0` | leave | `is Bullet` matches `ProtoBuster`: 0, as Megaman's pellet |
| `entities/bosses/WilyFinalBoss.kt:171` | `if (damager is Bullet) return 1` | leave | `is Bullet` matches `ProtoBuster`: 1, as Megaman's pellet |
| `entities/blocks/BreakableIce.kt:84` | `is Bullet, ... -> hit()` | leave | matches `ProtoBuster` |
| `entities/blocks/BreakableIce.kt:98` | shield receiver `is Bullet, ... -> hit()` | leave | matches `ProtoBuster` |
| `entities/bosses/Bospider.kt:96` | `Bullet` entry in commented-out code | leave | dead code |
| `entities/bosses/Bospider.kt:100` | `ChargedShot` entry in commented-out code | leave | dead code |

## Not in scope (fetch or spawn of an enemy `Bullet`)

| File:line | What the line is | Choice | Reason |
|---|---|---|---|
| `entities/bosses/WilyFinalBoss.kt:1895` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/CanonHopper.kt:126` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/DuoBallCanon.kt:280` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/Screwie.kt:270` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/JetMet.kt:239` | fetch `Bullet` (commented out) | skip | enemy shot |
| `entities/enemies/TotemPolem.kt:129` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/HeliMet.kt:335` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/SniperJoe.kt:513` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/TankBot.kt:160` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/Met.kt:183` | fetch `Bullet` | skip | enemy shot |
| `entities/enemies/ToxicBarrelBot.kt:228` | fetch `Bullet` | skip | enemy shot |

## Counts

- Changed: 44 lines in 21 files (`StandardDamageNegotiator.kt` and 20 entity files).
- Left as is: 6 lines.
- Not in scope: 11 fetch lines.

## Tag-keyed sets (added after the review before the play test)

The step-6 scan looked for `::class` and `is` only. 2 sets name the player shots by tag:

| File | Set | Choice |
|---|---|---|
| `entities/bosses/TimberWoman.kt` | `DEFLECTABLE_PROJECTILES` (`Bullet.TAG`, `ChargedShot.TAG`) | added `ProtoBuster.TAG`, `ProtoStrike.TAG` |
| `entities/hazards/SpikeTeeth.kt` | `PROJECTILES` (`ChargedShot.TAG`) | added `ProtoStrike.TAG` |

`GravityCapsule.kt` names both tags only in commented-out code.

## Scope change (2026-11-12): half-charge entries

- `ProtoStrike` is removed, with all its entries.
- `ProtoBuster` (B4) and `PlayerSabreSlash` (B3) take the half `ChargedShot` value of each damage table:

| Table | `ProtoBuster` | `PlayerSabreSlash` |
|---|---|---|
| `StandardDamageNegotiator` (large / medium / small) | 3 / 10 / 15 | 3 / 10 / 15 (new) |
| `AbstractBoss.BOSS_DMG_NEG` | 1 | 1 (new) |
| `BigAssMaverickRobot` | 2 | 2 (new) |
| `WoodCrate` | 15 | 15 (new) |
| `GroundPebble`, `Petal`, `CactusMissile`, `Needle`, `Cactus` | `MAX_HEALTH` | `MAX_HEALTH` (new) |
| `Junkroid` | 15 | 23 (X's, kept) |
| `HoverGunner` | 30 | 30 (X's, kept) |

- Not changed: the light maps (`Darkness*`) and the sets (`Asteroid`, `SmallIceCube`, `TimberWoman`, `SpikeTeeth`)
  get no slash entry, because they are about projectiles. `WilyFinalBoss`, `D1000` and `BreakableIce` test
  `is Bullet`, so a `ProtoBuster` counts as a pellet there.

- Changed 2026-11-12 (owner, play test 1): `PlayerSabreSlash` takes the FULL `ChargedShot` value: standard 5 / 15 /
  `MAX_HEALTH`, `AbstractBoss` 2, `BigAssMaverickRobot` 3, `WoodCrate` `MAX_HEALTH`; the others were already
  `MAX_HEALTH`; `Junkroid` and `HoverGunner` keep X's values.
