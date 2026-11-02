# Notes: 048 Replace entity inheritance with components

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## From the old `BACKLOG.md` (2026-10-22)

Entities get health by extending `AbstractHealthEntity`. Enemies must extend `AbstractEnemy` and
bosses must extend `AbstractBoss`. Logic in the game does type checks on these classes. Replace
the inheritance layers with components (for example a health component) and remove the type
checks.

## Scoping scan (2026-11-02, `master` at `71761c21c`)

Counts are `grep` results, so they can include a few comments. Check them again before the work starts. The subclass counts for `AbstractEnemy`, `AbstractBoss` and `AbstractProjectile` come from the `O1` audit. Some enemy and boss files are fully commented out, for example `ReactorMan_OLD.kt` (see `092`).

### Enemy, boss and health layers

| Area | Today | Measured size | Destination |
|---|---|---|---|
| Health | `AbstractHealthEntity`, `IHealthEntity` | About 142 enemy and boss classes (124 or 125 direct `AbstractEnemy` subclasses, plus 18 through `AbstractBoss`; the 1-line difference is not resolved), `Megaman`, 6 other classes | Health component |
| Taking damage | `canBeDamagedBy`, `takeDamageFrom`, `editDamageFrom`, `invincible` | 36, 25, 4 and 17 overrides | Component with gates and reactions; `023` owns the amounts |
| Dealing damage | `canDamage`, `onDamageInflictedTo` | 18 and 51 overrides; 272 `IDamagerEntity` implementers | Same pattern |
| Hit feedback | `damageBlink`, spawn blink | 155 lines in 148 files; `spawn_blink` 314 times in TMX | Data component and a system (`017`) |
| Culling | Set in `AbstractEnemy.onSpawn` and in `AbstractProjectile.onSpawn` | `cull_out_of_bounds` 975 times in TMX | One component, then `041` |
| Death | Drops, `disintegrate`, `explode`, points | 24 `onHealthDepleted` overrides; `explode(` in 39 lines | `ItemDropComponent` and its system for drops (`ITEM.md`, "Decisions"); plain functions for the rest |
| Helpers | `spawnWhackForOverlap`, `spawnFloatingPoints`, `isMegamanShootingAtMe` | 0 overrides | Plain functions |
| Boss flow | `ready`, `defeated`, `isReady`, `onReady`, `triggerDefeat`, `explodeOnDefeat` | 21, 9 and 8 overrides of `isReady`, `onReady`, `triggerDefeat`; `defeated` in 101 lines in 28 files | `BossComponent`, with states from `015` |
| Boss readers | `MegaLevelScreen`, `Gate`, `IBossListener` | 5 typed reads, 123 listeners | Read the component, not `AbstractBoss` |
| Identity | `is AbstractEnemy` or `AbstractEnemy::class`, `EntityType.ENEMY` | 15 live lines; 6 live lines. Most `getType() ==` lines are `FixtureType`, not `EntityType` | `FactionComponent` and capability data (`ITEM.md`, "Decisions") |
| Template methods | `defineBodyComponent`, `defineSpritesComponent`, `defineUpdatablesComponent` | 150, 150 and 145 overrides in enemies and bosses | Each `init()` adds the components |
| State contracts | `IFreezableEntity`, `IFaceable`, others | 65 `IFreezableEntity` implementers, 64 with the same delegate | Components |

### Projectile layer

| Part | Today | Measured size | Destination |
|---|---|---|---|
| Base layer | `AbstractProjectile`, `IProjectileEntity` | 69 subclasses, 73 and 98 lines | Removed; the classes extend `MegaGameEntity` |
| Hit hooks | `hitBlock`, `hitShield`, `hitProjectile`, `hitBody`, `hitSand`, `hitWater` | 133 overrides: 57, 34, 14, 11, 9, 8 (`hitExplosion` not counted) | Handlers in a `ProjectileComponent`, called by `ProjectileRules` |
| Owner | `IOwnable<IGameEntity>` | 108 implementers | Component, with the design from `030` |
| Type checks | `is` or `as` on `IProjectileEntity` or `AbstractProjectile` | 24 lines in 20 files | Capability checks |
| Helper | `explodeAndDie` | On the interface | Plain function |

`AbstractProjectile` and `AbstractEnemy` each have their own copy of the `onDamageInflictedTo` callback, `movementScalar` and the cull setup.

