# Scope: mega-refactor-139a-shared-enemy-shot

What this branch changes and why. Write this file after the scan, with `CONTRACT.md` and before
`BRIEF.src.html`. This is Claude's record. The owner approves the brief, not this file. If the scope
changes later, update this file and the brief, and ask the owner again.

- Status: approved
- Approved: 2026-11-13 (owner, at the brief)

Scan of `master` at `add9d66d2`, 2026-11-13.

## Goal

Make one shared simple enemy shot class. The art and the values of each shot are data in a small typed kind, not a
class of their own. `JunkShot` becomes this class. Then move the closest one-off shots into it. Each moved shot looks,
moves and does damage the same as before.

## Item

`139` (`ITEMS.md`). From `ITEM.md`:

> Many enemies have their own one-off shot class in `entities/projectiles/`, for example `JunkShot` (`081e`, 141
> lines). Most of these shots only fly in a line, damage a player and go away, with their own art and values. One
> shared simple enemy shot, with the art and the values as data, removes the copies, so that a new enemy needs no new
> shot class.
>
> - List the one-off enemy shots and what each one does that a straight shot does not do.
> - One shared simple shot class that takes the differences as spawn data or as a small typed def. `JunkShot` is the
>   first caller.
> - Move the other simple shots to it one at a time. Each shot looks, moves and does damage the same as before.
>
> Decision (owner, 2026-11-13): each branch squash-merges directly into `master`.
>
> Open question: which shots go in the first branch, after `JunkShot`?

This branch takes all of it, for `JunkShot` and the first set of moved shots (decision `D3`). Later branches move
more shots.

## Current state

### `JunkShot` is already a shared shot for 3 enemies

`JunkShot.kt` (141 lines) has an `enum class Type` (`JunkShot.kt:52-67`) with 4 entries. Each entry gives the region
key, the body size, the damage, `shootable`, a `TickFrameDef` and the gravity:

| `Type` | Spawner | Body (tiles) | Damage | Shootable | Gravity |
|---|---|---|---|---|---|
| `CORE` | `D1000Core.kt:528` | 0.5 | 2 | yes | 0 |
| `JUNKROID` | `Junkroid.kt:183-187` (normal mode) | 0.3125 | 1 | yes | `Junkroid.HURL_GRAVITY` (-20) |
| `JUNKROID_HARD` | `Junkroid.kt:183-187` (hard mode) | 0.3125 | 1 | no | `Junkroid.HURL_GRAVITY` |
| `HOVER_GUNNER` | `HoverGunner.kt:326-330` | 0.3125 | 3 | yes | 0 |

- Spawn data: `ConstKeys.TYPE` (the `Type`), `POSITION`, `TRAJECTORY`, `OWNER`.
- `init()` loads all 4 animations from `ENEMIES_2` with `TickFrames.animation` (`JunkShot.kt:83`), so the smoke test
  checks every region.
- Fixtures: `PROJECTILE`, `DAMAGER`, `DAMAGEABLE`. `onSpawn` sets the body and fixture size per type (`:92-93`).
- A player's shot destroys a shootable type (`canBeDamagedBy`, `:99-100`).
- It dies on a block and on a hit, and spawns a `Disintegration` (`:113-119`). No sound.
- It does not die on a shield (no `hitShield`).
- The sprite is placed with `TickFrames.place`, flipped by the sign of the velocity (`:135-138`).
- The `TODO` at `JunkShot.kt:48` points to this item.

So the shared class and its typed kind already exist in a small form. The branch makes the kind general enough for the
Maverick-style shots, gives the class a general name, and moves shots into it.

### Damage is keyed by the class or the tag

| Lookup | Key | Where |
|---|---|---|
| Player takes damage | tag; `JunkShot.TAG pairTo dmgNeg { (it as JunkShot).type.damage }` | `PlayerDamageNegotiator.kt:43` |
| Player takes damage, no tag entry | `ISizable.size`: `LARGE` 4, `MEDIUM` 3, `SMALL` 2 | `PlayerDamageNegotiator.kt:63-67` |
| Enemy takes damage | `KClass` tables | `StandardDamageNegotiator.kt:25-99` |
| Enemy takes damage, extra | `damageOverrides` per enemy, `KClass` | for example `Met.kt:124`, `HeliMet.kt:119` (`PurpleBlast`) |