### Risks

- **OR gates:** at least 13 `canBeDamagedBy` overrides have the form `damager is Axe || super.canBeDamagedBy(damager)`, so that damager skips the invincibility check. A plain list of AND gates cannot express this. The count comes from a listing of 30 of the 36 overrides.
- **Full replacements:** `Petal`, `Needle`, `CactusMissile`, `HomingMissile`, `PinkBlock` and `Wanaan` replace the damage logic and do not call `super`.
- **Boss death:** `AbstractBoss.definePointsComponent` replaces the base listener, so `onHealthDepleted` and `onHealthDepletedCallbacks` do not run for a boss. One shared death path would change this behavior.
- **Hidden order:** `AbstractBoss.onSpawn` writes `DROP_ITEM_ON_DEATH` and `CULL_OUT_OF_BOUNDS` to `spawnProps` before `super.onSpawn`.
- **Reset order:** `reset()` runs before `onDestroy`, but drops and defeat orbs read health at destroy. A new component must not clear that value (`087`, `112`).
- **Exact class keys:** `addComponent` keys on the exact class, so a boss component cannot be a subclass of an enemy component.
- **Performance:** each damage contact changes from 2 casts to 2 map lookups (`063` `NOTES.md`). This needs a measurement for `O5`.
- **Hook order:** hit hooks run during contact processing, so a handler must see the same state in the same contact pass.
- **`030` overlap:** `030` asks whether one `owner` is enough, so a move of `owner` before `030` can touch the same field twice.

### `065`

Only 1 enemy or boss file with an `init()` override has no `super.init()`, so `065` is a small part of this item's risk. This is a per-file check, not a check of each override.

## Agent report: Enemy and boss flags (2026-11-02)

Note: Replaced or corrected later: `ITEM.md`, "Decisions", is final. The claim that one side value for shots and actors gives `BulletBill` a special case is wrong; the reason to store them apart is in `030` `ITEM.md`. `BossComponent` is narrower than this report says (see `118`). The name `roomBound` here is a proposed cull field, not an existing one.

### Do `048` entities need an "enemy" or "boss" flag? (`master` at `29971e2fa`)

No files were changed. `ROADMAP.md` was already modified before this task.

#### 1. Direct answer

**The assumption is partly true. No enemy marker and no boss flag is necessary.**

- **Real role sites:** 0 sites need a role that no capability can describe.
- **Enemy-specific logic:** none of the logic in `AbstractEnemy` is enemy-only. Each part is a generic ability with enemy defaults.
- **Boss-specific logic:** this logic is real: intro handshake, defeat flow and level exit. It needs a `BossComponent` that holds fight state.
  - No site needs to ask "is this a boss?". The readers get the entity from an event payload and then read its data.
- **One value is necessary:** a side value on each actor (`030`). It is read by 14 lines in 8 files.
  - In practice, `FactionComponent(ENEMY)` is present on the same classes as `is AbstractEnemy` today.
  - If the owner counts that as a flag, the answer is "one value, not two markers, for these 14 lines".
  - It is more than a marker for 3 reasons: it also covers the player, it can change at run time, and it can hold teams.

**Sites that need a side value (14 lines):**

- `AbstractEnemy.kt:259-260` (2 lines)
- `Bullet.kt:141`
- `SmokePuff.kt:89-90` (2 lines)
- `Starkner.kt:218`
- `CartV2.kt:151,155,167,171` (4 lines)
- `ToxicWaterfall.kt:137,151` (2 lines)
- `CeilingCrusher.kt:160`
- `MagFly.kt:125`

**Why one marker cannot work:** "enemy" has two different meanings in the code today.

| Test | Boss | `SigmaRatClaw`, `GutsTankFist` | `BulletBill` | Live lines |
|---|---|---|---|---|
| `is AbstractEnemy` or `::class` | true | true | true | 15 |
| `EntityType.ENEMY` | false | true | true | 6 |

So one marker gives the wrong result at one of these two groups of sites. [code]

#### 2. How I searched

I used `grep` on `core/src/main` and `core/src/test` for these terms:

- `\bAbstractEnemy\b`, `\bAbstractBoss\b`
- `EntityType\.[A-Z_]+`
- `getType\(\)`
- `getOfType|getOfTypes`
- `isAny\(`
- `ConstKeys\.BOSS`
- `EventType` boss keys
- `IBossListener`

I also read each site in context. I checked which maps use an entity against the 14 story, `SMB3_1` and gauntlet TMX files that `LevelDefinition` names ("live maps" below). Test maps are not counted.

**Files that hold only comments or imports (excluded):** `SphinxMiniBoss.kt`, `MechaDragon_OLD.kt`, `ReactorMan_OLD.kt`, `TreeStump.kt`, `GravityCapsule.kt`, `Zap.kt`, `GapingFish.kt`, `JetMet.kt`, `ColtonJoe.kt`, `MegamanTrailSprite.kt`.

**Count corrections:**

| Claim in the brief or docs | Measured |
|---|---|
| `is AbstractEnemy`: 12 lines in 8 files | Correct, but 1 line is a comment (`Matasaburo.kt:77`). 11 live lines in 7 files |
| `AbstractEnemy::class` | 4 more lines in 3 files |
| `getType() ==`: 39 lines in 30 files | Now 43 lines in 34 files. 34 are `FixtureType`. Only 9 lines in 9 files are `EntityType`, and only 2 of those are enemy or boss |
| `EntityType.ENEMY`: 17 lines, `EntityType.BOSS`: 4 lines | Correct in `main`. `EntityQueriesTest.kt:36` adds 1 test line |
| 128 `AbstractEnemy` subclasses, 17 through `AbstractBoss` (`O1`, `048` `NOTES.md`) | I count 124 direct and 18 through `AbstractBoss`, so 142 live classes. The 124 include `SniperJoe_OLD` and `SpikeBot_OLD`. `112` `NOTES.md` also says 18 |

#### 3. Site inventory

##### Side or hostility: 6 lines in 4 files

| Site | What it asks | Replacement | Form |
|---|---|---|---|
| `AbstractEnemy.kt:260` | Is the damager's owner a player, or null? | Same test through the side of the owner | Value on `FactionComponent` |
| `AbstractEnemy.kt:259` | An orb may hurt any enemy except its owner | A field on the damager, for example "hits its own side". The base class then has no `ReactorManProjectile` reference | Data on the damager |
| `Bullet.kt:141` | Target and owner are on the same side, so the bullet passes through. A boss counts | Side of target and side of owner | Value |
| `SmokePuff.kt:89-90` | Player against enemy or `IHazard` | Side, and `IHazard` stays for now | Value |
| `Starkner.kt:218` | `owner !is AbstractEnemy` | Side. It changes the result only for a `ReactorManProjectile` with an enemy owner, because the base gate already rejects other cases [inferred]. `ReactorMan` and `TurnBlaster` are absent from the only live map with `Starkner` (`MoonMan_16x14_v5.tmx`) | Value, or remove it if the owner accepts this |

##### Capability or world reaction: 14 lines in 10 files

| Site | What it asks | Replacement | Form |
|---|---|---|---|
| `CartV2.kt:151,155,167,171` | A moving cart kills an enemy. A boss also dies in one hit | `faction == ENEMY` first. Later: hostile to the rider | Value |
| `ToxicWaterfall.kt:137,151` | Player or enemy gets the splash sound and the push | Has `FactionComponent` | Presence of the side value |
| `CeilingCrusher.kt:160` | A player or enemy triggers the crusher | Has `FactionComponent`. 0 live maps | Presence of the side value |
| `MagFly.kt:125` | No pull on enemies. See note below | `faction == ENEMY`. 0 live maps | Value |
| `FreezableEntityHandler.kt:50` | Only a way to reach `canDamagePredicates`. All 61 files that build the handler are enemies or bosses, so it is always true | A gate on the component that deals damage | Component API |
| `MegaContactFilter.kt:106` | Two entities of a group ignore each other's contacts | A field, see section 6 | Plain field |
| `LevelRoomHandler.kt:72`, `MegaLevelScreen.kt:544` | Destroy what belongs to the room | A `roomBound` field on the cull component (`048` step 3), then `041` | Data field |
| `DarknessV2.kt:105`, `DarknessV4.kt:116` | Only a candidate filter before a class-keyed map (`DarknessV4.kt:416`) | Look up the keys of the map by tag, or use a light component. `DarknessV2` is live in 2 maps, `DarknessV4` in 0 | No role |