- `PlayerDamageNegotiator` is the one table for all players (`MegaPlayer.kt:67`), so Megaman, X and Proto Man get the
  same values.
- `AbstractProjectile` defaults to `Size.MEDIUM`. So each one-off enemy shot with no tag entry does 3 damage to a
  player. `JunkShot` is `Size.SMALL`, but its tag entry wins.
- An enemy shot does no damage to an enemy unless its class is in a `KClass` table, because
  `AbstractHealthEntity.canBeDamagedBy` (`:76-78`) needs a non-zero negotiation.
- Tags are fixed at `init()` (`TagsComponent`; a change at runtime is item `149`). So a pooled shared class cannot
  have a tag per kind. The damage per kind must come from the kind, through one tag entry.

### Other lookups keyed by the shot class or tag

| Shot | Lookup | Where |
|---|---|---|
| `JunkShot` | `getOfTag(JunkShot.TAG)`, `shot !is JunkShot`, `type == CORE` | `D1000TrapGuard.kt:31,88-97` |
| `JunkShot` | tag list | `entity-tags.txt:365` |
| `JunkShot` | 38 lines in 11 test files (`D1000*Test`, `D1000Rig`, `X6IntroEnemiesTest`) | `core/src/test/` |
| `BunbyRedRocket`, `RocketBomb`, `SharpStar` | `Asteroid.HIT_PROJS` | `Asteroid.kt:60-70` |
| `SharpStar` | `isAny(...)` | `MoonScythe.kt:105` |
| `TeardropBlast` | `is TeardropBlast -> hit()` | `BreakableIce.kt:84,97` |
| `FireWall` | ice cube list | `SmallIceCube.kt:86` |
| `PurpleBlast` | `damageOverrides` | `Met.kt:124`, `HeliMet.kt:119` |
| `LampeonBullet`, `GreenPelletBlast` | `KClass` damage tables | `StandardDamageNegotiator.kt:46,55,85,94` |
| `DeathBomb` | `damager.owner is DeathBomb` | `LampeonBandito.kt:161` |

- No contact rule names a shot class. `MegaContactRules.kt:62-69` keys on fixture types only.
- `known-entity-failures.txt` lists only `projectiles.SigmaRatElectricBall` from this folder.
- `RollingBotShot`, `FirePellet` and `SubmarineMissile` have no lookup by class or tag, no test, and one spawner each.
  Their only lines outside their own file are the spawner, the `ProjectilesFactory` pool (`ProjectilesFactory.kt:29,82`
  and `:49,102`; none for `SubmarineMissile`) and `entity-tags.txt:358,390,402`. No code fetches these factory keys.

### Classification of `entities/projectiles/`

73 files, 12684 lines. `S` = straight line at constant velocity, damages a player, dies on a block, a hit or off
screen, maybe an animation. `L` = the same, plus a constant gravity (a lob). `M` = more than that. "Read" means the
whole file was read; the other rows come from `grep` signals (gravity, timers, hit callbacks, child spawns, lookups).