**Note on `MagFly.kt:125`:** `MegaContactFilter.kt:106` runs first and removes the `ENEMY` against `ENEMY` contact. So this check can be true only for an `AbstractBoss`. [inferred from code]

##### Encounter role: 10 lines in 7 files

| Site | What it reads | Replacement |
|---|---|---|
| `MegaLevelScreen.kt:717,764,802` | `boss.mini` | `BossComponent.mini` |
| `MegaLevelScreen.kt:793` | `getEventOnDefeated()` | A field or function on `BossComponent` |
| `Gate.kt:198` | `boss.bossKey` | `BossComponent.bossKey` |
| `BossSpawnEventHandler.kt:21` | Typed fetch, and `ready = false` | Fetch as `MegaGameEntity`. The write looks redundant: `AbstractBoss.kt:130,148` already clear `ready` |
| `IBossListener.kt:5`, `BigAssMaverickRobotOrb.kt:139`, `WanaanLauncher.kt:124`, `BigAssMaverickRobotHand.kt:215` | The `boss` parameter | Remove the parameter. It is used only in debug logs (2 overrides) or not at all (1 override) |

`BossHealthHandler.set` already takes `IHealthEntity` (`BossHealthHandler.kt:38`). So the health bar needs no boss type. [code]

##### Dead or leftover checks: 6 lines

| Site | Why |
|---|---|
| `MegaLevelScreen.kt:779` `getType() != BOSS` | No `AbstractBoss` subclass implements `IBossListener` or `IHazard`, and no type above them does. The test is always true |
| `MegamanDamageNegotiator.kt:33` `ENEMY` | Every `ENEMY` entity is `ISizable`, and that branch comes first (`:42` onward). Only `EXPLOSION` can reach the line |
| `Snowball.kt:91` | `SnowballExplosion.kt:76` tests the exact class, so `AbstractEnemy::class` matches nothing. No player spawn was found |
| `JoeBall.kt:151` | `Explosion` does not read `ConstKeys.MASK` at all |
| `Matasaburo.kt:77` | A comment |
| `AbstractEnemy.kt:189` `lastDamager != CartV2` | **Bug candidate.** `CartV2` here is the companion object, so the test is always true. Also, `depleteHealth()` does not set `lastDamager`. So a cart kill can drop items, and the design intent seems to be no drop. No issue file covers this |

##### Keep as labels: no change

These lines are label or package use:

- `EntityFactories.kt:19,20`
- `GauntletMaster.kt:143,211`
- `SpawnersLayerBuilder.kt:49`
- `MegaGameEntities.kt:21,27`
- The `EntityType` path at `BossSpawnEventHandler.kt:21`
- `EntityFactories.fetch(EntityType.ENEMY, ...)`: `PenguinMiniBoss.kt:189`, `PipiEgg.kt:103`, `PetitDevil.kt:156`, `SwingingPlatform.kt:184,196`
- `getType()` overrides at `AbstractEnemy.kt:372`, `AbstractBoss.kt:304`, `BulletBill.kt:178`

`super<AbstractEnemy>` at `Starkner.kt:198` and `BulletBill.kt:106` is only syntax, and it goes with the class.

Test site: `CameraEdgeBlockTest.kt:43` is a mock that stands for "not a player". The production code uses `asPlayer` (`CameraEdgeBlock.kt:59`), so the mock can be a plain `MegaGameEntity`.

#### 4. Base-class behavior

| Behavior | Evidence | Enemy-only or boss-only? | Target form |
|---|---|---|---|
| Spawn blink | `AbstractEnemy.kt:125-127,165-187`. No external reader of `isSpawnBlinking` | Generic | Data component and system (`017`) |
| Item drops | `:140-162`. `ItemDropComponent` already decided | Generic. A boss simply has no component | Keep the decision |
| Death `disintegrate()` | `:141-142` also runs for a boss, because only the drop is off | Generic | Must stay for bosses |
| Floating points | 15 lines in 9 files, all SMB-style enemies | Generic | Plain function |
| Hit sound | `:267`, `:276` | Generic, with a data value | Field on feedback data |
| Cull setup | `:205-239`. `AbstractProjectile` has its own copy | Generic | One cull component |
| `movementScalar` | 48 lines in 21 files. `AbstractProjectile` has a copy | Generic | Shared field |
| Side gate | `:259-260` | Hostility, not identity | `FactionComponent` |
| `DAMAGE_INCREASE` | Enemy scalar `2`, boss scalar `1.5` (`MegaEnhancement.kt:38-39`). `GutsTankFist` and `SigmaRatClaw` use the enemy scalar | The value differs, not the logic | Data field, then the `023` modifier pipeline |
| `BOSS_DMG_NEG` | Base table. `Bospider`, `SigmaRat`, `BigAssMaverickRobot` replace it and `WilyFinalBoss` extends it. `GutsTankFist` has its own | Data | The `boss` profile of `023` |
| `DEATH_LISTENER`, `ENTITY_KILLED_BY_DEATH_FIXTURE` | Already properties. Non-boss users include `Copipi`, `PetitDevil`, `AngryFlameBall`, `AbstractItem`, `Petal`, `Cart`, `CartinJoe`, `FireMet`, `DeadlyLeaf` | Generic | Data set by a preset |
| Boss health bar | Takes `IHealthEntity` | Generic reader. Only the `mini` gate is boss data | Reads `BossComponent` |
| Boss music | `AbstractBoss.kt:253-267` | Encounter | `BossComponent` data |
| `mini` | Spawn prop. Hard-coded in `PenguinMiniBoss`, `MechaDragon`, `MoonHead`; also in 3 live maps | Encounter field | `BossComponent` field |
| Intro, `ready`, `BOSS_READY`, `END_BOSS_SPAWN` | `:185-213`, `:239-267` | **Boss-only** | `BossComponent`, states from `015` |
| Defeat flow, `BOSS_DEFEATED`, `BOSS_DEAD`, exit event | `:215-229`, `:269-284` | **Boss-only today.** Another entity could use it | `BossComponent` |
| `bossKey`, `spawnDefeatOrbs` | `Gate.kt:200`, `:154` | Encounter | `BossComponent` |
| `IBossListener` | Default `destroy()`, inherited by `IHazard` and `AbstractProjectile` | Generic "fight ended" reaction | Keep, remove the parameter |
| `onPlayerDamaged` | 1 override (`PreciousWoman`) | Generic signal | Connect in the class |

#### 5. How a boss is an enemy today

| Site | Boss counts? | Intended? |
|---|---|---|
| `AbstractBoss.kt:173` calls the enemy side gate | Yes | Yes |
| `Bullet.kt:141` | Yes | Yes [inferred] |
| `FreezableEntityHandler.kt:50` | Yes | Yes. `WilyFinalBoss.kt:188` opts out on purpose, which proves the default |
| `SmokePuff.kt:89-90` | Yes | No effect: the boss table has no `SmokePuff` [code] |
| `Starkner.kt:218` | Yes, as an owner | Unclear, but no live effect |
| `MagFly.kt:125` | Yes, the only case that reaches it | Unclear. 0 live maps |
| `CartV2.kt:151,155,167,171` | Yes, a boss dies at once | Unclear. Probably not designed. Live `CartV2` objects are in `DESERT_MAN` `room9` and `room10` |
| `ToxicWaterfall.kt:137,151` | Yes | Unclear. 13 objects in `ReactorMan_v3.tmx` |
| `CeilingCrusher.kt:160` | Yes | Unclear. 0 live maps |
| `MegaContactFilter.kt:106` | **No**, a boss contacts enemies | Unclear |
| `LevelRoomHandler.kt:72`, `MegaLevelScreen.kt:544` | **No** | See note below |
| `DarknessV2.kt:105` | **No** | No effect: no boss class is in the map |

**Note on the room sweep:** a boss also has the `CullableOnEvent` part, because `AbstractBoss.kt:127-128` does not turn off `CULL_EVENTS`. So a boss is culled on `BEGIN_ROOM_TRANS`, `GATE_INIT_OPENING` and `PLAYER_SPAWN`, by a different path. [code]

**Boss parts have no shared identity:**

| Parts | `getType()` |
|---|---|
| `SigmaRatClaw`, `GutsTankFist` | `ENEMY` |
| `BigAssMaverickRobotHand`, `ElecDevilBody`, `ElecDevilBodyPieces` | `OTHER` |
| `BigAssMaverickRobotBody` | `DECORATION` |
| `ElecDevilBodyPiece` | Projectile |

A replacement must keep each of these as it is.

#### 6. Options