| Class | Lines | Kind | What it does more than a straight shot | Read |
|---|---|---|---|---|
| `JunkShot` | 141 | S, L | 4 types; lob for Junkroid; shootable | yes |
| `RollingBotShot` | 128 | S | spawn data `LEFT` (fixed speed 8); extra `BODY` fixture; dies on shield; burst + sound | yes |
| `FirePellet` | 91 | S | does not die on a hit; dies on shield; sprite turns with velocity; no animation; no owner | yes |
| `SubmarineMissile` | 107 | S | `SHIELD` fixture (player shots bounce off); 2x0.5 body; explosion + sound | yes |
| `TeardropBlast` | 109 | S | circle fixtures; `BreakableIce` class check; spawner `JetpackBlaster` not in a live map | yes |
| `BunbyRedRocket` | 144 | S | stops while the camera turns; dies on a player shot and on an explosion; `Asteroid` list | yes |
| `MagmaOrb` | 93 | S | never dies on a block or hit; stops while the camera turns; boss attack (`InfernoMan`) | yes |
| `ElectricBall` | 127 | S | 2 sizes; no live spawner (`Elecn` is in no live map); `explodeAndDie` empty | yes |
| `LampeonBullet` | 173 | S | in `KClass` damage tables (player can use it) | no |
| `GreenPelletBlast` | 158 | S | burst timer; in `KClass` damage tables | no |
| `SuperCoolActionStarWarsSpaceLazer` | 138 | M | burst state with its own animation | yes |
| `PurpleBlast` | 133 | M | charge then blast animation; `damageOverrides` in `Met`, `HeliMet` | yes |
| `TubeBeam` | 140 | M | cull timer; room transition listener; size by direction | yes |
| `RocketBomb` | 132 | M | aims at the player at spawn; `hitBody`; `Asteroid` list | yes |
| `SharpStar` | 126 | M | spins; dies on `PreciousGem`; class lists | yes |
| `Petal` | 159 | M | has health; own damage table | yes |
| `JoeBall` | 157 | M | reflects off shields; dynamic body | yes |
| `ElecBall` | 173 | M | hit timer; light source; boss | no |
| `SigmaRatElectricBall` | 166 | M | hit timer; known smoke failure | no |
| `SniperJoeShield` | 181 | M | rotation timer; shield | no |
| `FireWall` | 150 | M | disintegrate timer; `SmallIceCube` list | no |
| `DeathBomb` | 92 | L | `hitSand`; `LampeonBandito` owner check | no |
| `Nutt`, `Cokonut`, `Picket`, `KibboHammer` | 81-108 | L | lob with own gravity constant | no |
| `UFOBomb`, `ToxicGoopShot`, `Rock`, `ExplodingBall` | 113-138 | L | lob; own death effect (explosion, splash) | no |
| `FireMetFlame`, `CacaFlame`, `PipiEgg`, `MagmaPellet` | 120-168 | L, M | lob plus a timer, an aim or a child spawn | no |
| `CaveRock`, `DuoBall`, `BouncingPebble`, `ReactorMonkeyBall`, `Snowhead`, `MagmaGoop` | 115-228 | M | bounce | no |
| `CactusMissile`, `HomingMissile` | 247-272 | M | homing, health | no |
| `Snowball`, `IceBomb`, `ArigockBall`, `FallingIcicle`, `SpitFireball` | 115-244 | M | many hit rules, freeze or child spawns | no |
| `Bullet`, `ChargedShot`, `Fireball`, `MagmaWave`, `MoonScythe`, `Needle`, `Axe`, `SlashWave`, `Asteroid`, `PreciousGem`, `PreciousShard`, `ReactorManProjectile` | 126-423 | M | player weapons or shared boss shots; in damage tables | no |
| other boss and special shots (`D1000Bomb`, `DarkSerketClaw`, `BigAssMaverickRobotOrb`, `BoulderProjectile`, `GroundPebble`, `MagmaMeteor`, `MockingByteEgg`, `PreciousGemBomb`, `PreciousGemCluster`, `SealionBall`, `SmallGreenMissile`, `SphinxBall`, `SpiderWeb`, `WilyPlaneBomb`) | 92-279 | M | timers, child spawns, health, special contact | no |

Result: 10 classes are `S` by their flight. Only 3 of them (`RollingBotShot`, `FirePellet`, `SubmarineMissile`) have
no lookup by class or tag and a spawner in a live map. About 12 more are `L` and fit later, because the kind already
has gravity.

### The 3 closest shots, side by side

| | `JunkShot` (now) | `RollingBotShot` | `FirePellet` | `SubmarineMissile` |
|---|---|---|---|---|
| Spawner | 3, see above | `RollingBot.kt:180-189` | `DemonMet.kt:290-295` | `SubmarineJoe.kt:310-319` |
| Velocity data | `TRAJECTORY` | `LEFT` + `X_VEL` 8 | `IMPULSE` | `IMPULSE` |
| Atlas, region | `ENEMIES_2`, `JunkShot/<key>` | `PROJECTILES_1`, `RollingBotShot` | `PROJECTILES_1`, `FirePellet` | `PROJECTILES_1`, `SubmarineMissile` |
| Art | `TickFrames` | 1x2 grid, 0.1 s, loop | one region, no animation | 2x2 grid, 0.1 s, loop |
| Sprite size (tiles) | from `TickFrameDef` | 1x1 | 1x1 | 2x0.5 |
| Sprite turn | flip if moving right | flip if facing left | rotation = velocity angle - 90 | flip if facing right |
| Draw priority | `FOREGROUND` 0 | `PLAYGROUND` 1 | `FOREGROUND` 0 | `GameSprite()` default |
| Body (tiles) | square, per type | 0.85x0.45 | 0.5 | 2x0.5 |
| Fixtures | `PROJECTILE`, `DAMAGER`, `DAMAGEABLE` | `BODY`, `PROJECTILE`, `DAMAGER` | `PROJECTILE`, `DAMAGER` | `PROJECTILE`, `SHIELD`, `DAMAGER` |
| Dies on block | yes | yes | yes | yes |
| Dies on a hit | yes | yes | no | yes |
| Dies on shield | no | yes | yes | no |
| Death effect | `Disintegration` | `ChargedShotExplosion` (0.1 s, rotated, owner) | `Disintegration` | `Explosion` (owner) |
| Death sound (on camera) | none | `ENEMY_DAMAGE_SOUND` | `ENEMY_DAMAGE_SOUND` | `EXPLOSION_2_SOUND` |
| Damage to player | 2, 1, 1, 3 (tag entry) | 3 (`MEDIUM`) | 3 (`MEDIUM`) | 3 (`MEDIUM`) |
| Owner | spawner | spawner | none | spawner |
| Live room with spawn | `X6_INTRO` `room1`, `room2` | `REACTOR_MAN` `room1` | `INFERNO_MAN` `room4` | `WILY_STAGE_2` `path1_room1` (no spawn in it) |

Other facts:

- None of the 4 implements `IFreezableEntity`. None reflects or deflects off a shield. `JunkShot` and
  `RollingBotShot` and `SubmarineMissile` ignore `canMove` (the camera turn).
- All 4 use the `AbstractProjectile` culling (out of camera, cull events) with the default cull time.
- `RollingBotShot.onDamageInflictedTo` does not call `super`, so it ignores the `ON_DAMAGE_INFLICTED_TO` spawn
  callback. No spawner passes that callback, so a shared `super` call changes nothing.

## In scope

- Rename `JunkShot` to a general name (decision `D1`) and make its kind the typed def for all moved shots.
- Grow the kind only by the fields that the moved shots need (each field has a caller in this branch):
  - art: `TickFrames` def or a plain grid animation (rows, columns, frame time) with a sprite size;
  - atlas and region key per kind, so no art moves;
  - body width and height (not only a square);
  - extra fixtures per kind (`BODY`, `SHIELD`, `DAMAGEABLE`), made once in `init()` and set active per spawn;
  - dies on a hit, dies on a shield;
  - sprite turn: flip by velocity, flip by velocity with the art facing the other way, or rotate with velocity;
  - draw priority;
  - death effect and death sound.
- Move the first-branch shots (decision `D3`). Change their spawners to pass `ConstKeys.TYPE` and `TRAJECTORY` (for
  `RollingBot`, the caller makes the trajectory from its facing and the old speed of 8 tiles per second).
- One damage entry in `PlayerDamageNegotiator` that reads the kind's damage (decision `D2`). Each moved kind keeps its
  old value.
- Delete the moved classes, their `ProjectilesFactory` entries and their `entity-tags.txt` lines; add the line of the
  shared class.
- Rename the `JunkShot` uses in `D1000TrapGuard`, `D1000Core`, `Junkroid`, `HoverGunner` and the 11 test files.
- Remove the `TODO` at `JunkShot.kt:48`.
- New tests (see `CONTRACT.md`).

## Out of scope

- Shots with a lookup by class or tag (`BunbyRedRocket`, `TeardropBlast`, `LampeonBullet`, `GreenPelletBlast`,
  `PurpleBlast`, `SharpStar`, `RocketBomb`). A move must first change the lookup. Later branch.
- `MagmaOrb` (never dies on a hit, boss attack), `ElectricBall` (no live spawner), and all `L` and `M` shots. Later
  branches.