| Question | (a) `EnemyComponent` + `BossComponent` markers | (b) One role value | (c) Capabilities only | (d) Side value + `BossComponent` + capability fields |
|---|---|---|---|---|
| Sites handled | 15 or 6, not both, with no `&& !hasBoss` tests | Fails: a boss needs both `ENEMY` and `BOSS` | All, but needs at least 5 new reaction markers on up to 142 classes | All 30 live lines, with exact behavior |
| Boss that counts as an enemy | Both markers on 18 classes, or `has(Enemy) \|\| has(Boss)` at sites | Not possible with one value | Each capability is added | One `FactionComponent` class, value `ENEMY`, so exact-class keys are no problem |
| Enemy and block | Works | Works only if the value is not a kind | Works | Works: the side is separate from the block fixture |
| `030`, PvP, co-op | Marker duplicates faction. Sites still need player tests | A faction value fits. A role value does not | `030` still needs a side, so this is not a real alternative | Same value. Teams can go here. The value changes for the `031` captured boss case |
| `050` data | `tags: [enemy]` | One field | Many fields | One `faction` field, an optional `boss` block, presets |
| Hot damage path | 2 lookups in place of casts | 1 field read if on `MegaGameEntity` | Depends | Lookup count same as today's `asPlayer`, see below |
| Risk of a renamed `is AbstractEnemy` | **High** | High if it is a role | Low | Medium |

**`EntityType` cannot answer these questions, for these reasons:**

- It gives the boss the wrong result for the 15 `is AbstractEnemy` lines.
- It holds one value, but `BulletBill` is both enemy and projectile.
- It is not the package truth: 2 classes in `bosses/` say `ENEMY`, and `FireMetSpawner` and `FloatingCanHole` in `enemies/` say `HAZARD`.
- The 2026-10-22 decision moves these checks away from it.

It can still give a zero-cost answer for `MegaContactFilter.kt:106`. That is an open question below.

**Runtime operations:**

- **Damage gate today:** 1 class check, 1 interface check, 1 `owner` read, 1 cast, 1 `hasComponent` map lookup (`PlayerComponent.kt:11-12`), plus class-keyed damage map lookups.
- **`(d)` on the damage gate:** 1 map lookup on the owner replaces the `asPlayer` lookup, so the count stays the same. [inferred]
- **`Bullet.kt:141`:** 2 `instanceof` checks become up to 2 lookups, per projectile-body contact.
- **`MegaContactFilter.kt:106` is the hottest site:**
  - It runs for each candidate pair, before the overlap test (`WorldSystem.kt:242-243`).
  - Today it does 2 `getEntity()` property lookups and 2 virtual calls.
  - 2 component lookups here would double the lookups per pair. So it must not read a component.
- All of this is unmeasured, and `O5-R2` requires a measurement.

#### 7. Recommendation: option (d)

Do not add `EnemyComponent`. Do not make `BossComponent` a filter.

| New API | Purpose | First production caller |
|---|---|---|
| `FactionComponent(faction)` | Side of an actor. Present on players, enemies, bosses, `SigmaRatClaw`, `GutsTankFist`, `BulletBill`. Absent from projectiles and hazards | The enemy damage gate at `AbstractEnemy.kt:260` |
| Side of a damager's source (with `030`) | Copied from the root owner. Kept separate from `FactionComponent` | `AbstractEnemy.kt:260` when `030` lands |
| Damager field "hits its own side" | Replaces the `ReactorManProjectile` check | `AbstractEnemy.kt:259` logic |
| `roomBound` on the cull component | Replaces `getOfType(ENEMY)` | `LevelRoomHandler.kt:72` |
| `MegaGameEntity` field `ignoresPeerContacts` | Plain field for the hot filter | `MegaContactFilter.kt:106` |
| `enhancementDamageScalar` on the damage-taken component | Data for 2.0 against 1.5 | The `editDamageFrom` logic |
| `BossComponent` | Fight state | `MegaLevelScreen.kt:717` |
| A plain preset function for enemy defaults | Avoids repeating defaults in 142 `init()` methods | Each migrated class. It sets values and is not an identity |

**The design differs from the `030` proposal on one point.** `030` copies faction to projectiles. If I use one component for both, these two things happen:

- "Has a side" stops meaning "is an actor".
- `faction == ENEMY && !projectile` is false for `BulletBill`, and today `BulletBill` is an `AbstractEnemy`.