- Damage profiles by damage tag (item `023`).
- Session-owned factories (item `096`). The shared shot still uses `MegaEntityFactory`.
- Moving the art regions or renaming atlas keys.
- `TickFrames` durations in seconds (item `140`).

## New API and first caller

| New API | First caller in this branch |
|---|---|
| Shared shot class (renamed `JunkShot`) | `D1000Core.shoot`, `Junkroid.shoot`, `HoverGunner.shoot` (already), `RollingBot`, `DemonMet.fire` |
| Kind entries `ROLLING_BOT`, `FIRE_PELLET` (names in the plan) | `RollingBot.kt:180`, `DemonMet.kt:290` |
| Grid art field of the kind | `ROLLING_BOT`, `FIRE_PELLET` |
| Body width and height fields | `ROLLING_BOT` (0.85x0.45) |
| Extra fixture field | `ROLLING_BOT` (`BODY`), `CORE`/`JUNKROID`/`HOVER_GUNNER` (`DAMAGEABLE`) |
| `diesOnHit`, `diesOnShield` | `FIRE_PELLET` (no, yes), `ROLLING_BOT` (yes, yes), junk kinds (yes, no) |
| Sprite turn field | junk kinds (flip by velocity), `ROLLING_BOT` (flip), `FIRE_PELLET` (rotate) |
| Draw priority field | `ROLLING_BOT` (`PLAYGROUND` 1), others (`FOREGROUND` 0) |
| Death effect and sound fields | `ROLLING_BOT` (`ChargedShotExplosion`, sound), `FIRE_PELLET` (`Disintegration`, sound), junk kinds (`Disintegration`, no sound) |

With `D3` option B, `SUBMARINE_MISSILE` adds the `SHIELD` fixture value and the `Explosion` death. No field is added
for a later shot.

## Affected areas and risks

Files (option A of `D3`): `JunkShot.kt` (renamed), `RollingBotShot.kt` and `FirePellet.kt` (deleted), `RollingBot.kt`,
`DemonMet.kt`, `D1000Core.kt`, `D1000TrapGuard.kt`, `Junkroid.kt`, `HoverGunner.kt`, `PlayerDamageNegotiator.kt`,
`ProjectilesFactory.kt`, `entity-tags.txt`, 11 test files, 1 new test file.

- The damage of a moved shot goes from the `ISizable` path to the tag entry. A wrong number in a kind changes the
  damage. A test pins each kind's damage.
- A pooled shot keeps fixtures and sprite state from its last kind. `onSpawn` must set every per-kind value (fixture
  active flags, body size, sprite size, draw priority).
- The shared class is an `IDamageableEntity` (from `JunkShot`); `RollingBotShot` is not. A player `Bullet` or
  `ChargedShot` explodes on a `BODY` fixture of an `IDamageableEntity` that refuses its damage (`Bullet.kt:142`,
  `ChargedShot.kt:144-149`). So the `BODY` fixture of the Rolling Bot kind would stop player shots, which pass
  through today. The plan must keep the old result, for example: drop the `BODY` fixture for this kind if nothing
  else reads it, or make the damageable part a kind field. A test pins it (`CONTRACT.md`).
- A rename of the shot class changes the `D1000TrapGuard` scan: it now sees every shared shot, not only junk shots.
  It already filters by owner, so the result does not change.
- Text overlap with `060c` and `137a` in `Junkroid.kt`, `HoverGunner.kt`, `D1000Core.kt` and `entity-tags.txt`. See
  `NOTES.md`.

## Overlap with other branches

See `NOTES.md`. Short form: `060c` changes imports and damage tables in 3 files that this branch also renames in;
`137a` will change the `DebrisPiece` spawns in `Junkroid` and `HoverGunner`; `138a` has no overlap. All are text
overlaps, no design overlap. `137a` makes the same "spawn data or typed def" choice; both branches should choose the
same way.

## Objective check