Separate fields avoid both problems. They also keep `PreciousGem`, `SealionBall`, `GroundPebble`, `DarkSerketClaw`, `WanaanLauncher` and `DeadlyLeaf` outside the enemy set, as they are today.

**A rule keeps the marker risk low:** read the faction only for hostility and actor tests. Never choose enemy behavior from it. Behavior comes from a component that the entity has.

**X6 comparison** (`~/IdeaProjects/mmx-demake-tools/docs/engine.md`), for ideas only:

- "Object classes": enemies and bosses share one `main` class with no boss flag. The side is implicit in the object pool.
- "Boss entry and exit": boss flow is in separate stage-logic objects that the boss makes.
- "Damage to an object": each object holds a table pointer.

This supports a boss flow part and data tables, with no role marker. [read in that document]

#### 8. Behavior that a replacement must keep

1. `is AbstractEnemy` is true for bosses and parts. `EntityType.ENEMY` is false for bosses.
2. A boss has contacts with enemies, but `SigmaRatClaw` and `GutsTankFist` have no contacts with other `ENEMY` entities.
3. The sweep destroys gauntlet children, which have `CULL_EVENTS` false. `GauntletMaster.kt:191` depends on this.
4. The sweep queues the destroy at once. An event cull can run a frame later. [inferred]
5. An owner of `null` can hurt an enemy (`AbstractEnemy.kt:260`). `MagFly.kt:129` and `Laser.kt:449` create this case.
6. A damager that is not `IOwnable` skips the owner gate and goes to the negotiator.
7. A `ReactorManProjectile` can hurt other enemies, and not its owner. `ReactorManProjectile.kt:215` can change the owner to a shield entity.
8. A boss has no drops, but it does run `disintegrate()`.
9. A boss uses scalar 1.5. Its parts use 2.0.
10. `WilyFinalBoss` can deal damage while frozen.
11. A boss death does not run `onHealthDepleted` (`048` `NOTES.md`).
12. The order `DROP_ITEM_ON_DEATH` and `CULL_OUT_OF_BOUNDS`, then `super.onSpawn`, must keep its result.
13. A boss ignores death fixtures, through `ENTITY_KILLED_BY_DEATH_FIXTURE` and `DEATH_LISTENER`.

#### 9. Open questions for the owner

| Question | Recommendation |
|---|---|
| Should a moving cart kill a boss in one hit? | Keep it in `048`, and file a question. Later, use "hostile to the rider" |
| Should a cart kill drop items? `AbstractEnemy.kt:189` seems to intend no drop, but it never blocks one | File a bug in `docs/issues/`. Do not change it in `048` unless the owner asks |
| Keep `getType()` at `MegaContactFilter.kt:106`, add `ignoresPeerContacts`, or use faction? | Add the field. Faction also blocks enemy against boss contacts, which is a behavior change. Keeping `getType()` has no cost, but it is an exception to the 2026-10-22 decision |
| Must `030` split "actor side" from "damage source side"? | Yes, or `BulletBill` needs a special case |
| Do hazards get a side? | Not yet. `SmokePuff.kt:89-90` can keep `IHazard` |
| Must `AbstractEnemy.kt:259` stay, so that an enemy orb can hurt other enemies? | Keep it as a damager field. `TurnBlaster` is in 0 live maps, so only `ReactorMan` and shield changes reach it |
| Do the dead checks go before or in step 5 of `048`? | Remove the 5 code lines first. They carry no behavior, and the scan shows no reason to keep them. `AbstractEnemy.kt:189` is the exception: it stays under the bug question above |
| Should `O1` and `048` `NOTES.md` change 128 to 142 classes? | Yes, after one more independent count |

#### 10. Limits of this research

- I did not run the game or any test. Claims about reachability come from reading code and TMX files.
- "0 live maps" covers only the 14 maps that I listed by name. I did not check enemies that code spawns inside other entities.
- I read `Starkner`, `MagFly` and the gauntlet code in part, not each full file.

Relevant docs: `/home/johnlavender/IdeaProjects/Megaman-Maverick/docs/mega-refactor/items/048-components-not-inheritance/ITEM.md`, `/home/johnlavender/IdeaProjects/Megaman-Maverick/docs/mega-refactor/items/030-faction/ITEM.md`, `/home/johnlavender/IdeaProjects/Megaman-Maverick/docs/mega-refactor/items/031-*/NOTES.md`.