| Rule | Effect | Note |
|------|--------|------|
| `O1-R1` No global session state | waiting | The shot still uses `MegaEntityFactory` (static), as now. Item `096`. |
| `O1-R2` Components, not inheritance | helps | Removes 2 or 3 `AbstractProjectile` subclasses; adds no abstract class. |
| `O1-R3` No new fragile order | no effect | No new listener or event. |
| `O1-R4` Typed state | helps | The kind is typed data under the existing `ConstKeys.TYPE`. The moved spawners stop using `LEFT` and `IMPULSE` for these shots. No new key. |
| `O1-R5` Code only with a caller | helps | Each kind field has a caller (table above). |
| `O2-R1` Input only through `InputFrame` | no effect | |
| `O2-R2` Seeded random | no effect | No random values. |
| `O2-R3` One fixed tick | waiting | No change to timing. |
| `O2-R4` Stable order | no effect | |
| `O2-R5` No logic assumes one player | no effect | `canBeDamagedBy` uses `owner.asPlayer`, as now. |
| `O2-R6` One camera | waiting | `overlapsGameCamera()` gates the death sound, as now. |
| `O2-R7` Gameplay not from render or audio | no effect | The camera check gates only the sound. |
| `O2-R8` Local data out of simulation | no effect | |
| `O2-R9` Events name a slot | no effect | |
| `O3-R1` No JVM-only API on the web path | waiting | No new JVM API. |
| `O5-R1` No per-frame allocation | no effect | The new code runs at spawn and at death; the sprite pre-process allocates nothing new. |
| `O5-R2` Measure hot paths | no effect | Not a hot path. |
| `O6-R1` CI passes | helps | The automated checks of the contract. |
| `O6-R2` Named checks | helps | New test pins damage, size and death rules per kind. |
| `O7-R3` No level-identity checks | no effect | |
| `O8-R2` Capabilities, not character checks | no effect | |
| `O9-R1` NES-style pad | no effect | |
| `O9-R2` Saves stay loadable | no effect | No save data. |

## Decisions

### `D1`: the name of the shared class

- A. Rename `JunkShot` to `EnemyShot` (one rename; the atlas regions keep the `JunkShot/` prefix, because each kind
  names its own region). **Recommended**: the class is not about junk any more, and a later reader finds it by name.
- B. Keep the name `JunkShot`. Fewer changed lines (16 in `core/src/main`, 38 in tests), but the name is wrong for a
  fire pellet.

### `D2`: how the damage of a kind reaches the player

- A. One tag entry: `EnemyShot.TAG pairTo dmgNeg { (it as EnemyShot).kind.damage }`. Each kind holds its damage; the
  moved kinds hold 3, the old `MEDIUM` value. **Recommended**: one place per shot, typed, and a test can read it.
- B. Keep the size path: give each kind a `Size`, and use the tag entry only for junk kinds. The damage of a moved
  shot then hides in a size, and the shared class needs 2 paths.
- C. A tag per kind. Not possible today: tags are fixed at `init()` (item `149`).

### `D3`: the shots in the first branch

- A. `JunkShot` (4 kinds) + `RollingBotShot` + `FirePellet`. **Recommended**: no lookup by class or tag, one spawner
  each, a live room with a player spawn for each, and together they cover each kind field above.
- B. A + `SubmarineMissile`. Adds the `SHIELD` fixture (player shots bounce off) and the `Explosion` death. Its live
  room (`WILY_STAGE_2` `path1_room1`) has no player spawn, so its check starts in `fork_room` above it.
- C. `JunkShot` only: rename and make the kind general, no moved shot. The smallest change, but no new caller proves
  the general fields, so most fields could not be added (`O1-R5`).

### Settled without the owner

- Typed kind, not loose spawn properties: the kind is one `enum` entry in the shared class, passed as
  `ConstKeys.TYPE`, as `JunkShot.Type` is now. `init()` loads the art of every kind, so `core:smokeTest` checks every
  region (fail fast). Loose spawn keys would add keys (`O1-R4`) and move errors to play time.
- The kinds stay in one `enum` in the shot, not in each enemy, so that `init()` sees them all.

## Open questions

None besides `D1` to `D3`.

## Owner answers (2026-11-13)

- D1: Rename `JunkShot` to `EnemyShot`; the atlas regions keep their names.
- D2: One tag entry in `PlayerDamageNegotiator` reads `kind.damage`.
- D3: The 4 `JunkShot` kinds, `RollingBotShot` and `FirePellet`.
